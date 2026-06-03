#include "renderer.hpp"
#include "util/MPVException.hpp"

#include <mpv/client.h>
#include <mpv/render.h>
#include <mpv/render_gl.h>

#ifdef TARGET_LINUX
#include <GL/glx.h>
#include <EGL/egl.h>

void close_display(void *display) {
    if (display) {
        XCloseDisplay(static_cast<Display *>(display));
    }
}
#endif

void *MPVRenderer::glGetProcAddressInvoker(void *ctx, const char *name) {
    const auto jniRef = static_cast<JniCallRef<jlong, jstring> *>(ctx);
    const auto env = jniRef->attach();
    const auto nameStr = env->get()->NewStringUTF(name);
    const auto result = (*jniRef)(env->get(), nameStr);
    if (env->get()->ExceptionCheck()) {
        return nullptr;
    }
    env->get()->DeleteLocalRef(nameStr);
    return reinterpret_cast<void *>(result);
}

void MPVRenderer::renderUpdateCallbackInvoker(void *ctx) {
    RENDERER(ctx);
    renderer->renderUpdateCallback();
}

void MPVRenderer::renderUpdateCallback() {
    m_wakeup = true;
    m_cv.notify_one();
}

void MPVRenderer::eventLoop() {
    const auto attached = m_renderUpdateCallback.attach();
    while (m_running) {
        {
            std::unique_lock lock(m_mtx);
            m_cv.wait(lock, [&] { return !m_running || m_wakeup; });
            if (!m_running) {
                return;
            }
            m_wakeup = false;
        }
        m_renderUpdateCallback(attached->get());
        if (attached->get()->ExceptionCheck()) {
            attached->get()->ExceptionDescribe();
        }
    }
}

MPVRenderer::MPVRenderer(const MPVInstance *instance, JniCallRef<jlong, jstring> &&glGetProcAddress,
                         JniCallRef<void> &&renderUpdateCallback)
    : m_instance(instance),
      m_glGetProcAddress(std::move(glGetProcAddress)),
      m_renderUpdateCallback(std::move(renderUpdateCallback)) {
    std::vector<mpv_render_param> params{
        {MPV_RENDER_PARAM_API_TYPE, const_cast<char *>(MPV_RENDER_API_TYPE_OPENGL)},
    };
#ifdef TARGET_LINUX
    if (const auto glxDisplay = glXGetCurrentDisplay(); glxDisplay != nullptr) {
        params.emplace_back(MPV_RENDER_PARAM_X11_DISPLAY, glxDisplay);
    } else if (const auto eglDisplay = eglGetCurrentDisplay(); eglDisplay != EGL_NO_DISPLAY) {
        // Compose always runs on X11
        const auto display = XOpenDisplay(nullptr);
        m_platformContext.reset(static_cast<void *>(display), close_display);
        params.emplace_back(MPV_RENDER_PARAM_X11_DISPLAY, display);
    }
#endif

    mpv_opengl_init_params gl_params{
        .get_proc_address = &MPVRenderer::glGetProcAddressInvoker,
        .get_proc_address_ctx = &m_glGetProcAddress,
    };

    params.emplace_back(MPV_RENDER_PARAM_OPENGL_INIT_PARAMS, &gl_params);
    int advControl = 1;
    params.emplace_back(MPV_RENDER_PARAM_ADVANCED_CONTROL, &advControl);
    params.emplace_back();

    const auto ret = mpv_render_context_create(&m_handle, instance->m_handle, params.data());
    if (ret < 0) {
        throw MPVException(ret, "mpv_render_context_create");
    }
    mpv_render_context_set_update_callback(m_handle, &MPVRenderer::renderUpdateCallbackInvoker, this);
}

MPVRenderer::~MPVRenderer() {
    m_running = false;
    m_cv.notify_all();
    m_eventDispatcher.join();

    mpv_render_context_set_update_callback(m_handle, nullptr, nullptr);
    mpv_render_context_free(m_handle);
}

void MPVRenderer::render(const int fbo, const int width, const int height, const int internalFormat) const {
    mpv_render_context_update(m_handle);
    mpv_opengl_fbo mpvFbo{
        .fbo = fbo,
        .w = width,
        .h = height,
        .internal_format = internalFormat,
    };
    int flipY{0};
    mpv_render_param params[]{
        {MPV_RENDER_PARAM_OPENGL_FBO, &mpvFbo},
        {MPV_RENDER_PARAM_FLIP_Y, &flipY},
        {},
    };
    const auto ret = mpv_render_context_render(m_handle, params);
    if (ret < 0) {
        throw MPVException(ret, "mpv_render_context_render");
    }
}

void MPVRenderer::start() {
    if (m_eventDispatcher.joinable()) {
        throw std::runtime_error("Renderer already started");
    }
    m_eventDispatcher = std::thread{&MPVRenderer::eventLoop, this};
}
