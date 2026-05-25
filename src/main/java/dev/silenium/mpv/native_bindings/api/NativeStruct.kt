package dev.silenium.mpv.native_bindings.api

import java.lang.foreign.Arena
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
        noinline mapper: (T) -> M,
        noinline reverseMapper: (M, Arena) -> T,
    ): NativeStructField<M> {
        if (isRealized) error("Cannot register fields after struct layout has been realized")
        entries.add(name to fieldLayout)
        return MappedNativeStructField(layoutLazy, name, fieldLayout, T::class.java, mapper, reverseMapper)
    }

    @PublishedApi
    internal fun <T> register(name: String, field: NativeStructField<T>) {
        if (isRealized) error("Cannot register fields after struct layout has been realized")
        entries.add(name to field.layout)
    }

    protected inline fun <reified E> enum(name: String): NativeStructField<E> where E : Enum<E>, E : NativeEnum<E> {
        val enumMap = enumValues<E>().associateBy { it.value }
        return register<Int, E>(
            name,
            ValueLayout.JAVA_INT,
            mapper = { raw ->
                enumMap[raw] ?: error("Invalid enum value: $raw")
            },
            reverseMapper = { value, _ ->
                value.value
            },
        )
    }

    protected fun string(name: String): NativeStructField<String> =
        register<MemorySegment, String>(
            name,
            ValueLayout.ADDRESS,
            mapper = { raw ->
                raw.reinterpret(Long.MAX_VALUE).getString(0)
            },
            reverseMapper = { value, arena ->
                arena.allocateFrom(value)
            },
        )

    protected fun bool(name: String): NativeStructField<Boolean> =
        register<Int, Boolean>(
            name,
            ValueLayout.JAVA_INT,
            mapper = { it != 0 },
            reverseMapper = { value, _ -> 1.takeIf { value } ?: 0 },
        )

    protected fun double(name: String): NativeStructField<Double> =
        register<Double, Double>(
            name,
            ValueLayout.JAVA_DOUBLE,
            mapper = { it },
            reverseMapper = { it, _ -> it },
        )

    protected fun int(name: String): NativeStructField<Int> =
        register<Int, Int>(
            name,
            ValueLayout.JAVA_INT,
            mapper = { it },
            reverseMapper = { it, _ -> it },
        )

    protected fun uint(name: String): NativeStructField<UInt> =
        register<Int, UInt>(
            name,
            ValueLayout.JAVA_INT,
            Int::toUInt,
            reverseMapper = { it, _ -> it.toInt() },
        )

    protected fun long(name: String): NativeStructField<Long> =
        register<Long, Long>(
            name,
            ValueLayout.JAVA_LONG,
            mapper = { it },
            reverseMapper = { it, _ -> it },
        )

    protected fun ulong(name: String): NativeStructField<ULong> =
        register<Long, ULong>(
            name,
            ValueLayout.JAVA_LONG,
            Long::toULong,
            reverseMapper = { it, _ -> it.toLong() },
        )

    protected fun pointer(name: String): NativeStructField<MemorySegment> =
        register<MemorySegment, MemorySegment>(
            name,
            ValueLayout.ADDRESS,
            mapper = { it },
            reverseMapper = { it, _ -> it },
        )

    protected fun pointerArray(
        name: String,
        lengthField: NativeStructField<Int>,
    ): NativeStructField<List<MemorySegment>> =
        NativePointerList(
            layoutLazy,
            name,
            lengthField,
            mapper = { it },
            reverseMapper = { it, _ -> it },
        ).also { register(name, it) }

    protected fun array(
        name: String,
        lengthField: NativeStructField<Int>,
        inner: MemoryLayout,
    ): NativeStructField<List<MemorySegment>> =
        NativeList(
            layoutLazy,
            inner,
            name,
            lengthField,
            mapper = { it },
            reverseMapper = { it, _ -> it },
        ).also { register(name, it) }

    protected fun stringArray(name: String, lengthField: NativeStructField<Int>): NativeStructField<List<String>> =
        NativePointerList(
            layoutLazy,
            name,
            lengthField,
            mapper = { it.reinterpret(Long.MAX_VALUE).getString(0L) },
            reverseMapper = { it, arena -> arena.allocateFrom(it) }
        ).also { register(name, it) }

    protected inline fun <reified T : InstantiatedStruct> struct(
        name: String,
        layout: InstantiableLayout<T>
    ): NativeStructField<T> =
        EmbeddedStructField(
            layoutLazy,
            name,
            layout.layout,
            mapper = { layout.from(it) },
            reverseMapper = { it, arena -> it.into(arena) },
        ).also { register(name, it) }

    protected fun union(name: String, layout: NativeUnionLayout): NativeStructField<MemorySegment> =
        EmbeddedStructField(
            layoutLazy,
            name,
            layout.layout,
            mapper = { it },
            reverseMapper = { it, _ -> it },
        ).also { register(name, it) }

    private var paddingCounter = 0L
    protected fun padding(size: Long): NativeStructField<Unit> =
        register<Any?, Unit>(
            "padding-${paddingCounter++}",
            MemoryLayout.paddingLayout(size),
            mapper = { },
            reverseMapper = { _, _ -> },
        )
}

operator fun <R> MemorySegment.get(field: NativeStructField<R>) = field.get(this)
operator fun <R> MemorySegment.set(field: NativeStructField<R>, arena: Arena, value: R) = field.set(this, value, arena)
