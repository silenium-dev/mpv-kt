package dev.silenium.mpv.native_bindings.event

import dev.silenium.mpv.native_bindings.api.InstantiableLayout
import dev.silenium.mpv.native_bindings.api.NativeStructLayout
import dev.silenium.mpv.native_bindings.api.get
import dev.silenium.libs.foreign.MemorySegment

data class Hook(val name: String, val id: ULong) : EventData{
    constructor(struct: MemorySegment) : this(
        struct[Layout.name],
        struct[Layout.id],
    )

    companion object Layout : NativeStructLayout(), InstantiableLayout<Hook> {
        val name = string("name")
        val id = ulong("id")

        override fun from(segment: MemorySegment): Hook = Hook(segment)
    }
}
