#ifdef TARGET_WINDOWS
#define WIN32_LEAN_AND_MEAN
#include <Windows.h>
#endif

#include "helper/results.hpp"
#include "platform.hpp"

#include <GL/gl.h>
#include <iostream>
#include <jni.h>
#include <mpv/client.h>
#include <mpv/render.h>
#include <mpv/render_gl.h>
#include <vector>
#include <mutex>
#include <condition_variable>
#include <atomic>

#include <new>
#include <type_traits>
#include <utility>

template<int, typename Callable, typename Ret, typename... Args>
auto fnptr_(Callable &&c, Ret (*)(Args...)) {
    static std::decay_t<Callable> storage = std::forward<Callable>(c);
    static bool used = false;
    if (used) {
        using type = decltype(storage);
        storage.~type();
        new(&storage) type(std::forward<Callable>(c));
    }
    used = true;

    return [](Args... args) -> Ret {
        auto &c = *std::launder(&storage);
        return Ret(c(std::forward<Args>(args)...));
    };
}

template<typename Fn, int N = 0, typename Callable>
Fn *fnptr(Callable &&c) {
    return fnptr_<N>(std::forward<Callable>(c), static_cast<Fn *>(nullptr));
}


struct MpvContext {
    JavaVM *jvm;
    mpv_handle *handle;
    jobject object;
    jmethodID method;

    std::thread eventLoop;
    std::mutex mtx;
    std::condition_variable cv;
    std::atomic<bool> wakeup{false};
    std::atomic<bool> running{true};
};

jobject eventDataToJava(JNIEnv *env, const mpv_event_property *prop) {
    jobject value = nullptr;
    switch (prop->format) {
        case MPV_FORMAT_INT64: {
            const auto clazz = env->FindClass("java/lang/Long");
            if (clazz == nullptr) {
                std::cerr << "Class not found: java/lang/Long" << std::endl;
                return nullptr;
            }
            const auto ctor = env->GetMethodID(clazz, "<init>", "(J)V");
            if (ctor == nullptr) {
                std::cerr << "Constructor not found: <init>" << std::endl;
                return nullptr;
            }
            value = env->NewObject(clazz, ctor, *static_cast<long *>(prop->data));
            break;
        }
        case MPV_FORMAT_DOUBLE: {
            const auto clazz = env->FindClass("java/lang/Double");
            if (clazz == nullptr) {
                std::cerr << "Class not found: java/lang/Double" << std::endl;
                return nullptr;
            }
            const auto ctor = env->GetMethodID(clazz, "<init>", "(D)V");
            if (ctor == nullptr) {
                std::cerr << "Constructor not found: <init>" << std::endl;
                return nullptr;
            }
            value = env->NewObject(clazz, ctor, *static_cast<double *>(prop->data));
            break;
        }
        case MPV_FORMAT_STRING:
            value = env->NewStringUTF(*static_cast<char **>(prop->data));
            break;
        case MPV_FORMAT_FLAG: {
            const auto clazz = env->FindClass("java/lang/Boolean");
            if (clazz == nullptr) {
                std::cerr << "Class not found: java/lang/Boolean" << std::endl;
                return nullptr;
            }
            const auto ctor = env->GetMethodID(clazz, "<init>", "(Z)V");
            if (ctor == nullptr) {
                std::cerr << "Constructor not found: <init>" << std::endl;
                return nullptr;
            }
            value = env->NewObject(clazz, ctor, *static_cast<bool *>(prop->data));
            break;
        }
        default:
            // std::cerr << "Unsupported format: " << prop->format << std::endl;
            break;
    }
    return value;
}

