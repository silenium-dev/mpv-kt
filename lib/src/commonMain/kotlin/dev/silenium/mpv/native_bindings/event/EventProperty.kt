package dev.silenium.mpv.native_bindings.event

import dev.silenium.mpv.native_bindings.api.InstantiableLayout
import dev.silenium.mpv.native_bindings.api.NativeStructLayout
import dev.silenium.mpv.native_bindings.api.get
import dev.silenium.mpv.native_bindings.api.parse
import dev.silenium.mpv.native_bindings.node.Format
import dev.silenium.mpv.native_bindings.node.Node
import dev.silenium.libs.foreign.MemorySegment
import dev.silenium.libs.foreign.ValueLayout

data class EventProperty(
    val name: String,
    val data: Node,
) : EventData {
    constructor(struct: MemorySegment) : this(
        struct[Layout.name],
        when (struct[Layout.format]) {
            Format.None -> Node.None
            Format.String -> Node.String(struct[Layout.data].reinterpret(Long.MAX_VALUE).getString(0))
            Format.OsdString -> Node.OsdString(struct[Layout.data].reinterpret(Long.MAX_VALUE).getString(0))
            Format.Flag -> Node.Flag(struct[Layout.data].get(ValueLayout.JAVA_BOOLEAN, 0))
            Format.Int64 -> Node.Int64(struct[Layout.data].get(ValueLayout.JAVA_LONG, 0))
            Format.Double -> Node.Double(struct[Layout.data].get(ValueLayout.JAVA_DOUBLE, 0))
            Format.Node -> Node.parse(struct[Layout.data])
            Format.NodeArray -> Node.List.parse(struct[Layout.data])
            Format.NodeMap -> Node.Map.parse(struct[Layout.data])
            Format.ByteArray -> Node.ByteArray.parse(struct[Layout.data])
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
