package dev.silenium.mpv.native_bindings.api

import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

abstract class NativeStructLayout {
    @PublishedApi
    internal val entries: MutableList<Pair<String, MemoryLayout>> = mutableListOf()

    internal open fun layoutCreator(entries: List<MemoryLayout>): MemoryLayout =
        MemoryLayout.structLayout(*entries.toTypedArray())

    @PublishedApi
    internal val layoutLazy = lazy {
        val fields = entries.map { (name, layout) ->
            layout.withName(name)
        }
        layoutCreator(fields)
    }
    val isRealized: Boolean get() = layoutLazy.isInitialized()

    val layout: MemoryLayout by layoutLazy

    @PublishedApi
    internal inline fun <reified T, reified M> register(
        name: String,
        fieldLayout: MemoryLayout,
        noinline mapper: (T) -> M
    ): NativeStructField<M> {
        if (isRealized) error("Cannot register fields after struct layout has been realized")
        entries.add(name to fieldLayout)
        return MappedNativeStructField(layoutLazy, name, fieldLayout, mapper)
    }

    @PublishedApi
    internal fun <T> register(name: String, field: NativeStructField<T>) {
        if (isRealized) error("Cannot register fields after struct layout has been realized")
        entries.add(name to field.layout)
    }

    protected inline fun <reified E> enum(name: String): NativeStructField<E> where E : Enum<E>, E : NativeEnum<E> =
        register<Int, E>(name, ValueLayout.JAVA_INT_UNALIGNED) { raw ->
            enumValues<E>().first { it.value == raw }
        }

    protected fun string(name: String): NativeStructField<String> =
        register<MemorySegment, String>(name, ValueLayout.ADDRESS_UNALIGNED) { raw ->
            raw.reinterpret(Long.MAX_VALUE).getString(0)
        }

    protected fun bool(name: String): NativeStructField<Boolean> =
        register<Int, Boolean>(name, ValueLayout.JAVA_INT_UNALIGNED) { it != 0 }

    protected fun double(name: String): NativeStructField<Double> =
        register<Double, Double>(name, ValueLayout.JAVA_DOUBLE_UNALIGNED) { it }

    protected fun int(name: String): NativeStructField<Int> =
        register<Int, Int>(name, ValueLayout.JAVA_INT_UNALIGNED) { it }

    protected fun uint(name: String): NativeStructField<UInt> =
        register<Int, UInt>(name, ValueLayout.JAVA_INT_UNALIGNED, Int::toUInt)

    protected fun long(name: String): NativeStructField<Long> =
        register<Long, Long>(name, ValueLayout.JAVA_LONG_UNALIGNED) { it }

    protected fun ulong(name: String): NativeStructField<ULong> =
        register<Long, ULong>(name, ValueLayout.JAVA_LONG_UNALIGNED, Long::toULong)

    protected fun pointer(name: String): NativeStructField<MemorySegment> =
        register<MemorySegment, MemorySegment>(name, ValueLayout.ADDRESS_UNALIGNED) { it }

    protected fun pointerArray(
        name: String,
        lengthField: NativeStructField<Int>,
    ): NativeStructField<List<MemorySegment>> =
        NativePointerList(layoutLazy, name, lengthField) { it }
            .also { register(name, it) }

    protected fun array(
        name: String,
        lengthField: NativeStructField<Int>,
        inner: MemoryLayout,
    ): NativeStructField<List<MemorySegment>> =
        NativeList(layoutLazy, inner, name, lengthField) { it }
            .also { register(name, it) }

    protected fun stringArray(name: String, lengthField: NativeStructField<Int>): NativeStructField<List<String>> =
        NativePointerList(layoutLazy, name, lengthField) { it.reinterpret(Long.MAX_VALUE).getString(0L) }
            .also { register(name, it) }

    protected inline fun <reified T> struct(name: String, layout: InstantiableLayout<T>): NativeStructField<T> =
        EmbeddedStructField(layoutLazy, name, layout.layout) { layout.from(it) }
            .also { register(name, it) }

    protected fun union(name: String, layout: NativeUnionLayout): NativeStructField<MemorySegment> =
        EmbeddedStructField(layoutLazy, name, layout.layout) { it }
            .also { register(name, it) }

    private var paddingCounter = 0L
    protected fun padding(size: Long): NativeStructField<Unit> =
        register<Any?, Unit>("padding-${paddingCounter++}", MemoryLayout.paddingLayout(size)) { }
}

operator fun <R> MemorySegment.get(field: NativeStructField<R>) = field.get(this)