void eventCallback(JNIEnv *env, const mpv_event *event, jobject callback) {
    switch (event->event_id) {
        case MPV_EVENT_PROPERTY_CHANGE: {
            const auto prop = static_cast<mpv_event_property *>(event->data);
            const auto name = env->NewStringUTF(prop->name);
            const auto value = eventDataToJava(env, prop);
            if (value == nullptr) {
                return;
            }
            const auto clazz = env->FindClass("dev/silenium/multimedia/core/mpv/MPVListener");
            if (clazz == nullptr) {
                std::cerr << "Class not found" << std::endl;
                return;
            }
            const auto method = env->GetMethodID(clazz, "onPropertyChanged", "(Ljava/lang/String;Ljava/lang/Object;)V");
            if (method == nullptr) {
                std::cerr << "Method not found: onPropertyChanged" << std::endl;
                return;
            }
            env->CallVoidMethod(callback, method, name, value);
            break;
        }
        case MPV_EVENT_GET_PROPERTY_REPLY: {
            const auto prop = static_cast<mpv_event_property *>(event->data);
            jobject value = nullptr;
            if (event->error == MPV_ERROR_SUCCESS) {
                value = eventDataToJava(env, prop);
                if (value == nullptr) {
                    return;
                }
            } else if (event->error == MPV_ERROR_PROPERTY_UNAVAILABLE) {
                value = resultSuccessNull();
            } else {
                value = mpvResultFailure(env, "mpv_event_property", event->error);
            }

            const auto clazz = env->FindClass("dev/silenium/multimedia/core/mpv/MPVListener");
            if (clazz == nullptr) {
                std::cerr << "Class not found" << std::endl;
                return;
            }
            const auto method = env->GetMethodID(clazz, "onPropertyGet", "(JLjava/lang/Object;)V");
            if (method == nullptr) {
                std::cerr << "Method not found: onPropertyGet" << std::endl;
                return;
            }
            env->CallVoidMethod(callback, method, static_cast<jlong>(event->reply_userdata), value);
            break;
        }
        case MPV_EVENT_SET_PROPERTY_REPLY: {
            jobject value = nullptr;
            if (event->error == MPV_ERROR_SUCCESS) {
                value = resultSuccess(env);
            } else {
                value = mpvResultFailure(env, "mpv_event_property", event->error);
            }

            const auto clazz = env->FindClass("dev/silenium/multimedia/core/mpv/MPVListener");
            if (clazz == nullptr) {
                std::cerr << "Class not found" << std::endl;
                return;
            }
            const auto method = env->GetMethodID(clazz, "onPropertySet", "(JLjava/lang/Object;)V");
            if (method == nullptr) {
                std::cerr << "Method not found: onPropertySet" << std::endl;
                return;
            }
            env->CallVoidMethod(callback, method, static_cast<jlong>(event->reply_userdata), value);
            break;
        }
        case MPV_EVENT_COMMAND_REPLY: {
            // const auto reply = static_cast<mpv_event_command *>(event->data);
            // TODO: Proper node conversions
            jobject value = nullptr;
            if (event->error == MPV_ERROR_SUCCESS) {
                value = resultSuccess(env);
            } else {
                value = mpvResultFailure(env, "mpv_event_command reply", event->error);
            }
            const auto clazz = env->FindClass("dev/silenium/multimedia/core/mpv/MPVListener");
            if (clazz == nullptr) {
                std::cerr << "Class not found" << std::endl;
                return;
            }
            const auto method = env->GetMethodID(clazz, "onCommandReply", "(JLjava/lang/Object;)V");
            if (method == nullptr) {
                std::cerr << "Method not found: onCommandReply" << std::endl;
                return;
            }
            env->CallVoidMethod(callback, method, static_cast<jlong>(event->reply_userdata), value);
            break;
        }
        default:
            break;
    }
}

// void handle_mpv_events(void *ctx) {
//     const auto context = static_cast<MpvContext *>(ctx);
//     JNIEnv *env;
//     const auto res = context->jvm->AttachCurrentThread(reinterpret_cast<void **>(&env), nullptr);
//     if (res != JNI_OK) {
//         std::cerr << "Failed to attach current thread" << std::endl;
//         return;
//     }
//     while (true) {
//         const auto event = mpv_wait_event(context->handle, 0);
//         if (event->event_id == MPV_EVENT_NONE) {
//             break;
//         }
//         eventCallback(env, event, context->object);
//     }
//     context->jvm->DetachCurrentThread();
// }

