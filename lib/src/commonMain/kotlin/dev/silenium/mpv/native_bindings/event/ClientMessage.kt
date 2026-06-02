package dev.silenium.mpv.native_bindings.event

import dev.silenium.mpv.native_bindings.api.InstantiableLayout
import dev.silenium.mpv.native_bindings.api.NativeStructLayout
import dev.silenium.mpv.native_bindings.api.get
import java.lang.foreign.MemorySegment

data class ClientMessage(val args: List<String>): EventData {
    constructor(struct: MemorySegment) : this(
        struct[Layout.args]
    )

    companion object Layout : NativeStructLayout(), InstantiableLayout<ClientMessage> {
        val num_args = int("num_args")
        val args = stringArray("args", num_args)

        override fun from(segment: MemorySegment) = ClientMessage(segment)
    }
}
