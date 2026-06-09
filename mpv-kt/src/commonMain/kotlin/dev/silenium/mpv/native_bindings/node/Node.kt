package dev.silenium.mpv.native_bindings.node

import dev.silenium.libs.foreign.Arena
import dev.silenium.libs.foreign.MemoryLayout
import dev.silenium.libs.foreign.MemorySegment
import dev.silenium.mpv.native_bindings.api.InstantiableLayout
import dev.silenium.mpv.native_bindings.api.InstantiatedStruct
import dev.silenium.mpv.native_bindings.api.NativeStructLayout
import dev.silenium.mpv.native_bindings.api.NativeUnionLayout
import dev.silenium.mpv.native_bindings.api.get
import dev.silenium.mpv.native_bindings.api.parse
import dev.silenium.mpv.native_bindings.api.set
import java.nio.ByteBuffer

sealed interface Node : InstantiatedStruct {
    override val layout: MemoryLayout get() = Layout.layout

    data object None : Node {
        override fun into(arena: Arena): MemorySegment {
            val segment = arena.allocate(Layout.layout)
            segment[format, arena] = Format.None
            val u = arena.allocate(UnionLayout.layout)
            u[UnionLayout.flag, arena] = false
            segment[union, arena] = u
            return segment
        }
    }

    data class String(val string: kotlin.String) : Node {
        override fun into(arena: Arena): MemorySegment {
            val segment = arena.allocate(Layout.layout)
            segment[format, arena] = Format.String
            val u = arena.allocate(UnionLayout.layout)
            u[UnionLayout.string, arena] = string
            segment[union, arena] = u
            return segment
        }
    }

    data class OsdString(val string: kotlin.String) : Node {
        override fun into(arena: Arena): MemorySegment {
            val segment = arena.allocate(Layout.layout)
            segment[format, arena] = Format.OsdString
            val u = arena.allocate(UnionLayout.layout)
            u[UnionLayout.string, arena] = string
            segment[union, arena] = u
            return segment
        }
    }

    data class Flag(val flag: Boolean) : Node {
        override fun into(arena: Arena): MemorySegment {
            val segment = arena.allocate(Layout.layout)
            segment[format, arena] = Format.Flag
            val u = arena.allocate(UnionLayout.layout)
            u[UnionLayout.flag, arena] = flag
            segment[union, arena] = u
            return segment
        }
    }

    data class Int64(val int64: Long) : Node {
        override fun into(arena: Arena): MemorySegment {
            val segment = arena.allocate(Layout.layout)
            segment[format, arena] = Format.Int64
            val u = arena.allocate(UnionLayout.layout)
            u[UnionLayout.int64, arena] = int64
            segment[union, arena] = u
            return segment
        }
    }

    data class Double(val double: kotlin.Double) : Node {
        override fun into(arena: Arena): MemorySegment {
            val segment = arena.allocate(Layout.layout)
            segment[format, arena] = Format.Double
            val u = arena.allocate(UnionLayout.layout)
            u[UnionLayout.double, arena] = double
            segment[union, arena] = u
            return segment
        }
    }

    data class List(val list: kotlin.collections.List<Node>) : Node {
        override fun into(arena: Arena): MemorySegment {
            val segment = arena.allocate(Node.layout)
            segment[format, arena] = Format.NodeArray
            val u = arena.allocate(UnionLayout.layout)

            val values = list.map { it.into(arena) }
            val rawList = arena.allocate(ListLayout.layout)
            rawList[ListLayout.values, arena] = values

            u[UnionLayout.list, arena] = rawList
            segment[union, arena] = u

            return segment
        }

        companion object Layout : InstantiableLayout<List> {
            override val layout by ListLayout::layout
            override fun from(segment: MemorySegment): List {
                val values = segment[ListLayout.values].map(Node::parse)
                return List(values)
            }
        }
    }

    data class Map(val map: kotlin.collections.Map<kotlin.String, Node>) : Node {
        override fun into(arena: Arena): MemorySegment {
            val segment = arena.allocate(Node.layout)
            segment[format, arena] = Format.NodeMap
            val u = arena.allocate(UnionLayout.layout)

            val keys = map.keys.toList()
            val values = keys.map { map[it]!! }.map { it.into(arena) }
            val rawList = arena.allocate(ListLayout.layout)
            rawList[ListLayout.keys, arena] = keys
            rawList[ListLayout.values, arena] = values

            u[UnionLayout.list, arena] = rawList
            segment[union, arena] = u

            return segment
        }

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

    data class ByteArray(val bytes: kotlin.ByteArray) : Node {
        override fun into(arena: Arena): MemorySegment {
            val segment = arena.allocate(Node.layout)
            segment[format, arena] = Format.ByteArray
            val u = arena.allocate(UnionLayout.layout)

            val ba = arena.allocate(Layout.layout)
            val data = arena.allocate(bytes.size.toLong())
            data.copyFrom(MemorySegment.ofArray(bytes))
            ba[Layout.data, arena] = data
            ba[size, arena] = bytes.size.toULong()

            u[UnionLayout.ba, arena] = ba
            segment[union, arena] = u

            return segment
        }

        companion object Layout : NativeStructLayout(), InstantiableLayout<ByteArray> {
            val data = pointer("data")
            val size = ulong("size")

            override fun from(segment: MemorySegment): ByteArray {
                val size = segment[size]
                val buf = ByteBuffer.allocate(size.toInt())
                buf.put(segment[data].asByteBuffer())
                return ByteArray(buf.array())
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
        val padding_ = intPadding()
        val values = array("values", num, Node.layout)
        val keys = stringArray("keys", num)
    }

    companion object Layout : NativeStructLayout(), InstantiableLayout<Node> {
        val union = union("u", UnionLayout)
        val format = enum<Format>("format")
        val padding_ = intPadding()

        override fun from(segment: MemorySegment): Node = when (segment[format]) {
            Format.None -> None
            Format.String -> String(segment[union][UnionLayout.string])
            Format.OsdString -> OsdString(segment[union][UnionLayout.string])
            Format.Flag -> Flag(segment[union][UnionLayout.flag])
            Format.Int64 -> Int64(segment[union][UnionLayout.int64])
            Format.Double -> Double(segment[union][UnionLayout.double])
            Format.Node -> error("Node cannot be of type Node")
            Format.NodeArray -> List.parse(segment[union][UnionLayout.list])
            Format.NodeMap -> Map.parse(segment[union][UnionLayout.list])
            Format.ByteArray -> ByteArray.parse(segment[union][UnionLayout.ba])
        }
    }
}
