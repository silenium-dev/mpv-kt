package dev.silenium.mpv.native_bindings.api

import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

abstract class NativeStructLayout<T> {
    @PublishedApi
    internal val entries: MutableList<Pair<String, MemoryLayout>> = mutableListOf()

    internal open fun layoutCreator(entries: List<MemoryLayout>): MemoryLayout =
        MemoryLayout.structLayout(*entries.toTypedArray())

    private val layoutLazy = lazy {
        val fields = entries.map { (name, layout) ->
            layout.withName(name)
        }
        layoutCreator(fields)
    }
    val isRealized: Boolean get() = layoutLazy.isInitialized()

    @PublishedApi
    internal val layout: MemoryLayout by layoutLazy

    @PublishedApi
    internal inline fun <reified T, reified M> register(
        name: String,
        fieldLayout: MemoryLayout,
        crossinline mapper: (T) -> M
    ): NativeStructField<M> {
        if (isRealized) error("Cannot register fields after struct layout has been realized")
        entries.add(name to fieldLayout)
        return object : NativeStructField<M> {
            override val layout: MemoryLayout = fieldLayout

            override fun get(segment: MemorySegment): M {
                val raw = this@NativeStructLayout.layout
                    .varHandle(MemoryLayout.PathElement.groupElement(name))
                    .get(segment, 0L) as T
                return mapper(raw)
            }
        }
    }

    @PublishedApi
    internal fun <T> register(name: String, field: NativeStructField<T>) {
        if (isRealized) error("Cannot register fields after struct layout has been realized")
        entries.add(name to field.layout)
    }

    protected inline fun <reified E> enum(name: String): NativeStructField<E> where E : Enum<E>, E : NativeEnum<E> {
        return register<Int, E>(name, ValueLayout.JAVA_INT) { raw ->
            enumValues<E>().first { it.value == raw }
        }
    }

    protected fun string(name: String): NativeStructField<String> {
        return register<MemorySegment, String>(name, ValueLayout.ADDRESS) { raw ->
            raw.getString(0)
        }
    }

    protected fun int(name: String): NativeStructField<Int> =
        register<Int, Int>(name, ValueLayout.JAVA_INT) { it }

    protected fun uint(name: String): NativeStructField<UInt> =
        register<Int, UInt>(name, ValueLayout.JAVA_INT, Int::toUInt)

    protected fun long(name: String): NativeStructField<Long> =
        register<Long, Long>(name, ValueLayout.JAVA_LONG) { it }

    protected fun ulong(name: String): NativeStructField<ULong> =
        register<Long, ULong>(name, ValueLayout.JAVA_LONG, Long::toULong)

    protected fun pointer(name: String): NativeStructField<MemorySegment> =
        register<MemorySegment, MemorySegment>(name, ValueLayout.ADDRESS) { it }

    protected fun pointerArray(
        name: String,
        lengthField: NativeStructField<Int>,
    ): NativeStructField<List<MemorySegment>> =
        NativePointerList(lengthField) { it }
            .also { register(name, it) }

    protected fun stringArray(name: String, lengthField: NativeStructField<Int>): NativeStructField<List<String>> =
        NativePointerList(lengthField) { it.getString(0L) }
            .also { register(name, it) }

    protected inline fun <reified T> struct(name: String, layout: InstantiableLayout<T>): NativeStructField<T> =
        register<MemorySegment, T>(name, layout.layout) { segment ->
            layout.from(segment)
        }

    private var paddingCounter = 0L
    protected fun padding(size: Long): NativeStructField<Unit> =
        register<Any?, Unit>("padding-${paddingCounter++}", MemoryLayout.paddingLayout(size)) { }
}

operator fun <R> MemorySegment.get(field: NativeStructField<R>) = field.get(this)
