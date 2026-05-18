package dev.silenium.mpv.types

enum class Format(val value: Int) {
    None(0),
    String(1),
    OsdString(2),
    Flag(3),
    Int64(4),
    Double(5),
    Node(6),
    NodeArray(7),
    NodeMap(8),
    ByteArray(9),
}

sealed class Node {
    abstract val format: Format

    data object None : Node() {
        override val format = Format.None
    }

    data class Flag(val value: Boolean) : Node() {
        override val format = Format.Flag
    }

    data class Int64(val value: Long) : Node() {
        override val format = Format.Int64
    }

    data class Double(val value: kotlin.Double) : Node() {
        override val format = Format.Double
    }

    data class String(val value: kotlin.String) : Node() {
        override val format = Format.String
    }

    data class OsdString(val value: kotlin.String) : Node() {
        override val format = Format.OsdString
    }

    data class ByteArray(val value: kotlin.ByteArray) : Node() {
        override val format = Format.ByteArray

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ByteArray

            return value.contentEquals(other.value)
        }

        override fun hashCode(): Int {
            return value.contentHashCode()
        }
    }

    data class Array(val value: List<Node>) : Node() {
        // Used by JNI
        @Suppress("unused")
        constructor(array: kotlin.Array<Node>) : this(array.toList())

        // Used by JNI
        @Suppress("unused")
        private fun asArray() = value.toTypedArray()

        override val format = Format.NodeArray
    }

    data class Map(val keys: kotlin.Array<kotlin.String>, val values: kotlin.Array<Node>) : Node() {
        val map = keys.zip(values).toMap()

        override val format = Format.NodeMap

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Map

            if (!keys.contentEquals(other.keys)) return false
            if (!values.contentEquals(other.values)) return false
            if (map != other.map) return false
            if (format != other.format) return false

            return true
        }

        override fun hashCode(): Int {
            var result = keys.contentHashCode()
            result = 31 * result + values.contentHashCode()
            result = 31 * result + map.hashCode()
            result = 31 * result + format.hashCode()
            return result
        }
    }
}
