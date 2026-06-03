#ifndef NATIVES_MPVEXCEPTION_HPP
#define NATIVES_MPVEXCEPTION_HPP
#include <jni.h>
#include <exception>
#include <string>


#define CATCHING(call) try { \
    call \
} catch (MPVException &e) { \
    return e.toResult(env); \
}

class MPVException : public std::exception {
public:
    explicit MPVException(int code, const std::string &operation = "");

    const char *what() const noexcept override;

    int code() const noexcept;

    jobject toResult(JNIEnv *env) const;

private:
    const int m_code;
    const std::string m_operation;
};


#endif //NATIVES_MPVEXCEPTION_HPP
