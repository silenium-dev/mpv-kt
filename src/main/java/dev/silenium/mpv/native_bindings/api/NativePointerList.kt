package dev.silenium.mpv.native_bindings.api

import java.lang.foreign.AddressLayout
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemoryLayout.PathElement.groupElement
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.invoke.VarHandle

class NativePointerList<E>(
    private val struct: Lazy<MemoryLayout>,
    val name: String,
    val lengthField: NativeStructField<Int>,
    val mapper: (MemorySegment) -> E
) :
    NativeStructField<List<E>> {
    override val layout: MemoryLayout = ValueLayout.ADDRESS
    override val varHandle: VarHandle by lazy {
        struct.value.varHandle(groupElement(name))
    }

    override fun get(segment: MemorySegment): List<E> {
        val length = lengthField.get(segment)
        val array = (varHandle.get(segment, 0L) as MemorySegment).reinterpret(AddressLayout.ADDRESS.byteSize() * length)
        return List(length) { i ->
            val raw = array.getAtIndex(AddressLayout.ADDRESS, i.toLong())
            mapper(raw)
        }
    }
}

class NativeList<E>(
    private val struct: Lazy<MemoryLayout>,
    val inner: MemoryLayout,
    val name: String,
    val lengthField: NativeStructField<Int>,
    val mapper: (MemorySegment) -> E
) :
    NativeStructField<List<E>> {
    override val layout: MemoryLayout = ValueLayout.ADDRESS
    override val varHandle: VarHandle by lazy {
        struct.value.varHandle(groupElement(name))
    }

    override fun get(segment: MemorySegment): List<E> {
        val length = lengthField.get(segment)
        val array = (varHandle.get(segment, 0L) as MemorySegment).reinterpret(inner.byteSize() * length)
        return List(length) { i ->
            val raw = array.asSlice(i * inner.byteSize(), inner.byteSize())
            mapper(raw)
        }
    }
}
