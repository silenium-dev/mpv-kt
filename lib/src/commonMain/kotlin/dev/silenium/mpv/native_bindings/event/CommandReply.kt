package dev.silenium.mpv.native_bindings.event

import dev.silenium.mpv.native_bindings.api.InstantiableLayout
import dev.silenium.mpv.native_bindings.api.NativeStructLayout
import dev.silenium.mpv.native_bindings.api.get
import dev.silenium.mpv.native_bindings.node.Node
import dev.silenium.libs.foreign.MemorySegment

data class CommandReply(val node: Node): EventData {
    constructor(struct: MemorySegment) : this(
        struct[Layout.node],
    )

    companion object Layout : NativeStructLayout(), InstantiableLayout<CommandReply> {
        val node = struct("node", Node)

        override fun from(segment: MemorySegment) = CommandReply(segment)
    }
}
