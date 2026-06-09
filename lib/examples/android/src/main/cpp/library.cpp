#include <jni.h>
#include <EGL/egl.h>
#include <cstdint>
#include <iostream>

extern "C" {
    JNIEXPORT jlong JNICALL
    Java_dev_silenium_mpv_examples_android_EGLGetProcAddress_eglGetProcAddressN(JNIEnv *env, jobject thiz, jstring name) {
        const char *cName = env->GetStringUTFChars(name, nullptr);
        void *ptr = reinterpret_cast<void*>(eglGetProcAddress(cName));
        env->ReleaseStringUTFChars(name, cName);
        return reinterpret_cast<intptr_t>(ptr);
    }
}
