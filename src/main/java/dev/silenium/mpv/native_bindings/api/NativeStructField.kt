package dev.silenium.mpv.native_bindings.api

import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment

interface NativeStructField<T> {
    val layout: MemoryLayout
    fun get(segment: MemorySegment): T
}
