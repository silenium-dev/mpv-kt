package dev.silenium.libs.foreign

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.Path
import java.lang.invoke.MethodHandle as JMethodHandle
import java.lang.invoke.VarHandle as JVarHandle

@JvmInline
expect value class SymbolLookup internal constructor(internal val value: Any) {
    fun find(name: String): MemorySegment?
    fun findOrThrow(name: String): MemorySegment

    companion object {
        fun loaderLookup(): SymbolLookup
        fun libraryLookup(name: String, arena: Arena): SymbolLookup
        fun libraryLookup(path: Path, arena: Arena): SymbolLookup
    }
}

@JvmInline
expect value class Linker internal constructor(internal val value: Any) {
    fun downcallHandle(
        symbol: MemorySegment,
        descriptor: FunctionDescriptor
    ): MethodHandle

    fun upcallStub(
        target: MethodHandle,
        descriptor: FunctionDescriptor,
        arena: Arena
    ): MemorySegment

    fun defaultLookup(): SymbolLookup

    companion object {
        fun nativeLinker(): Linker
    }
}

fun Linker.upcallStub(
    target: JMethodHandle,
    descriptor: FunctionDescriptor,
    arena: Arena
): MemorySegment = upcallStub(MethodHandle(target), descriptor, arena)

@JvmInline
expect value class MethodHandle(val value: JMethodHandle) {
    operator fun invoke(vararg args: Any?): Any?
    fun bindTo(target: Any?): MethodHandle
}

@JvmInline
expect value class VarHandle(val value: Any) {
    fun set(vararg args: Any?)
    fun get(vararg args: Any?): Any?
}

@JvmInline
expect value class FunctionDescriptor(val value: Any) {
    companion object {
        fun of(returnType: MemoryLayout, vararg parameters: MemoryLayout): FunctionDescriptor
        fun ofVoid(vararg parameters: MemoryLayout): FunctionDescriptor
    }
}

interface SegmentAllocator {
    fun allocate(size: Long, alignment: Long = 1): MemorySegment
    fun allocate(layout: MemoryLayout): MemorySegment
    fun allocate(layout: MemoryLayout, count: Long): MemorySegment

    fun allocateFrom(str: String, charset: Charset = Charsets.UTF_8): MemorySegment
    fun allocateFrom(layout: ValueLayout.OfByte, value: Byte): MemorySegment
    fun allocateFrom(layout: ValueLayout.OfChar, value: Char): MemorySegment
    fun allocateFrom(layout: ValueLayout.OfShort, value: Short): MemorySegment
    fun allocateFrom(layout: ValueLayout.OfInt, value: Int): MemorySegment
    fun allocateFrom(layout: ValueLayout.OfLong, value: Long): MemorySegment
    fun allocateFrom(layout: ValueLayout.OfFloat, value: Float): MemorySegment
    fun allocateFrom(layout: ValueLayout.OfDouble, value: Double): MemorySegment
    fun allocateFrom(layout: AddressLayout, value: MemorySegment): MemorySegment

    fun allocateFrom(elementLayout: ValueLayout.OfByte, vararg values: Byte): MemorySegment
    fun allocateFrom(elementLayout: ValueLayout.OfChar, vararg values: Char): MemorySegment
    fun allocateFrom(elementLayout: ValueLayout.OfShort, vararg values: Short): MemorySegment
    fun allocateFrom(elementLayout: ValueLayout.OfInt, vararg values: Int): MemorySegment
    fun allocateFrom(elementLayout: ValueLayout.OfLong, vararg values: Long): MemorySegment
    fun allocateFrom(elementLayout: ValueLayout.OfFloat, vararg values: Float): MemorySegment
    fun allocateFrom(elementLayout: ValueLayout.OfDouble, vararg values: Double): MemorySegment
}

@JvmInline
expect value class Arena internal constructor(internal val value: Any) : SegmentAllocator,
    AutoCloseable {
    companion object {
        fun ofAuto(): Arena
        fun ofShared(): Arena
        fun ofConfined(): Arena
        fun global(): Arena
    }

    val scope: MemorySegment.Scope

    override fun close()

    override fun allocate(size: Long, alignment: Long): MemorySegment
    override fun allocate(layout: MemoryLayout): MemorySegment
    override fun allocate(layout: MemoryLayout, count: Long): MemorySegment

    override fun allocateFrom(str: String, charset: Charset): MemorySegment
    override fun allocateFrom(layout: ValueLayout.OfByte, value: Byte): MemorySegment
    override fun allocateFrom(layout: ValueLayout.OfChar, value: Char): MemorySegment
    override fun allocateFrom(layout: ValueLayout.OfShort, value: Short): MemorySegment
    override fun allocateFrom(layout: ValueLayout.OfInt, value: Int): MemorySegment
    override fun allocateFrom(layout: ValueLayout.OfLong, value: Long): MemorySegment
    override fun allocateFrom(layout: ValueLayout.OfFloat, value: Float): MemorySegment
    override fun allocateFrom(layout: ValueLayout.OfDouble, value: Double): MemorySegment
    override fun allocateFrom(layout: AddressLayout, value: MemorySegment): MemorySegment
    override fun allocateFrom(elementLayout: ValueLayout.OfByte, vararg values: Byte): MemorySegment
    override fun allocateFrom(elementLayout: ValueLayout.OfChar, vararg values: Char): MemorySegment
    override fun allocateFrom(
        elementLayout: ValueLayout.OfShort,
        vararg values: Short
    ): MemorySegment

    override fun allocateFrom(elementLayout: ValueLayout.OfInt, vararg values: Int): MemorySegment
    override fun allocateFrom(elementLayout: ValueLayout.OfLong, vararg values: Long): MemorySegment
    override fun allocateFrom(
        elementLayout: ValueLayout.OfFloat,
        vararg values: Float
    ): MemorySegment

    override fun allocateFrom(
        elementLayout: ValueLayout.OfDouble,
        vararg values: Double
    ): MemorySegment
}

