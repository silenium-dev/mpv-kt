#include "helper/results.hpp"
#include "mpv/instance.hpp"
#include "../util/MPVException.hpp"

#include <jni.h>
#include <vector>

#include "mpv/nodes.hpp"

extern "C" {
JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_commandN(
    JNIEnv *env, jobject thiz, const jlong handle, const jobjectArray command) {
    INSTANCE(handle);
    const auto size = env->GetArrayLength(command);
    std::vector<std::string> argv(size);
    for (auto i = 0; i < size; i++) {
        const auto arg = env->GetObjectArrayElement(command, i);
        const auto str = env->GetStringUTFChars(static_cast<jstring>(arg), nullptr);
        argv[i] = std::string{str};
        env->ReleaseStringUTFChars(static_cast<jstring>(arg), str);
    }
    CATCHING(
        const auto result = instance->command(argv);
        return mapNode(env, *result);
    );
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_commandStringN(
    JNIEnv *env, jobject thiz, const jlong handle, const jstring command) {
    INSTANCE(handle);
    const char *commandChars = env->GetStringUTFChars(command, nullptr);
    const std::string commandStr{commandChars};
    env->ReleaseStringUTFChars(command, commandChars);
    CATCHING(
        instance->command(commandStr);
        return resultSuccess(env);
    );
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_commandAsyncN(
    JNIEnv *env, jobject thiz, const jlong handle, jobjectArray command, jlong subscriptionId) {
    INSTANCE(handle);
    const auto size = env->GetArrayLength(command);
    std::vector<std::string> argv(size);
    for (auto i = 0; i < size; i++) {
        const auto arg = env->GetObjectArrayElement(command, i);
        const auto str = env->GetStringUTFChars(static_cast<jstring>(arg), nullptr);
        argv[i] = std::string{str};
        env->ReleaseStringUTFChars(static_cast<jstring>(arg), str);
    }
    CATCHING(
        instance->commandAsync(argv, subscriptionId);
        return resultSuccess(env);
    );
}
}
