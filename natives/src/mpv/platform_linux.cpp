#include "platform.hpp"

#include <GL/glx.h>
#include <EGL/egl.h>

class LinuxMpvPlatformContext : public MpvPlatformContext {
public:
    explicit LinuxMpvPlatformContext(Display *x_display) : display(x_display) {
    }

    ~LinuxMpvPlatformContext() override {
        if (display != nullptr) {
            XCloseDisplay(display);
        }
    }

private:
    Display *display;
};

std::shared_ptr<MpvPlatformContext> populatePlatformMpvParams(JNIEnv *env, std::vector<mpv_render_param> &params) {
    Display *display{nullptr};
    if (const auto glxDisplay = glXGetCurrentDisplay(); glxDisplay != nullptr) {
        params.push_back({MPV_RENDER_PARAM_X11_DISPLAY, glxDisplay});
    } else if (const auto eglDisplay = eglGetCurrentDisplay(); eglDisplay != EGL_NO_DISPLAY) {
        // Compose always runs on X11
        display = XOpenDisplay(nullptr);
        params.push_back({MPV_RENDER_PARAM_X11_DISPLAY, display});
    }
    return std::make_shared<LinuxMpvPlatformContext>(display);
}
