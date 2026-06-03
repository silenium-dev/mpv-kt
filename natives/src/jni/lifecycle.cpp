#include "mpv/instance.hpp"
#include "mpv/nodes.hpp"
#include "util/MPVException.hpp"
#include "helper/JniMpvCallback.hpp"
#include "helper/results.hpp"

#include <jni.h>


extern "C" {
JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_createN(
    JNIEnv *env, jobject thiz) {
    JavaVM *jvm;
    if (env->GetJavaVM(&jvm) != JNI_OK) {
        return mpvResultFailure(env, "GetJavaVM", MPV_ERROR_GENERIC);
    }
    CATCHING(
        const auto handle = new MPVInstance(jvm);
        return resultSuccess(env, reinterpret_cast<jlong>(handle));
    )
}

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_destroyN(
    JNIEnv *env, jobject thiz, jlong handle) {
    INSTANCE(handle);
    delete instance;
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setOptionStringN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring name, const jstring value) {
    INSTANCE(handle);
    const auto nameChars = env->GetStringUTFChars(name, nullptr);
    const auto valueChars = env->GetStringUTFChars(value, nullptr);
    const std::string nameStr(nameChars);
    const std::string valueStr(valueChars);
    env->ReleaseStringUTFChars(name, nameChars);
    env->ReleaseStringUTFChars(value, valueChars);
    CATCHING(
        instance->setOption(nameStr, valueStr);
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_initializeN(
    JNIEnv *env, jobject thiz, const jlong handle) {
    INSTANCE(handle);
    CATCHING(
        instance->initialize();
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_setCallbackN(
    JNIEnv *env, jobject thiz, const jlong handle, jobject listener) {
    INSTANCE(handle);
    CATCHING(
        instance->setCallback(std::make_unique<JniMpvCallback>(env, listener));
        return resultSuccess(env);
    )
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_unsetCallbackN(
    JNIEnv *env, jobject thiz, const jlong handle) {
    INSTANCE(handle);
    CATCHING(
        instance->unsetCallback();
        return resultSuccess(env);
    )
}
}
