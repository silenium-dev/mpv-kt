package dev.silenium.mpv.native_bindings.event

import dev.silenium.libs.foreign.MemorySegment
import dev.silenium.libs.foreign.ValueLayout
import dev.silenium.mpv.native_bindings.api.InstantiableLayout
import dev.silenium.mpv.native_bindings.api.NativeStructLayout
import dev.silenium.mpv.native_bindings.api.get
import dev.silenium.mpv.native_bindings.api.parse
import dev.silenium.mpv.native_bindings.node.Format
import dev.silenium.mpv.native_bindings.node.Node

data class EventProperty(
    val name: String,
    val data: Node,
) : EventData {
    constructor(struct: MemorySegment) : this(
        struct[name],
        when (struct[format]) {
            Format.None -> Node.None
            Format.String -> Node.String(struct[data].reinterpret(Long.MAX_VALUE).getString(0))
            Format.OsdString -> Node.OsdString(struct[data].reinterpret(Long.MAX_VALUE).getString(0))
            Format.Flag -> Node.Flag(struct[data].get(ValueLayout.JAVA_BOOLEAN, 0))
            Format.Int64 -> Node.Int64(struct[data].get(ValueLayout.JAVA_LONG, 0))
            Format.Double -> Node.Double(struct[data].get(ValueLayout.JAVA_DOUBLE, 0))
            Format.Node -> Node.parse(struct[data])
            Format.NodeArray -> Node.List.parse(struct[data])
            Format.NodeMap -> Node.Map.parse(struct[data])
            Format.ByteArray -> Node.ByteArray.parse(struct[data])
        },
    )

    companion object Layout : NativeStructLayout(), InstantiableLayout<EventProperty> {
        val name = string("name")
        val format = enum<Format>("format")
        private val padding_ = intPadding()
        val data = pointer("data")

        override fun from(segment: MemorySegment): EventProperty = EventProperty(segment)
    }
}
