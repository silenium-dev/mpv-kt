package dev.silenium.mpv.native_bindings.node

import dev.silenium.mpv.native_bindings.api.NativeEnum

enum class Format(override val value: Int): NativeEnum<Format> {
    None(0),
    String(1),
    OsdString(2),
    Flag(3),
    Int64(4),
    Double(5),
    Node(6),
    NodeArray(7),
    NodeMap(8),
    ByteArray(9),
}
