package dev.silenium.mpv.native_bindings.render

import dev.silenium.libs.foreign.Arena
import dev.silenium.libs.foreign.FunctionDescriptor
import dev.silenium.libs.foreign.Linker
import dev.silenium.libs.foreign.MemoryLayout
import dev.silenium.libs.foreign.MemorySegment
import dev.silenium.libs.foreign.ValueLayout
import dev.silenium.libs.foreign.upcallStub
import dev.silenium.mpv.native_bindings.api.InstantiatedStruct
import dev.silenium.mpv.native_bindings.api.NativeStructLayout
import dev.silenium.mpv.native_bindings.api.get
import dev.silenium.mpv.native_bindings.api.set
import dev.silenium.mpv.native_bindings.node.Node
import java.lang.invoke.MethodHandles
import kotlin.reflect.jvm.javaMethod

sealed class RenderParam<T> : InstantiatedStruct {
    abstract val type: RenderParamType
    abstract val value: T
    override val layout: MemoryLayout by Layout::layout

    final override fun into(arena: Arena): MemorySegment {
        val segment = arena.allocate(Layout.layout)
        segment[Layout.type, arena] = type
        segment[data, arena] = dataInto(arena)
        return segment
    }

    abstract fun dataInto(arena: Arena): MemorySegment

    sealed class Create<T>: RenderParam<T>()

    sealed class Render<T>: RenderParam<T>()

    data object Invalid : RenderParam<Nothing?>() {
        override val value: Nothing? = null

        override val type: RenderParamType = RenderParamType.INVALID

        override fun dataInto(arena: Arena): MemorySegment = MemorySegment.NULL
    }

    data class ApiType(override val value: Api) : Create<ApiType.Api>() {
        override val type: RenderParamType = RenderParamType.API_TYPE

        enum class Api(internal val value: String) {
            OPENGL("opengl"),
            SW("sw"),
        }

        override fun dataInto(arena: Arena): MemorySegment = arena.allocateFrom(value.value)
    }

    data class OpenGLInitParams(val getProcAddress: GLGetProcAddress) : Create<OpenGLInitParams>() {
        override val type: RenderParamType = RenderParamType.OPENGL_INIT_PARAMS
        override val value: OpenGLInitParams get() = this
        private val wrapper = GLGetProcAddressWrapper(getProcAddress)
        override fun dataInto(arena: Arena): MemorySegment {
            val handle =
                MethodHandles.lookup().unreflect(wrapper::call.javaMethod!!).bindTo(wrapper)
            val stub = Linker.nativeLinker().upcallStub(
                handle,
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                arena
            )

            val segment = arena.allocate(Layout.layout)
            segment[Layout.getProcAddress, arena] = stub
            segment[ctx, arena] = MemorySegment.NULL
            return segment
        }

        companion object Layout : NativeStructLayout() {
            val getProcAddress = pointer("get_proc_address")
            val ctx = pointer("ctx")
        }
    }

    data class OpenGLFBO(
        val fbo: Int,
        val width: Int,
        val height: Int,
        val internalFormat: Int,
    ) : Render<OpenGLFBO>() {
        override val type: RenderParamType = RenderParamType.OPENGL_FBO
        override val value: OpenGLFBO get() = this
        override fun dataInto(arena: Arena): MemorySegment {
            val segment = arena.allocate(Layout.layout)
            segment[Layout.fbo, arena] = fbo
            segment[Layout.width, arena] = width
            segment[Layout.height, arena] = height
            segment[Layout.internalFormat, arena] = internalFormat
            return segment
        }

        companion object Layout : NativeStructLayout() {
            val fbo = int("fbo")
            val width = int("w")
            val height = int("h")
            val internalFormat = int("internal_format")
        }
    }

    data class FlipY(override val value: Boolean) : Render<Boolean>() {
        override val type: RenderParamType = RenderParamType.FLIP_Y
        override fun dataInto(arena: Arena): MemorySegment {
            return arena.allocateFrom(ValueLayout.JAVA_INT, if (value) 1 else 0)
        }
    }

    data class Depth(override val value: Int) : Render<Int>() {
        override val type: RenderParamType = RenderParamType.DEPTH
        override fun dataInto(arena: Arena): MemorySegment {
            return arena.allocateFrom(ValueLayout.JAVA_INT, value)
        }
    }

    data class ICCProfile(override val value: ByteArray) : RenderParam<ByteArray>() {
        override val type: RenderParamType = RenderParamType.ICC_PROFILE
        override fun dataInto(arena: Arena): MemorySegment {
            val ba = Node.ByteArray(value)
            return ba.into(arena)
        }
    }