@JvmInline
expect value class MemorySegment internal constructor(internal val value: Any) {
    val address: Long
    fun asReadOnly(): MemorySegment
    fun asSlice(offset: Long, size: Long): MemorySegment
    fun asSlice(offset: Long, newSize: Long, byteAlignment: Long): MemorySegment
    fun asSlice(offset: Long, layout: MemoryLayout): MemorySegment
    fun asSlice(offset: Long): MemorySegment
    fun reinterpret(
        size: Long,
        arena: Arena? = null,
        cleanup: ((MemorySegment) -> Unit)? = null
    ): MemorySegment

    fun getString(offset: Long, charset: Charset = Charsets.UTF_8): String
    fun asByteBuffer(): ByteBuffer

    fun get(layout: ValueLayout.OfBoolean, offset: Long): Boolean
    fun set(layout: ValueLayout.OfBoolean, offset: Long, value: Boolean)
    fun getAtIndex(layout: ValueLayout.OfBoolean, index: Long): Boolean
    fun setAtIndex(layout: ValueLayout.OfBoolean, index: Long, value: Boolean)

    fun get(layout: ValueLayout.OfByte, offset: Long): Byte
    fun set(layout: ValueLayout.OfByte, offset: Long, value: Byte)
    fun getAtIndex(layout: ValueLayout.OfByte, index: Long): Byte
    fun setAtIndex(layout: ValueLayout.OfByte, index: Long, value: Byte)

    fun get(layout: ValueLayout.OfChar, offset: Long): Char
    fun set(layout: ValueLayout.OfChar, offset: Long, value: Char)
    fun getAtIndex(layout: ValueLayout.OfChar, index: Long): Char
    fun setAtIndex(layout: ValueLayout.OfChar, index: Long, value: Char)

    fun get(layout: ValueLayout.OfShort, offset: Long): Short
    fun set(layout: ValueLayout.OfShort, offset: Long, value: Short)
    fun getAtIndex(layout: ValueLayout.OfShort, index: Long): Short
    fun setAtIndex(layout: ValueLayout.OfShort, index: Long, value: Short)

    fun get(layout: ValueLayout.OfInt, offset: Long): Int
    fun set(layout: ValueLayout.OfInt, offset: Long, value: Int)
    fun getAtIndex(layout: ValueLayout.OfInt, index: Long): Int
    fun setAtIndex(layout: ValueLayout.OfInt, index: Long, value: Int)

    fun get(layout: ValueLayout.OfLong, offset: Long): Long
    fun set(layout: ValueLayout.OfLong, offset: Long, value: Long)
    fun getAtIndex(layout: ValueLayout.OfLong, index: Long): Long
    fun setAtIndex(layout: ValueLayout.OfLong, index: Long, value: Long)

    fun get(layout: ValueLayout.OfFloat, offset: Long): Float
    fun set(layout: ValueLayout.OfFloat, offset: Long, value: Float)
    fun getAtIndex(layout: ValueLayout.OfFloat, index: Long): Float
    fun setAtIndex(layout: ValueLayout.OfFloat, index: Long, value: Float)

    fun get(layout: ValueLayout.OfDouble, offset: Long): Double
    fun set(layout: ValueLayout.OfDouble, offset: Long, value: Double)
    fun getAtIndex(layout: ValueLayout.OfDouble, index: Long): Double
    fun setAtIndex(layout: ValueLayout.OfDouble, index: Long, value: Double)

    fun get(layout: AddressLayout, offset: Long): MemorySegment
    fun set(layout: AddressLayout, offset: Long, value: MemorySegment)
    fun getAtIndex(layout: AddressLayout, index: Long): MemorySegment
    fun setAtIndex(layout: AddressLayout, index: Long, value: MemorySegment)

    fun copyFrom(src: MemorySegment): MemorySegment

    class Scope {
        val isAlive: Boolean
    }

    companion object {
        fun ofArray(values: ByteArray): MemorySegment
        fun ofArray(values: CharArray): MemorySegment
        fun ofArray(values: ShortArray): MemorySegment
        fun ofArray(values: IntArray): MemorySegment
        fun ofArray(values: LongArray): MemorySegment
        fun ofArray(values: FloatArray): MemorySegment
        fun ofArray(values: DoubleArray): MemorySegment
        fun ofAddress(address: Long): MemorySegment

        val NULL: MemorySegment

        fun copy(
            src: MemorySegment,
            srcOffset: Long,
            dst: MemorySegment,
            dstOffset: Long,
            bytes: Long
        )

        fun copy(
            src: MemorySegment,
            srcElementLayout: ValueLayout,
            srcOffset: Long,
            dst: MemorySegment,
            dstElementLayout: ValueLayout,
            dstOffset: Long,
            count: Long,
        )

        fun copy(
            src: MemorySegment,
            srcLayout: ValueLayout,
            srcOffset: Long,
            dstArray: Any,
            dstIndex: Int,
            count: Int
        )

        fun copy(
            srcArray: Any,
            srcIndex: Int,
            dst: MemorySegment,
            dstLayout: ValueLayout,
            dstOffset: Long,
            count: Int
        )

        fun mismatch(
            src: MemorySegment,
            srcRange: LongRange,
            dst: MemorySegment,
            dstRange: LongRange
        ): Long
    }
}


