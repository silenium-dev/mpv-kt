#include "nodes.hpp"

#include <cstring>
#include <iostream>

#include <jni.h>
#include <mutex>
#include <unordered_map>
#include <vector>

static std::mutex classCacheMutex;
static std::unordered_map<std::string, jclass> classCache;
static std::mutex nodeClassCacheMutex;
static std::unordered_map<ClassConstructorRef, NodeClass> nodeClassCache;

jclass nodeClass(JNIEnv *env, const std::string &name) {
    std::lock_guard lock(classCacheMutex);
    if (classCache.contains(name)) {
        return classCache[name];
    }
    const auto clazz = env->FindClass(name.c_str());
    if (clazz == nullptr) {
        throw std::runtime_error("Class not found: " + name);
    }
    const auto globalRef = static_cast<jclass>(env->NewGlobalRef(clazz));
    env->DeleteLocalRef(clazz);
    classCache[name] = globalRef;
    return globalRef;
}

NodeClass nodeClass(JNIEnv *env, const ClassConstructorRef &ref) {
    std::lock_guard lock(nodeClassCacheMutex);
    if (nodeClassCache.contains(ref)) {
        return nodeClassCache.at(ref);
    }
    const NodeClass clazz{env, ref};
    nodeClassCache.emplace(ref, clazz);
    return clazz;
}

ClassConstructorRef::ClassConstructorRef(const std::string &name, const std::string &signature) : m_name(name),
    m_signature(signature) {
}

std::string ClassConstructorRef::name() const {
    return m_name;
}

std::string ClassConstructorRef::signature() const {
    return m_signature;
}

bool ClassConstructorRef::operator==(const ClassConstructorRef &other) const {
    return m_name == other.m_name && m_signature == other.m_signature;
}

NodeClass::NodeClass(JNIEnv *env, const ClassConstructorRef &ref) {
    const auto clazz = env->FindClass(ref.name().c_str());
    if (clazz == nullptr) {
        throw std::runtime_error("Class not found: " + ref.name());
    }
    m_clazz = static_cast<jclass>(env->NewGlobalRef(clazz));
    env->DeleteLocalRef(clazz);
    m_ctor = env->GetMethodID(m_clazz, "<init>", ref.signature().c_str());
    if (m_ctor == nullptr) {
        throw std::runtime_error("Constructor not found: <init> " + ref.signature());
    }
}

jobject NodeClass::newInstance(JNIEnv *env, ...) const {
    va_list args;
    va_start(args, env);
    const auto result = env->NewObjectV(m_clazz, m_ctor, args);
    va_end(args);
    return result;
}

jclass NodeClass::clazz() const {
    return m_clazz;
}

std::string formatName(const mpv_format fmt) {
    switch (fmt) {
        case MPV_FORMAT_STRING:
            return "MPV_FORMAT_STRING";
        case MPV_FORMAT_FLAG:
            return "MPV_FORMAT_FLAG";
        case MPV_FORMAT_INT64:
            return "MPV_FORMAT_INT64";
        case MPV_FORMAT_DOUBLE:
            return "MPV_FORMAT_DOUBLE";
        case MPV_FORMAT_NODE_ARRAY:
            return "MPV_FORMAT_NODE_ARRAY";
        case MPV_FORMAT_NODE_MAP:
            return "MPV_FORMAT_NODE_MAP";
        case MPV_FORMAT_BYTE_ARRAY:
            return "MPV_FORMAT_BYTE_ARRAY";
        case MPV_FORMAT_NONE:
            return "MPV_FORMAT_NONE";
        default:
            return "Unknown";
    }
}

