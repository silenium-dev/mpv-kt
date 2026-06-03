#include "JniMpvCallback.hpp"

/*
private interface MPVListener {
    fun onPropertyChanged(name: String, value: Any?)
    fun onPropertyGet(subscriptionId: Long, result: Result<Any?>)
    fun onPropertySet(subscriptionId: Long, result: Result<Unit>)
    fun onCommandReply(subscriptionId: Long, result: Result<Unit>)
}
*/

JniMpvCallback::JniMpvCallback(JNIEnv *env, const jobject thiz)
    : propertyChanged{
          JniCallRef<void, jstring, jobject>(
              env, thiz, "onPropertyChanged", "(Ljava/lang/String;Ljava/lang/Object;)V")
      },
      propertyGet{
          JniCallRef<void, jlong, jobject>(
              env, thiz, "onPropertyGet", "(JLjava/lang/Object;)V")
      },
      propertySet{
          JniCallRef<void, jlong, jobject>(
              env, thiz, "onPropertySet", "(JLjava/lang/Object;)V")
      },
      commandReply{
          JniCallRef<void, jlong, jobject>(
              env, thiz, "onCommandReply", "(JLjava/lang/Object;)V")
      } {
}

void JniMpvCallback::onPropertyChanged(JNIEnv *env, const std::string &name, const jobject result) {
    const auto nameStr = env->NewStringUTF(name.c_str());
    propertyChanged(env, nameStr, result);
    env->DeleteLocalRef(nameStr);
}

void JniMpvCallback::onCommandReply(JNIEnv *env, const long subscriptionId, const jobject result) {
    commandReply(env, subscriptionId, result);
}

void JniMpvCallback::onPropertyGet(JNIEnv *env, const long subscriptionId, const jobject result) {
    propertyGet(env, subscriptionId, result);
}

void JniMpvCallback::onPropertySet(JNIEnv *env, const long subscriptionId, const jobject result) {
    propertySet(env, subscriptionId, result);
}
