#include "instance.hpp"

#include <format>

#include "util/MPVException.hpp"

std::shared_ptr<mpv_node> MPVInstance::command(const std::vector<std::string> &argv) const {
    std::vector<const char *> argv_c;
    for (const auto &arg: argv) {
        argv_c.push_back(arg.c_str());
    }
    argv_c.push_back(nullptr);
    auto result = std::make_shared<mpv_node>();
    const auto ret = mpv_command_ret(m_handle, argv_c.data(), result.get());
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_command_ret");
    }
    return result;
}

void MPVInstance::command(const std::string &argv) const {
    const auto ret = mpv_command_string(m_handle, argv.data());
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_command_ret");
    }
}

void MPVInstance::commandAsync(const std::vector<std::string> &argv, const int64_t subscriptionId) const {
    std::vector<const char *> argv_c;
    for (const auto &arg: argv) {
        argv_c.push_back(arg.c_str());
    }
    argv_c.push_back(nullptr);
    const auto ret = mpv_command_async(m_handle, subscriptionId, argv_c.data());
    if (ret < MPV_ERROR_SUCCESS) {
        throw MPVException(ret, "mpv_command_async");
    }
}
