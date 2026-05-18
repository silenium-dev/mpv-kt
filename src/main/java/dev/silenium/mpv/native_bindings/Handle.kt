package dev.silenium.mpv.native_bindings

import java.lang.foreign.MemorySegment

@JvmInline
value class Handle(internal val pointer: MemorySegment) {
    override fun toString(): String = "MpvHandle(0x%016x)".format(pointer.address())
}
