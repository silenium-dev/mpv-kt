#ifndef NATIVES_INSTANCE_HPP
#define NATIVES_INSTANCE_HPP

#include <condition_variable>
#include <memory>
#include <shared_mutex>
#include <mpv/client.h>
#include <string>
#include <thread>
#include <vector>

#include "jni.h"

#define INSTANCE(ptr) const auto instance = reinterpret_cast<MPVInstance *>(ptr);

class MpvCallback;

class MPVInstance {
public:
    explicit MPVInstance(JavaVM *jvm);

    virtual ~MPVInstance();

    void setOption(const std::string &name, const std::string &value) const;

    void setOption(const std::string &name, int64_t value) const;

    void setOption(const std::string &name, bool value) const;

    void setOption(const std::string &name, double value) const;

    void setCallback(std::unique_ptr<MpvCallback> callback);

    void unsetCallback();

    void initialize() const;

    [[nodiscard]] std::shared_ptr<mpv_node> command(const std::vector<std::string> &argv) const;

    void command(const std::string &argv) const;

    void commandAsync(const std::vector<std::string> &argv, int64_t subscriptionId) const;

    void setProperty(const std::string &name, const std::string &value) const;

    void setProperty(const std::string &name, int64_t value) const;

    void setProperty(const std::string &name, double value) const;

    void setProperty(const std::string &name, bool value) const;

    void setProperty(const std::string &name, mpv_node value) const;

    std::string getPropertyString(const std::string &name) const;

    int64_t getPropertyLong(const std::string &name) const;

    double getPropertyDouble(const std::string &name) const;

    bool getPropertyFlag(const std::string &name) const;

    mpv_node getPropertyNode(const std::string &name) const;

    void setPropertyAsync(const std::string &name, const std::string &value, int64_t subscriptionId) const;

    void setPropertyAsync(const std::string &name, int64_t value, int64_t subscriptionId) const;

    void setPropertyAsync(const std::string &name, double value, int64_t subscriptionId) const;

    void setPropertyAsync(const std::string &name, bool value, int64_t subscriptionId) const;

    void setPropertyAsync(const std::string &name, mpv_node value, int64_t subscriptionId) const;

    void getPropertyStringAsync(const std::string &name, int64_t subscriptionId) const;

    void getPropertyLongAsync(const std::string &name, int64_t subscriptionId) const;

    void getPropertyDoubleAsync(const std::string &name, int64_t subscriptionId) const;

    void getPropertyFlagAsync(const std::string &name, int64_t subscriptionId) const;

    void getPropertyNodeAsync(const std::string &name, int64_t subscriptionId) const;

    void observePropertyString(const std::string &name, int64_t subscriptionId) const;

    void observePropertyLong(const std::string &name, int64_t subscriptionId) const;

    void observePropertyDouble(const std::string &name, int64_t subscriptionId) const;

    void observePropertyFlag(const std::string &name, int64_t subscriptionId) const;

    void observePropertyNode(const std::string &name, int64_t subscriptionId) const;

    void unobserveProperty(int64_t subscriptionId) const;

private:
    static void wakeupCallback(void *);

    void onWakeup();

    void eventLoop();

    void dispatchEvents(JNIEnv *env);

    mpv_handle *m_handle;
    std::shared_mutex m_callbackMutex;
    std::unique_ptr<MpvCallback> m_callback;

    JavaVM *m_jvm;
    std::thread m_eventDispatcher;
    volatile bool m_running{true};
    std::condition_variable m_cv;
    std::mutex m_mtx;
    bool m_wakeup{false};

    friend class MPVRenderer;
};

#endif //NATIVES_INSTANCE_HPP
