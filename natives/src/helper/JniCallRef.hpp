#ifndef NATIVES_JNICALLREF_HPP
#define NATIVES_JNICALLREF_HPP

#include <iostream>
#include <jni.h>
#include <memory>
#include <stdexcept>
#include <type_traits>

namespace detail {
    class AttachedEnv {
    public:
        explicit AttachedEnv(JavaVM *jvm) : m_jvm(jvm), m_env(nullptr) {
            if (m_jvm->AttachCurrentThread(reinterpret_cast<void **>(&m_env), nullptr) != JNI_OK) {
                throw std::runtime_error("Failed to attach current thread");
            }
        }

        ~AttachedEnv() {
            if (m_jvm != nullptr) {
                m_jvm->DetachCurrentThread();
            }
        }

        JNIEnv *get() const {
            return m_env;
        }

    private:
        JavaVM *m_jvm;
        JNIEnv *m_env;
    };

    template<typename Return>
    struct JniCallTraits;

    template<>
    struct JniCallTraits<void> {
        template<typename... Args>
        static void call(JNIEnv *env, const jobject obj, const jmethodID method, Args... args) {
            env->CallVoidMethod(obj, method, args...);
        }
    };

    template<>
    struct JniCallTraits<jboolean> {
        template<typename... Args>
        static jboolean call(JNIEnv *env, const jobject obj, const jmethodID method, Args... args) {
            return env->CallBooleanMethod(obj, method, args...);
        }
    };

    template<>
    struct JniCallTraits<jbyte> {
        template<typename... Args>
        static jbyte call(JNIEnv *env, const jobject obj, const jmethodID method, Args... args) {
            return env->CallByteMethod(obj, method, args...);
        }
    };

    template<>
    struct JniCallTraits<jchar> {
        template<typename... Args>
        static jchar call(JNIEnv *env, const jobject obj, const jmethodID method, Args... args) {
            return env->CallCharMethod(obj, method, args...);
        }
    };

    template<>
    struct JniCallTraits<jshort> {
        template<typename... Args>
        static jshort call(JNIEnv *env, const jobject obj, const jmethodID method, Args... args) {
            return env->CallShortMethod(obj, method, args...);
        }
    };

    template<>
    struct JniCallTraits<jint> {
        template<typename... Args>
        static jint call(JNIEnv *env, const jobject obj, const jmethodID method, Args... args) {
            return env->CallIntMethod(obj, method, args...);
        }
    };

    template<>
    struct JniCallTraits<jlong> {
        template<typename... Args>
        static jlong call(JNIEnv *env, const jobject obj, const jmethodID method, Args... args) {
            return env->CallLongMethod(obj, method, args...);
        }
    };

    template<>
    struct JniCallTraits<jfloat> {
        template<typename... Args>
        static jfloat call(JNIEnv *env, const jobject obj, const jmethodID method, Args... args) {
            return env->CallFloatMethod(obj, method, args...);
        }
    };

    template<>
    struct JniCallTraits<jdouble> {
        template<typename... Args>
        static jdouble call(JNIEnv *env, const jobject obj, const jmethodID method, Args... args) {
            return env->CallDoubleMethod(obj, method, args...);
        }
    };

    template<>
    struct JniCallTraits<jobject> {
        template<typename... Args>
        static jobject call(JNIEnv *env, const jobject obj, const jmethodID method, Args... args) {
            return env->CallObjectMethod(obj, method, args...);
        }
    };
} // namespace detail

template<typename Return, typename... Args>
class JniCallRef {
public:
    JniCallRef(JNIEnv *env, const jobject obj, const std::string &method, const std::string &signature) {
        if (env->GetJavaVM(&m_jvm) != JNI_OK) {
            throw std::runtime_error("Failed to get Java VM");
        }
        if (m_jvm == nullptr) {
            throw std::runtime_error("Failed to get Java VM");
        }
        m_method = env->GetMethodID(env->GetObjectClass(obj), method.c_str(), signature.c_str());
        if (m_method == nullptr) {
            throw std::runtime_error("Method not found: " + method);
        }
        m_obj = env->NewGlobalRef(obj);
        if (m_obj == nullptr) {
            throw std::runtime_error("Failed to create global reference for object");
        }
    }

    ~JniCallRef() {
        if (m_jvm == nullptr && m_obj == nullptr) {
            return;
        }
        if (m_jvm == nullptr && m_obj != nullptr) {
            std::cerr << "No jvm, failed to destroy global object ref, this is a memory leak!!!" << std::endl;
            return;
        }
        const detail::AttachedEnv env{m_jvm};
        env.get()->DeleteGlobalRef(m_obj);
    }

    JniCallRef(const JniCallRef &other) {
        const detail::AttachedEnv env{other.m_jvm};
        m_jvm = other.m_jvm;
        m_obj = env.get()->NewGlobalRef(other.m_obj);
        if (env.get()->ExceptionCheck()) {
            env.get()->ExceptionDescribe();
            env.get()->ExceptionClear();
        }
        m_method = other.m_method;
    };

    JniCallRef &operator=(const JniCallRef &other) {
        if (this == &other) {
            return *this;
        }
        if (m_jvm != nullptr && m_obj != nullptr) {
            const detail::AttachedEnv env{m_jvm};
            env.get()->DeleteGlobalRef(m_obj);
            if (env.get()->ExceptionCheck()) {
                env.get()->ExceptionDescribe();
                env.get()->ExceptionClear();
            }
        }
        const detail::AttachedEnv otherEnv{other.m_jvm};
        m_jvm = other.m_jvm;
        m_obj = otherEnv.get()->NewGlobalRef(other.m_obj);
        m_method = other.m_method;
        return *this;
    }

    JniCallRef(JniCallRef &&other) noexcept {
        m_jvm = other.m_jvm;
        m_obj = other.m_obj;
        m_method = other.m_method;
        other.m_jvm = nullptr;
        other.m_obj = nullptr;
        other.m_method = nullptr;
    }

    JniCallRef &operator=(JniCallRef &&other) noexcept {
        if (this == &other) {
            return *this;
        }
        if (m_jvm != nullptr && m_obj != nullptr) {
            const detail::AttachedEnv env{m_jvm};
            env.get()->DeleteGlobalRef(m_obj);
            if (env.get()->ExceptionCheck()) {
                env.get()->ExceptionDescribe();
                env.get()->ExceptionClear();
            }
        }
        m_jvm = other.m_jvm;
        m_obj = other.m_obj;
        m_method = other.m_method;
        other.m_jvm = nullptr;
        other.m_obj = nullptr;
        other.m_method = nullptr;
        return *this;
    }

    Return operator()(JNIEnv *env, Args... args) const {
        if constexpr (std::is_void_v<Return>) {
            detail::JniCallTraits<void>::call(env, m_obj, m_method, args...);
            return;
        } else {
            return detail::JniCallTraits<Return>::call(env, m_obj, m_method, args...);
        }
    }

    std::unique_ptr<detail::AttachedEnv> attach() const {
        return std::make_unique<detail::AttachedEnv>(m_jvm);
    }

private:
    JavaVM *m_jvm{nullptr};
    jobject m_obj{nullptr};
    jmethodID m_method{nullptr};
};

#endif // NATIVES_JNICALLREF_HPP
