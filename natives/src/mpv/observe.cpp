#include "instance.hpp"

#include <clocale>
#include <format>
#include <mutex>

#include "util/MPVException.hpp"
#include "helper/JniMpvCallback.hpp"

void MPVInstance::observePropertyString(const std::string &name, int64_t subscriptionId) const {
    const auto ret = mpv_observe_property(m_handle, subscriptionId, name.c_str(), MPV_FORMAT_STRING);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_observe_property");
    }
}

void MPVInstance::observePropertyLong(const std::string &name, int64_t subscriptionId) const {
    const auto ret = mpv_observe_property(m_handle, subscriptionId, name.c_str(), MPV_FORMAT_INT64);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_observe_property");
    }
}

void MPVInstance::observePropertyDouble(const std::string &name, int64_t subscriptionId) const {
    const auto ret = mpv_observe_property(m_handle, subscriptionId, name.c_str(), MPV_FORMAT_DOUBLE);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_observe_property");
    }
}

void MPVInstance::observePropertyFlag(const std::string &name, int64_t subscriptionId) const {
    const auto ret = mpv_observe_property(m_handle, subscriptionId, name.c_str(), MPV_FORMAT_FLAG);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_observe_property");
    }
}

void MPVInstance::observePropertyNode(const std::string &name, int64_t subscriptionId) const {
    const auto ret = mpv_observe_property(m_handle, subscriptionId, name.c_str(), MPV_FORMAT_NODE);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_observe_property");
    }
}

void MPVInstance::unobserveProperty(int64_t subscriptionId) const {
    const auto ret = mpv_unobserve_property(m_handle, subscriptionId);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_unobserve_property");
    }
}
