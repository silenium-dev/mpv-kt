package dev.silenium.mpv.native_bindings.event

import dev.silenium.mpv.native_bindings.Format
import dev.silenium.mpv.native_bindings.api.NativeStructLayout
import dev.silenium.mpv.native_bindings.api.get
import java.lang.foreign.MemorySegment

data class EventProperty(
    val name: String,
    val format: Format,
    val data: MemorySegment,
) {
    constructor(struct: MemorySegment) : this(
        struct[Layout.name],
        struct[Layout.format],
        struct[Layout.data],
    )

    companion object Layout : NativeStructLayout<EventProperty>() {
        val name = string("name")
        val format = enum<Format>("format")
        private val padding_ = padding(4)
        val data = pointer("data")

        override fun from(segment: MemorySegment): EventProperty = EventProperty(segment)
    }
}
