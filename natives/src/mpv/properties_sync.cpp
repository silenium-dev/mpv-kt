#include "instance.hpp"

#include <format>

#include "util/MPVException.hpp"

std::string MPVInstance::getPropertyString(const std::string &name) const {
    char *value{nullptr};
    const auto ret = mpv_get_property(m_handle, name.c_str(), MPV_FORMAT_STRING, &value);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_get_property");
    }
    const auto result = std::string{value};
    mpv_free(value);
    return result;
}

int64_t MPVInstance::getPropertyLong(const std::string &name) const {
    int64_t value{0};
    const auto ret = mpv_get_property(m_handle, name.c_str(), MPV_FORMAT_INT64, &value);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_get_property");
    }
    return value;
}

double MPVInstance::getPropertyDouble(const std::string &name) const {
    double value{0};
    const auto ret = mpv_get_property(m_handle, name.c_str(), MPV_FORMAT_DOUBLE, &value);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_get_property");
    }
    return value;
}

bool MPVInstance::getPropertyFlag(const std::string &name) const {
    bool value{false};
    const auto ret = mpv_get_property(m_handle, name.c_str(), MPV_FORMAT_FLAG, &value);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_get_property");
    }
    return value;
}

mpv_node MPVInstance::getPropertyNode(const std::string &name) const {
    mpv_node value;
    const auto ret = mpv_get_property(m_handle, name.c_str(), MPV_FORMAT_NODE, &value);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_get_property");
    }
    return value;
}

void MPVInstance::setProperty(const std::string &name, const std::string &value) const {
    const auto ret = mpv_set_property_string(m_handle, name.c_str(), value.c_str());
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_set_property_string");
    }
}

void MPVInstance::setProperty(const std::string &name, int64_t value) const {
    const auto ret = mpv_set_property(m_handle, name.c_str(), MPV_FORMAT_INT64, &value);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_set_property");
    }
}

void MPVInstance::setProperty(const std::string &name, double value) const {
    const auto ret = mpv_set_property(m_handle, name.c_str(), MPV_FORMAT_DOUBLE, &value);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_set_property");
    }
}

void MPVInstance::setProperty(const std::string &name, bool value) const {
    const auto ret = mpv_set_property(m_handle, name.c_str(), MPV_FORMAT_FLAG, &value);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_set_property");
    }
}

void MPVInstance::setProperty(const std::string &name, mpv_node value) const {
    const auto ret = mpv_set_property(m_handle, name.c_str(), MPV_FORMAT_NODE, &value);
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_set_property");
    }
}
