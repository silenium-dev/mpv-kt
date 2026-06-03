#include "instance.hpp"

#include <clocale>
#include <format>
#include <mutex>

#include "util/MPVException.hpp"
#include "helper/JniMpvCallback.hpp"

void free_mpv_node(mpv_node *node) {
    mpv_free_node_contents(node);
    delete node;
}

MPVInstance::MPVInstance(JavaVM *jvm): m_jvm(jvm) {
    setlocale(LC_NUMERIC, "C");
    m_handle = mpv_create();
    if (m_handle == nullptr) {
        throw MPVException(MPV_ERROR_NOMEM, "mpv_create");
    }
    m_running = true;
    m_eventDispatcher = std::thread{&MPVInstance::eventLoop, this};
    mpv_set_wakeup_callback(m_handle, &MPVInstance::wakeupCallback, this);
}

MPVInstance::~MPVInstance() {
    m_running = false;
    m_cv.notify_all();
    m_eventDispatcher.join();
    mpv_terminate_destroy(m_handle);
}

void MPVInstance::setOption(const std::string &name, const std::string &value) const {
    const auto ret = mpv_set_option_string(m_handle, name.c_str(), value.c_str());
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "set_option_string");
    }
}

void MPVInstance::setOption(const std::string &name, int64_t value) const {
    const auto ret = mpv_set_option(m_handle, name.c_str(), MPV_FORMAT_INT64, &value);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "set_option_int64");
    }
}

void MPVInstance::setOption(const std::string &name, double value) const {
    const auto ret = mpv_set_option(m_handle, name.c_str(), MPV_FORMAT_DOUBLE, &value);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "set_option_double");
    }
}

void MPVInstance::setOption(const std::string &name, bool value) const {
    const auto ret = mpv_set_option(m_handle, name.c_str(), MPV_FORMAT_FLAG, &value);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "set_option_flag");
    }
}

void MPVInstance::setCallback(std::unique_ptr<MpvCallback> callback) {
    std::unique_lock lock(m_callbackMutex);
    m_callback = std::move(callback);
}

void MPVInstance::unsetCallback() {
    std::unique_lock lock(m_callbackMutex);
    m_callback.reset();
}

void MPVInstance::initialize() const {
    const auto ret = mpv_initialize(m_handle);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_initialize");
    }
}
