package dev.silenium.mpv.native_bindings.event

import dev.silenium.mpv.native_bindings.api.NativeStructLayout
import dev.silenium.mpv.native_bindings.api.get
import dev.silenium.mpv.native_bindings.node.Node
import java.lang.foreign.MemorySegment

data class CommandReply(val node: Node) {
    constructor(struct: MemorySegment) : this(
        struct[Layout.node],
    )

    companion object Layout : NativeStructLayout<CommandReply>() {
        val node = struct("node", Node)

        override fun from(segment: MemorySegment) = CommandReply(segment)
    }
}
