package dev.silenium.mpv.native_bindings.api

import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemoryLayout.PathElement.groupElement
import java.lang.foreign.MemorySegment
import java.lang.invoke.VarHandle

interface NativeStructField<T> {
    val varHandle: VarHandle?
    val layout: MemoryLayout
    fun get(segment: MemorySegment): T
}

fun <T> NativeStructField<T>.nullable(): NativeStructField<T?> = NullableNativeStructField(this)

@PublishedApi
internal class MappedNativeStructField<T, M>(
    private val struct: Lazy<MemoryLayout>,
    val name: String,
    override val layout: MemoryLayout,
    private val mapper: (T) -> M,
) : NativeStructField<M> {
    override val varHandle: VarHandle by lazy {
        struct.value.varHandle(groupElement(name))
    }

    override fun get(segment: MemorySegment) = mapper(varHandle.get(segment, 0L) as T)
}

@PublishedApi
internal class EmbeddedStructField<T>(
    private val parentLayout: Lazy<MemoryLayout>,
    val name: String,
    override val layout: MemoryLayout,
    private val mapper: (MemorySegment) -> T,
): NativeStructField<T> {
    override val varHandle: VarHandle? = null
    private val byteOffset by lazy {
        parentLayout.value.byteOffset(groupElement(name))
    }

    override fun get(segment: MemorySegment): T {
        val slice = segment.asSlice(byteOffset, layout.byteSize())
        return mapper(slice)
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
}
