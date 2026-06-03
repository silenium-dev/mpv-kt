#include "helper/results.hpp"
#include "mpv/instance.hpp"
#include "mpv/renderer.hpp"
#include "util/MPVException.hpp"

#include <mpv/render.h>
#include <jni.h>


extern "C" {
JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_createRenderN(
    JNIEnv *env, jobject thiz, const jlong handle, const jobject callback) {
    INSTANCE(handle);
    JniCallRef<jlong, jstring> glGetProcAddress{env, callback, "glGetProcAddress", "(Ljava/lang/String;)J"};
    JniCallRef<void> renderUpdateCallback{env, callback, "renderUpdateCallback", "()V"};
    CATCHING(
        const auto renderer = new MPVRenderer(instance, std::move(glGetProcAddress), std::move(renderUpdateCallback));
        return resultSuccess(env, reinterpret_cast<jlong>(renderer));
    )
}

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_destroyRenderN(
    JNIEnv *env, jobject thiz, const jlong rendererHandle) {
    RENDERER(rendererHandle);
    delete renderer;
}

JNIEXPORT void JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_startRenderN(
    JNIEnv *env, jobject thiz, const jlong rendererHandle) {
    RENDERER(rendererHandle);
    renderer->start();
}

JNIEXPORT jobject JNICALL Java_dev_silenium_multimedia_core_mpv_MPVKt_renderN(
    JNIEnv *env, jobject thiz, const jlong rendererHandle,
    const jint fbo, const jint width, const jint height, const jint glInternalFormat) {
    RENDERER(rendererHandle);
    CATCHING(
        renderer->render(fbo, width, height, glInternalFormat);
        return resultSuccess(env);
    )
}
}
