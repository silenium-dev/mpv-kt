#include "MPVException.hpp"

#include <format>

#include "../mpv/platform.hpp"
#include "helper/results.hpp"

MPVException::MPVException(const int code, const std::string &operation) : m_code(code), m_operation(operation) {
}

int MPVException::code() const noexcept {
    return m_code;
}

const char *MPVException::what() const noexcept {
    if (m_operation.empty()) {
        return std::format("MPV error: {} ({})", mpv_error_string(m_code), m_code).c_str();
    }
    return std::format("MPV error during {}: {} ({})", m_operation, mpv_error_string(m_code), m_code).c_str();
}

jobject MPVException::toResult(JNIEnv *env) const {
    return mpvResultFailure(env, m_operation.c_str(), m_code);
}