void handle_mpv_wakeup(void *ctx) {
    const auto context = static_cast<MpvContext *>(ctx);
    {
        std::lock_guard lock(context->mtx);
        context->wakeup = true;
    }
    context->cv.notify_one();
}

void mpv_event_loop(MpvContext *context) {
    JNIEnv *env;
    const auto res = context->jvm->AttachCurrentThread(reinterpret_cast<void **>(&env), nullptr);
    if (res != JNI_OK) {
        std::cerr << "Failed to attach current thread" << std::endl;
        return;
    }
    while (context->running) {
        std::unique_lock lock(context->mtx);
        context->cv.wait(lock, [&] { return context->wakeup.load() || !context->running.load(); });
        context->wakeup = false;
        lock.unlock();

        if (!context->running.load()) break;

        while (context->running) {
            const auto event = mpv_wait_event(context->handle, 0);
            if (event->event_id == MPV_EVENT_NONE) break;
            eventCallback(env, event, context->object);
        }
    }
    context->jvm->DetachCurrentThread();
}

extern "C" {
// Client
JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_createN(JNIEnv *env, jobject thiz) {
    setlocale(LC_NUMERIC, "C");
    const auto handle = mpv_create();
    if (handle == nullptr) {
        return mpvResultFailure(env, "mpv_create", MPV_ERROR_NOMEM);
    }
    return resultSuccess(env, reinterpret_cast<jlong>(handle));
}

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_destroyN(
    JNIEnv *env, jobject thiz, const jlong handle) {
    mpv_terminate_destroy(reinterpret_cast<mpv_handle *>(handle));
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setOptionStringN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name, jstring value) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    const char *valueStr = env->GetStringUTFChars(value, nullptr);
    const auto ret = mpv_set_option_string(reinterpret_cast<mpv_handle *>(handle), nameStr, valueStr);
    env->ReleaseStringUTFChars(name, nameStr);
    env->ReleaseStringUTFChars(value, valueStr);
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_set_option_string", ret);
    }
    return resultSuccess(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setCallbackN(
    JNIEnv *env, jobject thiz, const jlong handle_, jobject callback) {
    const auto handle = reinterpret_cast<mpv_handle *>(handle_);
    JavaVM *jvm;
    if (const auto ret = env->GetJavaVM(&jvm); ret != JNI_OK) {
        std::cerr << "Failed to get JavaVM" << std::endl;
        return mpvResultFailure(env, "GetJavaVM", MPV_ERROR_GENERIC);
    }
    const auto ctx = new MpvContext{jvm, handle, env->NewGlobalRef(callback)};
    mpv_set_wakeup_callback(handle, handle_mpv_wakeup, ctx);
    // mpv_set_wakeup_callback(handle, handle_mpv_events, ctx);

    std::thread eventLoopThread{mpv_event_loop, ctx};
    ctx->eventLoop = std::move(eventLoopThread);
    return resultSuccess(env, reinterpret_cast<jlong>(ctx));
}

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_unsetCallbackN(
    JNIEnv *env, jobject thiz, const jlong ctx) {
    const auto context = reinterpret_cast<MpvContext *>(ctx);
    context->running = false;
    context->cv.notify_all();
    context->eventLoop.join();
    mpv_set_wakeup_callback(context->handle, nullptr, nullptr);
    env->DeleteGlobalRef(context->object);
    delete context;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_commandAsyncN(
    JNIEnv *env, jobject thiz, const jlong handle, jobjectArray args, const jlong replyUserdata) {
    const auto size = env->GetArrayLength(args);
    std::vector<const char *> argv(size + 1);
    for (auto i = 0; i < size; i++) {
        const auto arg = env->GetObjectArrayElement(args, i);
        const auto str = env->GetStringUTFChars(static_cast<jstring>(arg), nullptr);
        argv[i] = str;
    }
    argv[size] = nullptr;
    const auto ret = mpv_command_async(reinterpret_cast<mpv_handle *>(handle), replyUserdata, argv.data());
    for (auto i = 0; i < size; i++) {
        const auto arg = env->GetObjectArrayElement(args, i);
        env->ReleaseStringUTFChars(static_cast<jstring>(arg), argv[i]);
    }
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_command_async", ret);
    }
    return resultSuccess(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_commandN(
    JNIEnv *env, jobject thiz, const jlong handle, jobjectArray args) {
    const auto size = env->GetArrayLength(args);
    std::vector<const char *> argv(size + 1);
    for (auto i = 0; i < size; i++) {
        const auto arg = env->GetObjectArrayElement(args, i);
        const auto str = env->GetStringUTFChars(static_cast<jstring>(arg), nullptr);
        argv[i] = str;
    }
    argv[size] = nullptr;
    mpv_node result;
    const auto ret = mpv_command_ret(reinterpret_cast<mpv_handle *>(handle), argv.data(), &result);
    for (auto i = 0; i < size; i++) {
        const auto arg = env->GetObjectArrayElement(args, i);
        env->ReleaseStringUTFChars(static_cast<jstring>(arg), argv[i]);
    }
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_command", ret);
    }
    // TODO: Convert result to Java
    mpv_free_node_contents(&result);
    return resultSuccess(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_commandStringN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring command) {
    const char *commandStr = env->GetStringUTFChars(command, nullptr);
    const auto ret = mpv_command_string(reinterpret_cast<mpv_handle *>(handle), commandStr);
    env->ReleaseStringUTFChars(command, commandStr);
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_command_string", ret);
    }
    return resultSuccess(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyStringAsyncN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name, const jlong replyUserdata) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    const auto ret = mpv_get_property_async(reinterpret_cast<mpv_handle *>(handle), replyUserdata, nameStr,
                                            MPV_FORMAT_STRING);
    env->ReleaseStringUTFChars(name, nameStr);
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_get_property_async string", ret);
    }
    return resultSuccess(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyLongAsyncN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name, const jlong replyUserdata) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    const auto ret = mpv_get_property_async(reinterpret_cast<mpv_handle *>(handle), replyUserdata, nameStr,
                                            MPV_FORMAT_INT64);
    env->ReleaseStringUTFChars(name, nameStr);
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_get_property_async int64", ret);
    }
    return resultSuccess(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyDoubleAsyncN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name, const jlong replyUserdata) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    const auto ret = mpv_get_property_async(reinterpret_cast<mpv_handle *>(handle), replyUserdata, nameStr,
                                            MPV_FORMAT_DOUBLE);
    env->ReleaseStringUTFChars(name, nameStr);
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_get_property_async double", ret);
    }
    return resultSuccess(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyFlagAsyncN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name, const jlong replyUserdata) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    const auto ret = mpv_get_property_async(reinterpret_cast<mpv_handle *>(handle), replyUserdata, nameStr,
                                            MPV_FORMAT_FLAG);
    env->ReleaseStringUTFChars(name, nameStr);
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_get_property_async flag", ret);
    }
    return resultSuccess(env);
}


JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyStringN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    char *value;
    const auto ret = mpv_get_property(reinterpret_cast<mpv_handle *>(handle), nameStr, MPV_FORMAT_STRING, &value);
    env->ReleaseStringUTFChars(name, nameStr);
    if (ret == MPV_ERROR_PROPERTY_UNAVAILABLE) {
        return resultSuccessNull();
    }
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_get_property", ret);
    }
    const auto result = resultSuccess(env, value);
    mpv_free(value);
    return result;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyLongN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    jlong value;
    const auto ret = mpv_get_property(reinterpret_cast<mpv_handle *>(handle), nameStr, MPV_FORMAT_INT64, &value);
    env->ReleaseStringUTFChars(name, nameStr);
    if (ret == MPV_ERROR_PROPERTY_UNAVAILABLE) {
        return resultSuccessNull();
    }
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_get_property", ret);
    }
    return resultSuccess(env, value);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyDoubleN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    double value;
    const auto ret = mpv_get_property(reinterpret_cast<mpv_handle *>(handle), nameStr, MPV_FORMAT_DOUBLE, &value);
    env->ReleaseStringUTFChars(name, nameStr);
    if (ret == MPV_ERROR_PROPERTY_UNAVAILABLE) {
        return resultSuccessNull();
    }
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_get_property", ret);
    }
    return resultSuccess(env, value);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyFlagN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    jboolean value;
    const auto ret = mpv_get_property(reinterpret_cast<mpv_handle *>(handle), nameStr, MPV_FORMAT_FLAG, &value);
    env->ReleaseStringUTFChars(name, nameStr);
    if (ret == MPV_ERROR_PROPERTY_UNAVAILABLE) {
        return resultSuccessNull();
    }
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_get_property", ret);
    }
    return resultSuccess(env, value);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyStringAsyncN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name, jstring value, const jlong replyUserdata) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    auto valueStr = const_cast<char *>(env->GetStringUTFChars(value, nullptr));
    const auto ret = mpv_set_property_async(reinterpret_cast<mpv_handle *>(handle), replyUserdata, nameStr,
                                            MPV_FORMAT_STRING, &valueStr);
    env->ReleaseStringUTFChars(name, nameStr);
    env->ReleaseStringUTFChars(value, valueStr);
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_set_property_async string", ret);
    }
    return resultSuccess(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyLongAsyncN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name, jlong value, const jlong replyUserdata) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    const auto ret = mpv_set_property_async(reinterpret_cast<mpv_handle *>(handle), replyUserdata, nameStr,
                                            MPV_FORMAT_INT64, &value);
    env->ReleaseStringUTFChars(name, nameStr);
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_set_property_async int64", ret);
    }
    return resultSuccess(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyDoubleAsyncN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name, jdouble value, const jlong replyUserdata) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    const auto ret = mpv_set_property_async(reinterpret_cast<mpv_handle *>(handle), replyUserdata, nameStr,
                                            MPV_FORMAT_DOUBLE, &value);
    env->ReleaseStringUTFChars(name, nameStr);
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_set_property_async double", ret);
    }
    return resultSuccess(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyFlagAsyncN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name, jboolean value, const jlong replyUserdata) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    const auto ret = mpv_set_property_async(reinterpret_cast<mpv_handle *>(handle), replyUserdata, nameStr,
                                            MPV_FORMAT_FLAG, &value);
    env->ReleaseStringUTFChars(name, nameStr);
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_set_property_async flag", ret);
    }
    return resultSuccess(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyStringN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name, jstring value) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    const char *valueStr = env->GetStringUTFChars(value, nullptr);
    const auto ret = mpv_set_property_string(reinterpret_cast<mpv_handle *>(handle), nameStr, valueStr);
    env->ReleaseStringUTFChars(name, nameStr);
    env->ReleaseStringUTFChars(value, valueStr);
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_set_property_string", ret);
    }
    return resultSuccess(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyLongN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name, jlong value) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    const auto ret = mpv_set_property(reinterpret_cast<mpv_handle *>(handle), nameStr, MPV_FORMAT_INT64, &value);
    env->ReleaseStringUTFChars(name, nameStr);
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_set_property int64", ret);
    }
    return resultSuccess(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyDoubleN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name, jdouble value) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    const auto ret = mpv_set_property(reinterpret_cast<mpv_handle *>(handle), nameStr, MPV_FORMAT_DOUBLE, &value);
    env->ReleaseStringUTFChars(name, nameStr);
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_set_property double", ret);
    }
    return resultSuccess(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyFlagN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name, jboolean value) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    const auto ret = mpv_set_property(reinterpret_cast<mpv_handle *>(handle), nameStr, MPV_FORMAT_FLAG, &value);
    env->ReleaseStringUTFChars(name, nameStr);
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_set_property flag", ret);
    }
    return resultSuccess(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_observePropertyStringN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name, const jlong replyUserdata) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    const auto ret = mpv_observe_property(reinterpret_cast<mpv_handle *>(handle), replyUserdata, nameStr,
                                          MPV_FORMAT_STRING);
    env->ReleaseStringUTFChars(name, nameStr);
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_observe_property", ret);
    }
    return resultSuccess(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_observePropertyLongN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name, const jlong replyUserdata) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    const auto ret = mpv_observe_property(reinterpret_cast<mpv_handle *>(handle), replyUserdata, nameStr,
                                          MPV_FORMAT_INT64);
    env->ReleaseStringUTFChars(name, nameStr);
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_observe_property", ret);
    }
    return resultSuccess(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_observePropertyDoubleN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name, const jlong replyUserdata) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    const auto ret = mpv_observe_property(reinterpret_cast<mpv_handle *>(handle), replyUserdata, nameStr,
                                          MPV_FORMAT_DOUBLE);
    env->ReleaseStringUTFChars(name, nameStr);
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_observe_property", ret);
    }
    return resultSuccess(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_observePropertyFlagN(
    JNIEnv *env, jobject thiz, const jlong handle, jstring name, const jlong replyUserdata) {
    const char *nameStr = env->GetStringUTFChars(name, nullptr);
    const auto ret = mpv_observe_property(reinterpret_cast<mpv_handle *>(handle), replyUserdata, nameStr,
                                          MPV_FORMAT_FLAG);
    env->ReleaseStringUTFChars(name, nameStr);
    if (ret < 0) {
        return mpvResultFailure(env, "mpv_observe_property", ret);
    }
    return resultSuccess(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_unobservePropertyN(
    JNIEnv *env, jobject thiz, const jlong handle, const jlong replyUserdata) {
    if (const auto ret = mpv_unobserve_property(reinterpret_cast<mpv_handle *>(handle), replyUserdata); ret < 0) {
        return mpvResultFailure(env, "mpv_unobserve_property", ret);
    }
    return resultSuccess(env);
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_initializeN(
    JNIEnv *env, jobject thiz, const jlong handle) {
    if (const auto ret = mpv_initialize(reinterpret_cast<mpv_handle *>(handle)); ret < 0) {
        return mpvResultFailure(env, "mpv_initialize", ret);
    }
    return resultSuccess(env);
}

struct RenderContext {
    mpv_render_context *handle;
    jobject gref;
    std::shared_ptr<MpvPlatformContext> platformContext;
};

// Rendering
JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_createRenderN(
    JNIEnv *env, jobject thiz, const jlong mpvHandle, jobject self, const jboolean advancedControl) {
    std::vector<mpv_render_param> params{
        {MPV_RENDER_PARAM_API_TYPE, const_cast<char *>(MPV_RENDER_API_TYPE_OPENGL)},
    };

    const auto platformContext = populatePlatformMpvParams(env, params);

    JavaVM *jvm;
    if (const auto ret = env->GetJavaVM(&jvm); ret != JNI_OK) {
        std::cerr << "Failed to get JavaVM" << std::endl;
        return mpvResultFailure(env, "GetJavaVM", MPV_ERROR_GENERIC);
    }
    const auto object = env->NewGlobalRef(self);

    mpv_opengl_init_params gl_params{
        .get_proc_address = fnptr<void *(void *, const char *)>([jvm](void *opaque, const char *name) -> void * {
            const auto javaRender = static_cast<jobject>(opaque);
            JNIEnv *jni_env;
            const auto res = jvm->AttachCurrentThread(reinterpret_cast<void **>(&jni_env), nullptr);
            if (res != JNI_OK) {
                std::cerr << "Failed to attach current thread" << std::endl;
                return nullptr;
            }

            const auto glProcMethod = jni_env->GetMethodID(jni_env->GetObjectClass(javaRender), "getGlProc",
                                                           "(Ljava/lang/String;)J");
            if (glProcMethod == nullptr) {
                std::cerr << "Method not found: getGlProc" << std::endl;
                return nullptr;
            }

            const auto nameStr = jni_env->NewStringUTF(name);
            const auto ret = jni_env->CallLongMethod(javaRender, glProcMethod, nameStr);
            jni_env->DeleteLocalRef(nameStr);
            jvm->DetachCurrentThread();
            return reinterpret_cast<void *>(ret);
        }),
        .get_proc_address_ctx = object,
    };
    params.push_back({MPV_RENDER_PARAM_OPENGL_INIT_PARAMS, &gl_params});
    int advControl = advancedControl ? 1 : 0;
    params.push_back({MPV_RENDER_PARAM_ADVANCED_CONTROL, &advControl});
    params.push_back({MPV_RENDER_PARAM_INVALID, nullptr});

    mpv_render_context *handle{nullptr};
    if (const auto ret = mpv_render_context_create(&handle, reinterpret_cast<mpv_handle *>(mpvHandle), params.data());
        ret < 0) {
        env->DeleteGlobalRef(object);
        return mpvResultFailure(env, "mpv_render_context_create", ret);
    }
    const auto ctx = new RenderContext{handle, object, std::move(platformContext)};
    mpv_render_context_set_update_callback(
        handle,
        fnptr<void(void *)>([jvm](void *opaque) {
            const auto render_context = static_cast<RenderContext *>(opaque);
            JNIEnv *jni_env;
            const auto res = jvm->AttachCurrentThread(reinterpret_cast<void **>(&jni_env), nullptr);
            if (res != JNI_OK) {
                std::cerr << "Failed to attach current thread" << std::endl;
                return;
            }
            const auto updateMethod = jni_env->GetMethodID(jni_env->GetObjectClass(render_context->gref),
                                                           "requestUpdate", "()V");
            if (updateMethod == nullptr) {
                std::cerr << "Method not found: requestUpdate" << std::endl;
                return;
            }
            jni_env->CallVoidMethod(render_context->gref, updateMethod);
            jvm->DetachCurrentThread();
        }),
        ctx);
    return resultSuccess(env, reinterpret_cast<jlong>(ctx));
}

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_destroyRenderN(
    JNIEnv *env, jobject thiz, const jlong handle) {
    const auto context = reinterpret_cast<RenderContext *>(handle);
    mpv_render_context_free(context->handle);
    env->DeleteGlobalRef(context->gref);
    context->platformContext.reset();
    delete context;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_renderN(
    JNIEnv *env, jobject thiz, const jlong handle, const GLint fbo, const jint width, const jint height,
    const jint glInternalFormat) {
    const auto context = reinterpret_cast<RenderContext *>(handle);
    const auto flags = mpv_render_context_update(context->handle);
    if (flags & MPV_RENDER_UPDATE_FRAME) {
        mpv_opengl_fbo fboData{
            .fbo = fbo,
            .w = width,
            .h = height,
            .internal_format = glInternalFormat,
        };
        int flipY{0};
        mpv_render_param params[]{
            {MPV_RENDER_PARAM_OPENGL_FBO, &fboData},
            {MPV_RENDER_PARAM_FLIP_Y, &flipY},
            {MPV_RENDER_PARAM_INVALID, nullptr},
        };
        if (const auto ret = mpv_render_context_render(context->handle, params); ret < 0) {
            return mpvResultFailure(env, "mpv_render_context_render", ret);
        }
    }
    return resultSuccess(env);
}
}
