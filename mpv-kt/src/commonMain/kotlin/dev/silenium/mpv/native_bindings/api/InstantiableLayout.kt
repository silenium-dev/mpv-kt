package dev.silenium.mpv.native_bindings.api

import dev.silenium.libs.foreign.Arena
import dev.silenium.libs.foreign.MemoryLayout
import dev.silenium.libs.foreign.MemorySegment

interface InstantiableLayout<T> {
    val layout: MemoryLayout
    fun from(segment: MemorySegment): T
}

interface InstantiatedStruct {
    val layout: MemoryLayout
    fun into(arena: Arena): MemorySegment
}

fun <T> InstantiableLayout<T>.parse(segment: MemorySegment) = from(segment.asReadOnly().reinterpret(layout.byteSize))
fun <T: InstantiatedStruct> T.into(arena: Arena): MemorySegment = into(arena)
