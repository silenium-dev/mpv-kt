#ifdef TARGET_WINDOWS
#define WIN32_LEAN_AND_MEAN
#include <Windows.h>
#endif
#include <GL/gl.h>
#include <jni.h>
#include <mpv/client.h>

#ifdef TARGET_LINUX
#include <EGL/egl.h>
#define CASE_STR(value) case value: return #value;

const char *eglGetErrorString(const long error) {
    switch (error) {
        CASE_STR(EGL_SUCCESS)
        CASE_STR(EGL_NOT_INITIALIZED)
        CASE_STR(EGL_BAD_ACCESS)
        CASE_STR(EGL_BAD_ALLOC)
        CASE_STR(EGL_BAD_ATTRIBUTE)
        CASE_STR(EGL_BAD_CONTEXT)
        CASE_STR(EGL_BAD_CONFIG)
        CASE_STR(EGL_BAD_CURRENT_SURFACE)
        CASE_STR(EGL_BAD_DISPLAY)
        CASE_STR(EGL_BAD_SURFACE)
        CASE_STR(EGL_BAD_MATCH)
        CASE_STR(EGL_BAD_PARAMETER)
        CASE_STR(EGL_BAD_NATIVE_PIXMAP)
        CASE_STR(EGL_BAD_NATIVE_WINDOW)
        CASE_STR(EGL_CONTEXT_LOST)
        default:
            return "Unknown";
    }
}

#undef CASE_STR
#endif

extern "C" {
JNIEXPORT jstring JNICALL Java_dev_silenium_multimedia_core_util_ErrorsKt_mpvErrorStringN(
    JNIEnv *env,
    jobject thiz,
    const jint error) {
    return env->NewStringUTF(mpv_error_string(error));
}

JNIEXPORT jstring JNICALL Java_dev_silenium_multimedia_core_util_ErrorsKt_glErrorStringN(
    JNIEnv *env,
    jobject thiz,
    const jint error) {
    switch (error) {
        case GL_NO_ERROR:
            return env->NewStringUTF("GL_NO_ERROR");
        case GL_INVALID_ENUM:
            return env->NewStringUTF("GL_INVALID_ENUM");
        case GL_INVALID_VALUE:
            return env->NewStringUTF("GL_INVALID_VALUE");
        case GL_INVALID_OPERATION:
            return env->NewStringUTF("GL_INVALID_OPERATION");
#ifdef GL_INVALID_FRAMEBUFFER_OPERATION
        case GL_INVALID_FRAMEBUFFER_OPERATION:
            return env->NewStringUTF("GL_INVALID_FRAMEBUFFER_OPERATION");
#endif
        case GL_OUT_OF_MEMORY:
            return env->NewStringUTF("GL_OUT_OF_MEMORY");
        case GL_STACK_UNDERFLOW:
            return env->NewStringUTF("GL_STACK_UNDERFLOW");
        case GL_STACK_OVERFLOW:
            return env->NewStringUTF("GL_STACK_OVERFLOW");
        default:
            return env->NewStringUTF("Unknown");
    }
}

#ifdef TARGET_LINUX

JNIEXPORT jstring JNICALL Java_dev_silenium_multimedia_util_ErrorsKt_eglErrorStringN(
    JNIEnv *env,
    jobject thiz,
    jint error
) {
    return env->NewStringUTF(eglGetErrorString(error));
}

#endif
}
