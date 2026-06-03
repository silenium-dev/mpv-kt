#include "instance.hpp"

#include <format>
#include <iostream>
#include <mutex>

#include "nodes.hpp"
#include "helper/JniMpvCallback.hpp"
#include "helper/results.hpp"


jobject eventPropertyToJava(JNIEnv *env, const mpv_event_property *prop) {
    jobject value = nullptr;
    switch (prop->format) {
        case MPV_FORMAT_INT64: {
            value = boxedLong(env, *static_cast<int64_t *>(prop->data));
            break;
        }
        case MPV_FORMAT_DOUBLE: {
            value = boxedDouble(env, *static_cast<double *>(prop->data));
            break;
        }
        case MPV_FORMAT_FLAG: {
            value = boxedBool(env, *static_cast<bool *>(prop->data));
            break;
        }
        case MPV_FORMAT_OSD_STRING:
            [[fallthrough]];
        case MPV_FORMAT_STRING: {
            value = env->NewStringUTF(*static_cast<char **>(prop->data));
            break;
        }
        case MPV_FORMAT_BYTE_ARRAY: {
            const auto ba = static_cast<mpv_byte_array *>(prop->data);
            const auto result = env->NewByteArray(ba->size);
            env->SetByteArrayRegion(result, 0, ba->size, static_cast<const jbyte *>(ba->data));
            value = result;
            break;
        }
        case MPV_FORMAT_NODE: {
            const auto node = static_cast<mpv_node *>(prop->data);
            value = mapNode(env, *node);
            break;
        }
        case MPV_FORMAT_NODE_MAP:
            [[fallthrough]];
        case MPV_FORMAT_NODE_ARRAY: {
            const auto nodeList = static_cast<mpv_node_list *>(prop->data);
            const mpv_node node{
                .u = {.list = nodeList},
                .format = prop->format,
            };
            value = mapNode(env, node);
            break;
        }
        case MPV_FORMAT_NONE:
            return nullptr;
        default:
            std::cerr << "Unsupported format: " << prop->format << std::endl;
            break;
    }
    return value;
}

void MPVInstance::wakeupCallback(void *ctx) {
    if (ctx == nullptr) {
        return;
    }
    const auto instance = static_cast<MPVInstance *>(ctx);
    instance->onWakeup();
}

void MPVInstance::onWakeup() {
    m_wakeup = true;
    m_cv.notify_one();
}

void MPVInstance::eventLoop() {
    const detail::AttachedEnv attached{m_jvm};
    while (m_running) {
        {
            std::unique_lock lock(m_mtx);
            m_cv.wait(lock, [&] { return !m_running || m_wakeup; });
            m_wakeup = false;
        }
        dispatchEvents(attached.get());
    }
}

void MPVInstance::dispatchEvents(JNIEnv *env) {
    while (m_running) {
        std::shared_lock lock(m_callbackMutex);
        const auto event = mpv_wait_event(m_handle, 0);
        switch (event->event_id) {
            case MPV_EVENT_NONE:
                return;
            case MPV_EVENT_PROPERTY_CHANGE: {
                const auto prop = static_cast<mpv_event_property *>(event->data);
                std::string name{prop->name};
                const auto value = eventPropertyToJava(env, prop);
                m_callback->onPropertyChanged(env, name, value);
                env->DeleteLocalRef(value);
                break;
            }
            case MPV_EVENT_GET_PROPERTY_REPLY: {
                const auto prop = static_cast<mpv_event_property *>(event->data);
                jobject value = nullptr;
                if (event->error == MPV_ERROR_SUCCESS) {
                    value = eventPropertyToJava(env, prop);
                } else if (event->error == MPV_ERROR_PROPERTY_UNAVAILABLE) {
                    value = resultSuccessNull();
                } else {
                    value = mpvResultFailure(env, "mpv_get_propery reply", event->error);
                }
                m_callback->onPropertyGet(env, event->reply_userdata, value);
                env->DeleteLocalRef(value);
                break;
            }
            case MPV_EVENT_SET_PROPERTY_REPLY: {
                jobject value = nullptr;
                if (event->error == MPV_ERROR_SUCCESS) {
                    value = resultSuccess(env);
                } else {
                    value = mpvResultFailure(env, "mpv_set_propery reply", event->error);
                }
                m_callback->onPropertySet(env, event->reply_userdata, value);
                env->DeleteLocalRef(value);
                break;
            }
            case MPV_EVENT_COMMAND_REPLY: {
                const auto reply = static_cast<mpv_event_command *>(event->data);
                jobject value = nullptr;
                if (event->error == MPV_ERROR_SUCCESS) {
                    value = mapNode(env, reply->result);
                } else {
                    value = mpvResultFailure(env, "mpv_event_command reply", event->error);
                }
                m_callback->onCommandReply(env, event->reply_userdata, value);
                env->DeleteLocalRef(value);
                break;
            }
            default:
                break;
        }
    }
}
