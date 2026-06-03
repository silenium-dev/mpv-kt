#include "mpv/instance.hpp"
#include <jni.h>

#include "helper/results.hpp"
#include "../util/MPVException.hpp"
#include "mpv/nodes.hpp"

extern "C" {
JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyStringN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    CATCHING(
        const auto value = instance->getPropertyString(nameStr);
        return env->NewStringUTF(value.c_str());
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyLongN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    CATCHING(
        const auto value = instance->getPropertyLong(nameStr);
        return boxedLong(env, value);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyDoubleN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    CATCHING(
        const auto value = instance->getPropertyDouble(nameStr);
        return boxedDouble(env, value);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyFlagN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    CATCHING(
        const auto value = instance->getPropertyFlag(nameStr);
        return boxedBool(env, value);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyNodeN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    CATCHING(
        auto value = instance->getPropertyNode(nameStr);
        const auto result = mapNode(env, value);
        mpv_free_node_contents(&value);
        return result;
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyStringN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name, const jstring value) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    const auto valueChars = env->GetStringUTFChars(value, nullptr);
    const std::string valueStr{valueChars};
    env->ReleaseStringUTFChars(value, valueChars);
    CATCHING(
        instance->setProperty(nameStr, valueStr);
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyLongN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name, const jlong value) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    CATCHING(
        instance->setProperty(nameStr, value);
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyDoubleN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name, const jdouble value) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    CATCHING(
        instance->setProperty(nameStr, value);
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyFlagN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name, const jboolean value) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    CATCHING(
        instance->setProperty(nameStr, static_cast<bool>(value));
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyNodeN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name, const jobject value) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    const auto node = mapNode(env, value);
    CATCHING(
        instance->setProperty(nameStr, node);
        return resultSuccess(env);
    )
}
}
