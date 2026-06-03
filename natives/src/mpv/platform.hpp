#ifndef NATIVE_MPV_PLATFORM_HPP
#define NATIVE_MPV_PLATFORM_HPP

#include <jni.h>
#include <vector>
#include <memory>
#include <mpv/render.h>

class MpvPlatformContext {
public:
    virtual ~MpvPlatformContext() = default;
};

std::shared_ptr<MpvPlatformContext> populatePlatformMpvParams(JNIEnv *env, std::vector<mpv_render_param> &params);

#endif //NATIVE_MPV_PLATFORM_HPP
