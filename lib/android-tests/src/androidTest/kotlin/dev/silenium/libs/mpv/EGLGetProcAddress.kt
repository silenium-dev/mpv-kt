package dev.silenium.libs.mpv

import dev.silenium.libs.foreign.MemorySegment
import dev.silenium.mpv.native_bindings.render.GLGetProcAddress

object EGLGetProcAddress : GLGetProcAddress {
    override fun getProcAddress(name: String): MemorySegment {
        return MemorySegment.ofAddress(eglGetProcAddressN(name))
    }

    external fun eglGetProcAddressN(name: String): Long

    init {
        System.loadLibrary("example")
    }
}
