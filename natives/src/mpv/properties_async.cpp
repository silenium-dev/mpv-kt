#include "instance.hpp"

#include <format>

#include "util/MPVException.hpp"

void MPVInstance::getPropertyStringAsync(const std::string &name, const int64_t subscriptionId) const {
    const auto ret = mpv_get_property_async(m_handle, subscriptionId, name.c_str(), MPV_FORMAT_STRING);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_get_property_async");
    }
}

void MPVInstance::getPropertyLongAsync(const std::string &name, const int64_t subscriptionId) const {
    const auto ret = mpv_get_property_async(m_handle, subscriptionId, name.c_str(), MPV_FORMAT_INT64);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_get_property_async");
    }
}

void MPVInstance::getPropertyDoubleAsync(const std::string &name, const int64_t subscriptionId) const {
    const auto ret = mpv_get_property_async(m_handle, subscriptionId, name.c_str(), MPV_FORMAT_DOUBLE);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_get_property_async");
    }
}

void MPVInstance::getPropertyFlagAsync(const std::string &name, const int64_t subscriptionId) const {
    const auto ret = mpv_get_property_async(m_handle, subscriptionId, name.c_str(), MPV_FORMAT_FLAG);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_get_property_async");
    }
}

void MPVInstance::getPropertyNodeAsync(const std::string &name, const int64_t subscriptionId) const {
    const auto ret = mpv_get_property_async(m_handle, subscriptionId, name.c_str(), MPV_FORMAT_NODE);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_get_property_async");
    }
}

void MPVInstance::setPropertyAsync(const std::string &name, const std::string &value, const int64_t subscriptionId) const {
    auto valuePtr = value.c_str();
    const auto ret = mpv_set_property_async(m_handle, subscriptionId, name.c_str(), MPV_FORMAT_STRING, &valuePtr);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_set_property_string");
    }
}

void MPVInstance::setPropertyAsync(const std::string &name, int64_t value, const int64_t subscriptionId) const {
    const auto ret = mpv_set_property_async(m_handle, subscriptionId, name.c_str(), MPV_FORMAT_INT64, &value);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_set_property");
    }
}

void MPVInstance::setPropertyAsync(const std::string &name, double value, const int64_t subscriptionId) const {
    const auto ret = mpv_set_property_async(m_handle, subscriptionId, name.c_str(), MPV_FORMAT_DOUBLE, &value);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_set_property");
    }
}

void MPVInstance::setPropertyAsync(const std::string &name, bool value, const int64_t subscriptionId) const {
    const auto ret = mpv_set_property_async(m_handle, subscriptionId, name.c_str(), MPV_FORMAT_FLAG, &value);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_set_property");
    }
}

void MPVInstance::setPropertyAsync(const std::string &name, mpv_node value, const int64_t subscriptionId) const {
    const auto ret = mpv_set_property_async(m_handle, subscriptionId, name.c_str(), MPV_FORMAT_NODE, &value);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_set_property");
    }
}
