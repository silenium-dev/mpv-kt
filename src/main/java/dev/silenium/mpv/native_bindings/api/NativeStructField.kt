package dev.silenium.mpv.native_bindings.api

import java.lang.foreign.Arena
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemoryLayout.PathElement.groupElement
import java.lang.foreign.MemorySegment
import java.lang.invoke.VarHandle

interface NativeStructField<T> {
    val varHandle: VarHandle?
    val layout: MemoryLayout
    fun get(segment: MemorySegment): T
    fun set(segment: MemorySegment, value: T, arena: Arena)
}

fun <T> NativeStructField<T>.nullable(): NativeStructField<T?> = NullableNativeStructField(this)

@PublishedApi
internal class MappedNativeStructField<T, M>(
    private val struct: Lazy<MemoryLayout>,
    val name: String,
    override val layout: MemoryLayout,
    private val rawType: Class<T>,
    private val mapper: (T) -> M,
    private val reverseMapper: (M, Arena) -> T,
) : NativeStructField<M> {
    override val varHandle: VarHandle by lazy {
        struct.value.varHandle(groupElement(name))
    }

    override fun get(segment: MemorySegment) = mapper(rawType.cast(varHandle.get(segment, 0L)))
    override fun set(segment: MemorySegment, value: M, arena: Arena) =
        varHandle.set(segment, 0L, reverseMapper(value, arena))
}

@PublishedApi
internal class EmbeddedStructField<T>(
    private val parentLayout: Lazy<MemoryLayout>,
    val name: String,
    override val layout: MemoryLayout,
    private val mapper: (MemorySegment) -> T,
    private val reverseMapper: (T, Arena) -> MemorySegment,
) : NativeStructField<T> {
    override val varHandle: VarHandle? = null
    private val byteOffset by lazy {
        parentLayout.value.byteOffset(groupElement(name))
    }

    override fun get(segment: MemorySegment): T {
        val slice = segment.asSlice(byteOffset, layout.byteSize())
        return mapper(slice)
    }

    override fun set(segment: MemorySegment, value: T, arena: Arena) {
        val slice = segment.asSlice(byteOffset, layout.byteSize())
        slice.copyFrom(reverseMapper(value, arena))
    }
}

@PublishedApi
internal class NullableNativeStructField<T>(
    private val wrapped: NativeStructField<T>,
) : NativeStructField<T?> {
    override val layout by wrapped::layout
    override val varHandle by wrapped::varHandle

    override fun get(segment: MemorySegment): T? {
        wrapped.varHandle?.let {
            val raw = it.get(segment, 0L)
            if (raw == null || raw == MemorySegment.NULL) return null
        }
        return wrapped.get(segment)
    }

    override fun set(segment: MemorySegment, value: T?, arena: Arena) {
        wrapped.varHandle?.let {
            if (value == null) {
                it.set(segment, 0L, MemorySegment.NULL)
            } else {
                wrapped.set(segment, value, arena)
            }
        }
    }
}
