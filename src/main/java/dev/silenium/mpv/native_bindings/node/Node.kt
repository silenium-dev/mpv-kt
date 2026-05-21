package dev.silenium.mpv.native_bindings.node

import dev.silenium.mpv.native_bindings.Format
import dev.silenium.mpv.native_bindings.api.*
import java.lang.foreign.MemorySegment
import java.nio.ByteBuffer

sealed interface Node {
    data object None : Node
    data class String(val string: kotlin.String) : Node
    data class OsdString(val string: kotlin.String) : Node
    data class Flag(val flag: Boolean) : Node
    data class Int64(val int64: Long) : Node
    data class Double(val double: kotlin.Double) : Node
    data class List(val list: kotlin.collections.List<Node>) : Node {
        companion object Layout : InstantiableLayout<List> {
            override val layout by ListLayout::layout
            override fun from(segment: MemorySegment): List {
                val values = segment[ListLayout.values].map(Node::parse)
                return List(values)
            }
        }
    }

    data class Map(val map: kotlin.collections.Map<kotlin.String, Node>) : Node {
        companion object Layout : InstantiableLayout<Map> {
            override val layout by ListLayout::layout
            override fun from(segment: MemorySegment): Map {
                val keys = segment[ListLayout.keys]
                val values = segment[ListLayout.values].map {
                    Node.parse(it)
                }
                return Map(keys.zip(values).toMap())
            }
        }
    }

    data class ByteArray(val bytes: ByteBuffer) : Node {
        companion object Layout : NativeStructLayout(), InstantiableLayout<ByteArray> {
            val data = pointer("data")
            val size = ulong("size")

            override fun from(segment: MemorySegment): ByteArray {
                val size = segment[Layout.size]
                val buf = ByteBuffer.allocate(size.toInt())
                buf.put(segment[Layout.data].asByteBuffer())
                return ByteArray(buf)
            }
        }
    }

    object UnionLayout : NativeUnionLayout() {
        val string = string("string")
        val flag = bool("flag")
        val int64 = long("int64")
        val double = double("double_")

        val list = pointer("list")
        val ba = pointer("ba")
    }

    object ListLayout : NativeStructLayout() {
        val num = int("num")
        val padding_ = padding(4)
        val values = array("values", num, Layout.layout)
        val keys = stringArray("keys", num)
    }

    companion object Layout : NativeStructLayout(), InstantiableLayout<Node> {
        val union = union("u", UnionLayout)
        val format = enum<Format>("format")
        val padding_ = padding(4)

        override fun from(segment: MemorySegment): Node = when (segment[Layout.format]) {
            Format.None -> None
            Format.String -> String(segment[Layout.union][UnionLayout.string])
            Format.OsdString -> OsdString(segment[Layout.union][UnionLayout.string])
            Format.Flag -> Flag(segment[Layout.union][UnionLayout.flag])
            Format.Int64 -> Int64(segment[Layout.union][UnionLayout.int64])
            Format.Double -> Double(segment[Layout.union][UnionLayout.double])
            Format.Node -> error("Node cannot be of type Node")
            Format.NodeArray -> List.parse(segment[Layout.union][UnionLayout.list])
            Format.NodeMap -> Map.parse(segment[Layout.union][UnionLayout.list])
            Format.ByteArray -> ByteArray.parse(segment[Layout.union][UnionLayout.ba])
        }
    }
}
