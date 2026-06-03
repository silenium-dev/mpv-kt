#ifndef NATIVES_RENDERER_HPP
#define NATIVES_RENDERER_HPP

#include "instance.hpp"
#include "helper/JniCallRef.hpp"

struct mpv_render_context;

#define RENDERER(ptr) const auto renderer = reinterpret_cast<MPVRenderer *>(ptr);

class MPVRenderer {
public:
    explicit MPVRenderer(const MPVInstance *instance, JniCallRef<jlong, jstring> &&glGetProcAddress,
                         JniCallRef<void> &&renderUpdateCallback);

    ~MPVRenderer();

    void render(int fbo, int width, int height, int internalFormat) const;

    void start();

private:
    static void *glGetProcAddressInvoker(void *ctx, const char *name);

    static void renderUpdateCallbackInvoker(void *ctx);

    void renderUpdateCallback();

    void eventLoop();

    const MPVInstance *m_instance;
    mpv_render_context *m_handle{nullptr};
    std::shared_ptr<void> m_platformContext;
    JniCallRef<jlong, jstring> m_glGetProcAddress;
    JniCallRef<void> m_renderUpdateCallback;

    std::thread m_eventDispatcher;
    std::mutex m_mtx;
    std::condition_variable m_cv;
    bool m_wakeup{false};
    bool m_running{true};
};

#endif //NATIVES_RENDERER_HPP
