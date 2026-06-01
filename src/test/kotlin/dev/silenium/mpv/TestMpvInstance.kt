package dev.silenium.mpv

import dev.silenium.mpv.native_bindings.node.Node
import dev.silenium.mpv.native_bindings.render.GLGetProcAddress
import dev.silenium.mpv.native_bindings.render.RenderParam
import org.lwjgl.opengl.GL
import org.lwjgl.system.FunctionProvider
import java.lang.foreign.MemorySegment

class TestMpvInstance(val updateCallback: () -> Unit = {}) {
    class LwjglGlProc(val provider: FunctionProvider) : GLGetProcAddress {
        override fun getProcAddress(name: String): MemorySegment {
            val address = provider.getFunctionAddress(name)
            return MemorySegment.ofAddress(address)
        }
    }

    val mpv = Mpv().apply {
        setProperty("vo", Node.String("libmpv")).getOrThrow()
        setProperty("hwdec", Node.String("no")).getOrThrow()
        setProperty("msg-level", Node.String("all=info")).getOrThrow()
        setProperty("terminal", Node.Flag(true)).getOrThrow()
        initialize().getOrThrow()
    }
    lateinit var render: Mpv.Render
    var initialized = false
    lateinit var funcProvider: FunctionProvider

    fun initialize() {
        if (initialized) return
        println("Initializing...")
        funcProvider = GL.getFunctionProvider()!!
        render = mpv.createRender(
            RenderParam.ApiType(RenderParam.ApiType.Api.OPENGL),
            RenderParam.AdvancedControl(true),
            RenderParam.OpenGLInitParams(LwjglGlProc(funcProvider)),
            updateCallback = updateCallback,
        ).getOrThrow()
        initialized = true
    }

    fun render(fbo: Int, width: Int, height: Int, format: Int) {
        initialize()
        render.update()
        render.render(
            RenderParam.OpenGLFBO(fbo, width, height, format),
            RenderParam.FlipY(true),
        ).getOrThrow()
    }

    fun dispose() {
        println("Disposing...")
        render.close()
    }
}