jobject mapNode(JNIEnv *env, const mpv_node node) {
    switch (node.format) {
        case MPV_FORMAT_STRING:
            [[fallthrough]];
        case MPV_FORMAT_OSD_STRING: {
            const std::string str{node.u.string};
            return nodeClass(env, NODE_STRING_CLASS).newInstance(env, env->NewStringUTF(str.c_str()));
        }
        case MPV_FORMAT_FLAG: {
            return nodeClass(env, NODE_FLAG_CLASS).newInstance(env, static_cast<jboolean>(node.u.flag));
        }
        case MPV_FORMAT_INT64: {
            return nodeClass(env, NODE_LONG_CLASS).newInstance(env, static_cast<jlong>(node.u.int64));
        }
        case MPV_FORMAT_DOUBLE: {
            return nodeClass(env, NODE_DOUBLE_CLASS).newInstance(env, node.u.double_);
        }
        case MPV_FORMAT_NODE_ARRAY: {
            const auto result = env->NewObjectArray(node.u.list->num, nodeClass(env, NODE_BASE_CLASS), nullptr);
            for (int i = 0; i < node.u.list->num; i++) {
                const auto mapped = mapNode(env, node.u.list->values[i]);
                env->SetObjectArrayElement(result, i, mapped);
            }
            return nodeClass(env, NODE_LIST_CLASS).newInstance(env, result);
        }
        case MPV_FORMAT_NODE_MAP: {
            const auto entries = env->NewObjectArray(
                node.u.list->num,
                nodeClass(env, NODE_MAP_ENTRY_CLASS).clazz(),
                nullptr
            );
            for (int i = 0; i < node.u.list->num; i++) {
                const auto value = mapNode(env, node.u.list->values[i]);
                const auto key = env->NewStringUTF(node.u.list->keys[i]);
                const auto entry = nodeClass(env, NODE_MAP_ENTRY_CLASS).newInstance(env, key, value);
                env->SetObjectArrayElement(entries, i, entry);
            }
            return nodeClass(env, NODE_MAP_CLASS).newInstance(env, entries);
        }
        case MPV_FORMAT_BYTE_ARRAY: {
            const auto result = env->NewByteArray(node.u.ba->size);
            if (result == nullptr || env->ExceptionCheck()) {
                return nullptr;
            }
            env->SetByteArrayRegion(result, 0, node.u.ba->size, static_cast<const jbyte *>(node.u.ba->data));
            if (env->ExceptionCheck()) {
                return nullptr;
            }
            const auto instance = nodeClass(env, NODE_BYTEARRAY_CLASS).newInstance(env, result);
            if (instance == nullptr || env->ExceptionCheck()) {
                return nullptr;
            }
            return instance;
        }
        case MPV_FORMAT_NONE: {
            const auto clazz = nodeClass(env, NODE_NONE_CLASS);
            const auto instanceField = env->GetStaticFieldID(clazz, "INSTANCE",
                                                             std::format("L{};", NODE_NONE_CLASS).c_str());
            if (instanceField == nullptr) {
                throw std::runtime_error("Static field not found: INSTANCE");
            }
            return env->GetStaticObjectField(clazz, instanceField);
        }
        case MPV_FORMAT_NODE: // Not possible
            [[fallthrough]];
        default:
            throw std::runtime_error("Unsupported format: " + formatName(node.format));
    }
}

mpv_format NodeClass::getFormat(JNIEnv *env, jobject instance) {
    const auto clazz = env->GetObjectClass(instance);
    if (clazz == nullptr) {
        throw std::runtime_error("Class not found");
    }
    const auto formatGetter = env->GetMethodID(clazz, "getFormat", "()Ldev/silenium/multimedia/core/mpv/MpvFormat;");
    if (formatGetter == nullptr) {
        throw std::runtime_error("Method not found: getFormat");
    }
    const auto format = env->CallObjectMethod(instance, formatGetter);
    if (env->ExceptionCheck()) {
        throw std::runtime_error("Failed method invocation to getFormat");
    }
    const auto enumClazz = env->GetObjectClass(format);
    if (enumClazz == nullptr) {
        throw std::runtime_error("Class not found for format enum");
    }
    const auto formatValueGetter = env->GetMethodID(enumClazz, "getValue", "()I");
    if (formatValueGetter == nullptr) {
        throw std::runtime_error("Method not found in format enum: getValue");
    }
    const auto formatValue = env->CallIntMethod(format, formatValueGetter);
    if (env->ExceptionCheck()) {
        throw std::runtime_error("Failed method invocation to getValue");
    }
    return static_cast<mpv_format>(formatValue);
}

