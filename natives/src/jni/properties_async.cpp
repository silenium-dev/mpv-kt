#include "mpv/instance.hpp"
#include <jni.h>

#include "helper/results.hpp"
#include "mpv/nodes.hpp"
#include "util/MPVException.hpp"

extern "C" {
JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyStringAsyncN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name, const jlong subscriptionId) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    CATCHING(
        instance->getPropertyStringAsync(nameStr, subscriptionId);
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyLongAsyncN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name, const jlong subscriptionId) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    CATCHING(
        instance->getPropertyLongAsync(nameStr, subscriptionId);
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyDoubleAsyncN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name, const jlong subscriptionId) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    CATCHING(
        instance->getPropertyDoubleAsync(nameStr, subscriptionId);
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyFlagAsyncN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name, const jlong subscriptionId) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    CATCHING(
        instance->getPropertyFlagAsync(nameStr, subscriptionId);
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_getPropertyNodeAsyncN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name, const jlong subscriptionId) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    CATCHING(
        instance->getPropertyNodeAsync(nameStr, subscriptionId);
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyStringAsyncN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name, const jstring value,
    const jlong subscriptionId) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const auto valueChars = env->GetStringUTFChars(value, nullptr);
    const std::string nameStr{nameChars};
    const std::string valueStr{valueChars};
    env->ReleaseStringUTFChars(name, nameChars);
    env->ReleaseStringUTFChars(value, valueChars);
    CATCHING(
        instance->setPropertyAsync(nameStr, valueStr, subscriptionId);
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyLongAsyncN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name, const jlong value, const jlong subscriptionId) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    CATCHING(
        instance->setPropertyAsync(nameStr, value, subscriptionId);
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyDoubleAsyncN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name, const jdouble value,
    const jlong subscriptionId) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    CATCHING(
        instance->setPropertyAsync(nameStr, value, subscriptionId);
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyFlagAsyncN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name, const jboolean value,
    const jlong subscriptionId) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    CATCHING(
        instance->setPropertyAsync(nameStr, static_cast<bool>(value), subscriptionId);
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setPropertyNodeAsyncN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name, const jobject value,
    const jlong subscriptionId) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    const auto node = mapNode(env, value);
    CATCHING(
        instance->setPropertyAsync(nameStr, node, subscriptionId);
        return resultSuccess(env);
    )
}
}
