#ifndef NATIVES_MPVCALLBACK_HPP
#define NATIVES_MPVCALLBACK_HPP

#include <jni.h>
#include <string>


class MpvCallback {
public:
    virtual ~MpvCallback() = default;

    virtual void onPropertyChanged(JNIEnv *env, const std::string &name, jobject result) = 0;

    virtual void onPropertyGet(JNIEnv *env, long subscriptionId, jobject result) = 0;

    virtual void onPropertySet(JNIEnv *env, long subscriptionId, jobject result) = 0;

    virtual void onCommandReply(JNIEnv *env, long subscriptionId, jobject result) = 0;
};


#endif //NATIVES_MPVCALLBACK_HPP