template<typename T>
T getValue(JNIEnv *env, const jobject node, const char *signature) {
    const auto clazz = env->GetObjectClass(node);
    if (clazz == nullptr) {
        throw std::runtime_error("Class not found");
    }
    const auto valueGetter = env->GetMethodID(clazz, "getValue", signature);
    if (valueGetter == nullptr) {
        throw std::runtime_error("Method not found: getValue");
    }
    const auto result = env->CallObjectMethod(node, valueGetter);
    if (env->ExceptionCheck()) {
        throw std::runtime_error("Failed method invocation to getValue");
    }
    return static_cast<T>(result);
}

template<>
jlong getValue(JNIEnv *env, const jobject node, const char *signature) {
    const auto clazz = env->GetObjectClass(node);
    if (clazz == nullptr) {
        throw std::runtime_error("Class not found");
    }
    const auto valueGetter = env->GetMethodID(clazz, "getValue", "()J");
    if (valueGetter == nullptr) {
        throw std::runtime_error("Method not found: getValue");
    }
    const auto result = env->CallLongMethod(node, valueGetter);
    if (env->ExceptionCheck()) {
        throw std::runtime_error("Failed method invocation to getValue");
    }
    return result;
}

template<>
jboolean getValue(JNIEnv *env, const jobject node, const char *signature) {
    const auto clazz = env->GetObjectClass(node);
    if (clazz == nullptr) {
        throw std::runtime_error("Class not found");
    }
    const auto valueGetter = env->GetMethodID(clazz, "getValue", "()Z");
    if (valueGetter == nullptr) {
        throw std::runtime_error("Method not found: getValue");
    }
    const auto result = env->CallBooleanMethod(node, valueGetter);
    if (env->ExceptionCheck()) {
        throw std::runtime_error("Failed method invocation to getValue");
    }
    return result;
}

template<>
jdouble getValue(JNIEnv *env, const jobject node, const char *signature) {
    const auto clazz = env->GetObjectClass(node);
    if (clazz == nullptr) {
        throw std::runtime_error("Class not found");
    }
    const auto valueGetter = env->GetMethodID(clazz, "getValue", "()D");
    if (valueGetter == nullptr) {
        throw std::runtime_error("Method not found: getValue");
    }
    const auto result = env->CallDoubleMethod(node, valueGetter);
    if (env->ExceptionCheck()) {
        throw std::runtime_error("Failed method invocation to getValue");
    }
    return result;
}

jobjectArray getAsArray(JNIEnv *env, const jobject node, const char *elementSignature) {
    const auto clazz = env->GetObjectClass(node);
    if (clazz == nullptr) {
        throw std::runtime_error("Class not found");
    }
    const auto valueGetter = env->GetMethodID(clazz, "getAsArray", std::format("()[L{};", elementSignature).c_str());
    if (valueGetter == nullptr) {
        throw std::runtime_error("Method not found: getAsArray");
    }
    const auto result = env->CallObjectMethod(node, valueGetter);
    if (env->ExceptionCheck()) {
        throw std::runtime_error("Failed method invocation to getAsArray");
    }
    return static_cast<jobjectArray>(result);
}

jstring getKey(JNIEnv *env, const jobject mapEntry) {
    const auto clazz = env->GetObjectClass(mapEntry);
    if (clazz == nullptr) {
        throw std::runtime_error("Class not found");
    }
    const auto keyGetter = env->GetMethodID(clazz, "getKey", "()Ljava/lang/String;");
    if (keyGetter == nullptr) {
        throw std::runtime_error("Method not found: getKey");
    }
    const auto result = env->CallObjectMethod(mapEntry, keyGetter);
    if (env->ExceptionCheck()) {
        throw std::runtime_error("Failed method invocation to getKey");
    }
    return static_cast<jstring>(result);
}

jobject getValue(JNIEnv *env, const jobject mapEntry) {
    const auto clazz = env->GetObjectClass(mapEntry);
    if (clazz == nullptr) {
        throw std::runtime_error("Class not found");
    }
    const auto valueGetter = env->GetMethodID(clazz, "getValue", std::format("()L{};", NODE_BASE_CLASS).c_str());
    if (valueGetter == nullptr) {
        throw std::runtime_error("Method not found: getValue");
    }
    const auto result = env->CallObjectMethod(mapEntry, valueGetter);
    if (env->ExceptionCheck()) {
        throw std::runtime_error("Failed method invocation to getValue");
    }
    return result;
}

