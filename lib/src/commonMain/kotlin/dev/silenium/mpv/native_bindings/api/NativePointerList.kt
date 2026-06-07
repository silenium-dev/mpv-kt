package dev.silenium.mpv.native_bindings.api

import dev.silenium.libs.foreign.Arena
import dev.silenium.libs.foreign.MemoryLayout
import dev.silenium.libs.foreign.MemoryLayout.PathElement.Companion.groupElement
import dev.silenium.libs.foreign.MemorySegment
import dev.silenium.libs.foreign.ValueLayout
import dev.silenium.libs.foreign.VarHandle

class NativePointerList<E>(
    private val struct: Lazy<MemoryLayout>,
    val name: String,
    val lengthField: NativeStructField<Int>,
    val mapper: (MemorySegment) -> E,
    val reverseMapper: (E, Arena) -> MemorySegment,
) : NativeStructField<List<E>> {
    override val layout: MemoryLayout = ValueLayout.ADDRESS
    override val varHandle: VarHandle by lazy {
        struct.value.varHandle(listOf(groupElement(name)))
    }

    override fun get(segment: MemorySegment): List<E> {
        val length = lengthField.get(segment)
        val array = (varHandle.get(segment, 0L) as MemorySegment).reinterpret(ValueLayout.ADDRESS.byteSize * length)
        return List(length) { i ->
            val raw = array.getAtIndex(ValueLayout.ADDRESS, i.toLong())
            mapper(raw)
        }
    }

    override fun set(segment: MemorySegment, value: List<E>, arena: Arena) {
        val raw = arena.allocate(ValueLayout.ADDRESS, value.size.toLong())
        value.forEachIndexed { index, e ->
            raw.setAtIndex(ValueLayout.ADDRESS, index.toLong(), reverseMapper(e, arena))
        }
        varHandle.set(segment, 0L, raw)
        lengthField.set(segment, value.size, arena)
    }
}

class NativeList<E>(
    private val struct: Lazy<MemoryLayout>,
    val inner: MemoryLayout,
    val name: String,
    val lengthField: NativeStructField<Int>,
    val mapper: (MemorySegment) -> E,
    val reverseMapper: (E, Arena) -> MemorySegment,
) : NativeStructField<List<E>> {
    override val layout: MemoryLayout = ValueLayout.ADDRESS
    override val varHandle: VarHandle by lazy {
        struct.value.varHandle(listOf(groupElement(name)))
    }

    override fun get(segment: MemorySegment): List<E> {
        val length = lengthField.get(segment)
        val array = (varHandle.get(segment, 0L) as MemorySegment).reinterpret(inner.byteSize * length)
        return List(length) { i ->
            val raw = array.asSlice(i * inner.byteSize, inner.byteSize)
            mapper(raw)
        }
    }

    override fun set(segment: MemorySegment, value: List<E>, arena: Arena) {
        val raw = arena.allocate(inner, value.size.toLong())
        value.forEachIndexed { index, e ->
            raw.asSlice(index * inner.byteSize, inner.byteSize).copyFrom(reverseMapper(e, arena))
        }
        varHandle.set(segment, 0L, raw)
        lengthField.set(segment, value.size, arena)
    }
}
