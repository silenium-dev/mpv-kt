#ifndef NATIVES_JNIMPVCALLBACK_HPP
#define NATIVES_JNIMPVCALLBACK_HPP

#include "JniCallRef.hpp"
#include "MpvCallback.hpp"

#include <jni.h>
#include <string>


class JniMpvCallback : public MpvCallback {
public:
    explicit JniMpvCallback(JNIEnv *env, jobject thiz);

    void onPropertyChanged(JNIEnv *env, const std::string &name, jobject result) override;

    void onPropertyGet(JNIEnv *env, long subscriptionId, jobject result) override;

    void onPropertySet(JNIEnv *env, long subscriptionId, jobject result) override;

    void onCommandReply(JNIEnv *env, long subscriptionId, jobject result) override;

private:
    JniCallRef<void, jstring, jobject> propertyChanged;
    JniCallRef<void, jlong, jobject> propertyGet;
    JniCallRef<void, jlong, jobject> propertySet;
    JniCallRef<void, jlong, jobject> commandReply;
};


#endif //NATIVES_JNIMPVCALLBACK_HPP
