#ifndef NATIVES_NODES_HPP
#define NATIVES_NODES_HPP

#include <format>
#include <functional>
#include <jni.h>
#include <string>
#include <mpv/client.h>

class ClassConstructorRef {
public:
    explicit ClassConstructorRef(const std::string &name, const std::string &signature);

    std::string name() const;

    std::string signature() const;

    bool operator==(const ClassConstructorRef &other) const;

private:
    const std::string m_name;
    const std::string m_signature;
};

template<>
struct std::hash<ClassConstructorRef> {
    std::size_t operator()(const ClassConstructorRef &ref) const noexcept {
        const std::size_t h1 = std::hash<std::string>{}(ref.name());
        const std::size_t h2 = std::hash<std::string>{}(ref.signature());
        return h1 ^ (h2 << 1);
    }
};

class NodeClass {
public:
    NodeClass(JNIEnv *env, const ClassConstructorRef &ref);

    jobject newInstance(JNIEnv *env, ...) const;

    static mpv_format getFormat(JNIEnv *env, jobject instance);

    jclass clazz() const;

private:
    jclass m_clazz;
    jmethodID m_ctor;
};

constexpr auto NODE_BASE_CLASS = "dev/silenium/multimedia/core/mpv/Node";
constexpr auto NODE_NONE_CLASS = "dev/silenium/multimedia/core/mpv/Node$None";
static ClassConstructorRef NODE_LONG_CLASS{
    "dev/silenium/multimedia/core/mpv/Node$Long",
    "(J)V",
};
static ClassConstructorRef NODE_DOUBLE_CLASS{
    "dev/silenium/multimedia/core/mpv/Node$Double",
    "(D)V",
};
static ClassConstructorRef NODE_STRING_CLASS{
    "dev/silenium/multimedia/core/mpv/Node$String",
    "(Ljava/lang/String;)V",
};
static ClassConstructorRef NODE_FLAG_CLASS{
    "dev/silenium/multimedia/core/mpv/Node$Flag",
    "(Z)V",
};
static ClassConstructorRef NODE_BYTEARRAY_CLASS{
    "dev/silenium/multimedia/core/mpv/Node$ByteArray",
    "([B)V",
};
static ClassConstructorRef NODE_LIST_CLASS{
    "dev/silenium/multimedia/core/mpv/Node$List",
    "([Ldev/silenium/multimedia/core/mpv/Node;)V",
};
static ClassConstructorRef NODE_MAP_ENTRY_CLASS{
    "dev/silenium/multimedia/core/mpv/Node$Map$Entry",
    "(Ljava/lang/String;Ldev/silenium/multimedia/core/mpv/Node;)V",
};
static ClassConstructorRef NODE_MAP_CLASS{
    "dev/silenium/multimedia/core/mpv/Node$Map",
    std::format("([L{};)V", NODE_MAP_ENTRY_CLASS.name()),
};

jobject mapNode(JNIEnv *env, mpv_node node);
mpv_node mapNode(JNIEnv *env, jobject node);

jclass nodeClass(JNIEnv *env, const std::string &name);

std::string formatName(mpv_format fmt);

#endif //NATIVES_NODES_HPP