expect sealed interface MemoryLayout {
    val value: Any

    @Suppress("RedundantModalityModifier")
    open val byteSize: Long

    @Suppress("RedundantModalityModifier")
    open val byteAlignment: Long

    @Suppress("RedundantModalityModifier")
    open fun byteOffset(path: List<PathElement>): Long

    @Suppress("RedundantModalityModifier")
    open fun varHandle(path: List<PathElement>): VarHandle
    fun withName(name: String): MemoryLayout

    @JvmInline
    value class PathElement internal constructor(internal val value: Any) {
        companion object {
            fun groupElement(name: String): PathElement
            fun groupElement(index: Long): PathElement
            fun sequenceElement(index: Long): PathElement
        }
    }

    companion object {
        fun sequenceLayout(elementCount: Long, elementLayout: MemoryLayout): SequenceLayout
        fun structLayout(elements: List<MemoryLayout>): StructLayout
        fun paddingLayout(byteSize: Long): PaddingLayout
        fun unionLayout(elements: List<MemoryLayout>): UnionLayout
    }
}

@JvmInline
expect value class SequenceLayout internal constructor(override val value: Any) : MemoryLayout {
    override fun withName(name: String): SequenceLayout
}

@JvmInline
expect value class GroupLayout internal constructor(override val value: Any) : MemoryLayout {
    override fun withName(name: String): GroupLayout
}

@JvmInline
expect value class PaddingLayout internal constructor(override val value: Any) : MemoryLayout {
    override fun withName(name: String): PaddingLayout
}

@JvmInline
expect value class StructLayout internal constructor(override val value: Any) : MemoryLayout {
    override fun withName(name: String): StructLayout
}

@JvmInline
expect value class UnionLayout internal constructor(override val value: Any) : MemoryLayout {
    override fun withName(name: String): UnionLayout
}

expect sealed interface ValueLayout : MemoryLayout {
    override fun withName(name: String): ValueLayout

    @JvmInline
    value class OfBoolean internal constructor(override val value: Any) : ValueLayout {
        override fun withName(name: String): OfBoolean
    }

    @JvmInline
    value class OfByte internal constructor(override val value: Any) : ValueLayout {
        override fun withName(name: String): OfByte
    }

    @JvmInline
    value class OfChar internal constructor(override val value: Any) : ValueLayout {
        override fun withName(name: String): OfChar
    }

    @JvmInline
    value class OfShort internal constructor(override val value: Any) : ValueLayout {
        override fun withName(name: String): OfShort
    }

    @JvmInline
    value class OfInt internal constructor(override val value: Any) : ValueLayout {
        override fun withName(name: String): OfInt
    }

    @JvmInline
    value class OfLong internal constructor(override val value: Any) : ValueLayout {
        override fun withName(name: String): OfLong
    }

    @JvmInline
    value class OfFloat internal constructor(override val value: Any) : ValueLayout {
        override fun withName(name: String): OfFloat
    }

    @JvmInline
    value class OfDouble internal constructor(override val value: Any) : ValueLayout {
        override fun withName(name: String): OfDouble
    }

    companion object {
        val ADDRESS: AddressLayout
        val JAVA_BYTE: OfByte
        val JAVA_BOOLEAN: OfBoolean
        val JAVA_CHAR: OfChar
        val JAVA_SHORT: OfShort
        val JAVA_INT: OfInt
        val JAVA_LONG: OfLong
        val JAVA_FLOAT: OfFloat
        val JAVA_DOUBLE: OfDouble
        val ADDRESS_UNALIGNED: AddressLayout
        val JAVA_CHAR_UNALIGNED: OfChar
        val JAVA_SHORT_UNALIGNED: OfShort
        val JAVA_INT_UNALIGNED: OfInt
        val JAVA_LONG_UNALIGNED: OfLong
        val JAVA_FLOAT_UNALIGNED: OfFloat
        val JAVA_DOUBLE_UNALIGNED: OfDouble
    }
}

@JvmInline
expect value class AddressLayout internal constructor(override val value: Any) : ValueLayout {
    override fun withName(name: String): AddressLayout
}
