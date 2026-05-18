package dev.silenium.mpv.native_bindings.node

import dev.silenium.mpv.native_bindings.api.NativeStructLayout
import dev.silenium.mpv.native_bindings.api.NativeUnionLayout
import java.lang.foreign.MemorySegment

data class Node() {
    constructor(struct: MemorySegment) : this()

    private object UnionLayout: NativeUnionLayout() {
    }

    companion object : NativeStructLayout<Node>() {
        val union = struct()

        override fun from(segment: MemorySegment) = Node(segment)
    }
}
