package dev.silenium.mpv.native_bindings.api

import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment

interface InstantiableLayout<T> {
    val layout: MemoryLayout
    fun from(segment: MemorySegment): T
}