mpv_node mapNode(JNIEnv *env, const jobject node) {
    if (node == nullptr) {
        return {};
    }
    const auto format = NodeClass::getFormat(env, node);
    const auto clazz = env->GetObjectClass(node);
    if (clazz == nullptr) {
        throw std::runtime_error("Class not found");
    }
    switch (format) {
        case MPV_FORMAT_NONE: {
            return {};
        }
        case MPV_FORMAT_OSD_STRING:
            [[fallthrough]];
        case MPV_FORMAT_STRING: {
            const auto value = getValue<jstring>(env, node, "()Ljava/lang/String;");
            const auto length = env->GetStringUTFLength(value);

            const auto valueStr = env->GetStringUTFChars(value, nullptr);
            const auto str = new char[length + 1];
            std::strcpy(str, valueStr);
            env->ReleaseStringUTFChars(value, valueStr);

            const auto result = mpv_node{
                .u = {
                    .string = str,
                },
                .format = format,
            };
            return result;
        }
        case MPV_FORMAT_FLAG: {
            const auto value = getValue<jboolean>(env, node, "()Z");
            const auto result = mpv_node{
                .u = {
                    .flag = value,
                },
                .format = format,
            };
            return result;
        }
        case MPV_FORMAT_INT64: {
            const auto value = getValue<jlong>(env, node, "()J");
            const auto result = mpv_node{
                .u = {
                    .int64 = value,
                },
                .format = format,
            };
            return result;
        }
        case MPV_FORMAT_DOUBLE: {
            const auto value = getValue<jdouble>(env, node, "()D");
            const auto result = mpv_node{
                .u = {
                    .double_ = value,
                },
                .format = format,
            };
            return result;
        }
        case MPV_FORMAT_BYTE_ARRAY: {
            const auto value = getValue<jbyteArray>(env, node, "()[B");
            const auto length = env->GetArrayLength(value);
            const auto valuePtr = env->GetByteArrayElements(value, nullptr);
            const auto ptr = new jbyte[length];
            std::memcpy(ptr, valuePtr, length);
            const auto ba = new mpv_byte_array{
                .data = ptr,
                .size = static_cast<size_t>(length),
            };
            const auto result = mpv_node{
                .u = {
                    .ba = ba,
                },
                .format = format,
            };
            return result;
        }

        case MPV_FORMAT_NODE_ARRAY: {
            const auto array = getAsArray(env, node, NODE_BASE_CLASS);
            const auto length = env->GetArrayLength(array);
            const auto result = new mpv_node_list{
                .num = static_cast<int>(length),
                .values = new mpv_node[length],
            };
            for (int i = 0; i < length; i++) {
                const auto mapped = mapNode(env, env->GetObjectArrayElement(array, i));
                result->values[i] = mapped;
            }
            return {
                .u = {
                    .list = result
                },
                .format = format,
            };
        }
        case MPV_FORMAT_NODE_MAP: {
            const auto array = getAsArray(env, node, NODE_MAP_ENTRY_CLASS.name().c_str());
            const auto length = env->GetArrayLength(array);
            const auto result = new mpv_node_list{
                .num = static_cast<int>(length),
                .values = new mpv_node[length],
                .keys = new char *[length],
            };

            for (int i = 0; i < length; i++) {
                const auto entry = env->GetObjectArrayElement(array, i);
                const auto key = getKey(env, entry);
                const auto value = getValue(env, entry);

                const auto keyStr = env->GetStringUTFChars(key, nullptr);
                const auto keyPtr = new char[env->GetStringUTFLength(key) + 1];
                std::strcpy(keyPtr, keyStr);
                env->ReleaseStringUTFChars(key, keyStr);

                result->keys[i] = keyPtr;
                result->values[i] = mapNode(env, value);
            }
            return {
                .u = {
                    .list = result
                },
                .format = format,
            };
        }
        case MPV_FORMAT_NODE:
            throw std::runtime_error("Cannot convert generic node, check your Java-side format specifier");
        default:
            throw std::runtime_error("Unsupported format: " + formatName(format));
    }
}