    data class AmbientLight(override val value: Int) : RenderParam<Int>() {
        override val type: RenderParamType = RenderParamType.AMBIENT_LIGHT
        override fun dataInto(arena: Arena): MemorySegment {
            return arena.allocateFrom(ValueLayout.JAVA_INT, value)
        }
    }

    data class X11Display(override val value: MemorySegment) : Create<MemorySegment>() {
        override val type: RenderParamType = RenderParamType.X11_DISPLAY
        override fun dataInto(arena: Arena): MemorySegment {
            return value
        }
    }

    data class WLDisplay(override val value: MemorySegment) : Create<MemorySegment>() {
        override val type: RenderParamType = RenderParamType.WL_DISPLAY
        override fun dataInto(arena: Arena): MemorySegment {
            return value
        }
    }

    data class AdvancedControl(override val value: Boolean) : Create<Boolean>() {
        override val type: RenderParamType = RenderParamType.ADVANCED_CONTROL
        override fun dataInto(arena: Arena): MemorySegment {
            return arena.allocateFrom(ValueLayout.JAVA_INT, if (value) 1 else 0)
        }
    }

    class NextFrameInfo(arena: Arena) : RenderParam<MemorySegment>() {
        private val resultArea = arena.allocate(Layout.layout)
        override val type: RenderParamType = RenderParamType.NEXT_FRAME_INFO
        override val value: MemorySegment = resultArea

        override fun dataInto(arena: Arena): MemorySegment {
            return resultArea
        }

        data class Value(val flags: ULong, val targetTime: Long) {
            constructor(segment: MemorySegment) : this(segment[flags], segment[targetTime])
        }

        fun get() = Value(resultArea)

        companion object Layout : NativeStructLayout() {
            val flags = ulong("flags")
            val targetTime = long("target_time")
        }
    }

    data class BlockForTargetTime(override val value: Boolean) : Render<Boolean>() {
        override val type: RenderParamType = RenderParamType.BLOCK_FOR_TARGET_TIME
        override fun dataInto(arena: Arena): MemorySegment {
            return arena.allocateFrom(ValueLayout.JAVA_INT, if (value) 1 else 0)
        }
    }

    data class SkipRendering(override val value: Boolean) : Render<Boolean>() {
        override val type: RenderParamType = RenderParamType.SKIP_RENDERING
        override fun dataInto(arena: Arena): MemorySegment {
            return arena.allocateFrom(ValueLayout.JAVA_INT, if (value) 1 else 0)
        }
    }

    data class SWSize(val width: Int, val height: Int) : Render<SWSize>() {
        override val type: RenderParamType = RenderParamType.SW_SIZE
        override val value: SWSize get() = this
        override fun dataInto(arena: Arena): MemorySegment {
            val segment = arena.allocate(ValueLayout.JAVA_INT, 2)
            segment.setAtIndex(ValueLayout.JAVA_INT, 0, width)
            segment.setAtIndex(ValueLayout.JAVA_INT, 1, height)
            return segment
        }
    }

    data class SWFormat(override val value: Format) : Render<SWFormat.Format>() {
        sealed interface Format {
            val value: String

            data object FormatRGB0 : Format {
                override val value: String = "rgb0"
            }

            data object FormatBGR0 : Format {
                override val value: String = "bgr0"
            }

            data object Format0BGR : Format {
                override val value: String = "0bgr"
            }

            data object Format0RGB : Format {
                override val value: String = "0rgb"
            }

            data object FormatRGB24 : Format {
                override val value: String = "rgb24"
            }

            data class FormatOther(override val value: String) : Format
        }

        override val type: RenderParamType = RenderParamType.SW_FORMAT
        override fun dataInto(arena: Arena): MemorySegment {
            return arena.allocateFrom(value.value)
        }
    }

    data class SWStride(override val value: ULong) : Render<ULong>() {
        override val type: RenderParamType = RenderParamType.SW_STRIDE
        override fun dataInto(arena: Arena): MemorySegment {
            return arena.allocateFrom(ValueLayout.JAVA_LONG, value.toLong())
        }
    }

    data class SWPointer(override val value: MemorySegment) : Render<MemorySegment>() {
        override val type: RenderParamType = RenderParamType.SW_POINTER
        override fun dataInto(arena: Arena): MemorySegment {
            return value
        }
    }

    companion object Layout : NativeStructLayout() {
        val type = enum<RenderParamType>("type")
        val padding = intPadding()
        val data = pointer("data")
    }
}

interface GLGetProcAddress {
    fun getProcAddress(name: String): MemorySegment
}

private class GLGetProcAddressWrapper(val glGetProcAddress: GLGetProcAddress) {
    fun call(unused: MemorySegment, name: MemorySegment) =
        glGetProcAddress.getProcAddress(name.reinterpret(Long.MAX_VALUE).getString(0L))
}
