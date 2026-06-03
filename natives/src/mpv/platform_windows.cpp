#include "platform.hpp"

class WindowsMpvPlatformContext : public MpvPlatformContext {
public:
    WindowsMpvPlatformContext() = default;

    ~WindowsMpvPlatformContext() override = default;
};

std::shared_ptr<MpvPlatformContext> populatePlatformMpvParams(JNIEnv *env, std::vector<mpv_render_param> &params) {
    return std::make_shared<WindowsMpvPlatformContext>();
}
