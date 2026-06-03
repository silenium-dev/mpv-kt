#include "mpv/instance.hpp"
#include <jni.h>

#include "helper/results.hpp"
#include "util/MPVException.hpp"

extern "C" {
JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_observePropertyStringN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name, const jlong subscriptionId) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    CATCHING(
        instance->observePropertyString(nameStr, subscriptionId);
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_observePropertyLongN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name, const jlong subscriptionId) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    CATCHING(
        instance->observePropertyLong(nameStr, subscriptionId);
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_observePropertyDoubleN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name, const jlong subscriptionId) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    CATCHING(
        instance->observePropertyDouble(nameStr, subscriptionId);
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_observePropertyFlagN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name, const jlong subscriptionId) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const std::string nameStr{nameChars};
    env->ReleaseStringUTFChars(name, nameChars);
    CATCHING(
        instance->observePropertyFlag(nameStr, subscriptionId);
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_unobservePropertyN(
    JNIEnv *env, jobject thiz, const jlong handle, const jlong subscriptionId) {
    INSTANCE(handle);
    CATCHING(
        instance->unobserveProperty(subscriptionId);
        return resultSuccess(env);
    )
}
}
