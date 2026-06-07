package dev.silenium.mpv.native_bindings

import dev.silenium.libs.foreign.MemorySegment

@JvmInline
value class Handle(internal val pointer: MemorySegment) {
    override fun toString(): String = "Handle(0x%016x)".format(pointer.address)
}

@JvmInline
value class RenderContext(internal val pointer: MemorySegment) {
    override fun toString(): String = "RenderContext(0x%016x)".format(pointer.address)
}
