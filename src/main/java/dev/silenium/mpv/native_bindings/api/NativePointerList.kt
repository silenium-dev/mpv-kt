package dev.silenium.mpv.native_bindings.api

import java.lang.foreign.AddressLayout
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

class NativePointerList<E>(val lengthField: NativeStructField<Int>, val mapper: (MemorySegment) -> E) :
    NativeStructField<List<E>> {
    override val layout: MemoryLayout = ValueLayout.ADDRESS

    override fun get(segment: MemorySegment): List<E> {
        val length = lengthField.get(segment)
        return List(length) { i ->
            val raw = segment.getAtIndex(AddressLayout.ADDRESS, i.toLong())
            mapper(raw)
        }
    }
}
