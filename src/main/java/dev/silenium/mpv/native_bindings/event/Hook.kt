package dev.silenium.mpv.native_bindings.event

import dev.silenium.mpv.native_bindings.api.NativeStructLayout
import dev.silenium.mpv.native_bindings.api.get
import java.lang.foreign.MemorySegment

data class Hook(val name: String, val id: ULong) {
    constructor(struct: MemorySegment) : this(
        struct[Layout.name],
        struct[Layout.id],
    )

    companion object Layout : NativeStructLayout<Hook>() {
        val name = string("name")
        val id = ulong("id")

        override fun from(segment: MemorySegment): Hook = Hook(segment)
    }
}
