package dev.silenium.mpv

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.silenium.compose.gl.canvas.GLCanvas
import dev.silenium.compose.gl.canvas.rememberGLCanvasState
import dev.silenium.mpv.core.RenderCore
import dev.silenium.mpv.native_bindings.render.GLGetProcAddress
import dev.silenium.mpv.native_bindings.render.RenderParam
import dev.silenium.mpv.native_bindings.render.RenderParam.ApiType.Api
import org.lwjgl.opengl.GL
import org.lwjgl.system.FunctionProvider
import java.lang.foreign.MemorySegment

class LwjglGlProc(val provider: FunctionProvider) : GLGetProcAddress {
    override fun getProcAddress(name: String): MemorySegment {
        val address = provider.getFunctionAddress(name) ?: return MemorySegment.NULL
        return MemorySegment.ofAddress(address)
    }
}

class MpvTest {
    val mpv = Mpv().apply {
        initialize().getOrThrow()
    }
    lateinit var render: RenderCore
    var initialized = false
    lateinit var funcProvider: FunctionProvider

    fun initialize() {
        if (initialized) return
        println("Initializing...")
        initialized = true
        GL.createCapabilities()
        funcProvider = GL.getFunctionProvider()!!
        render = mpv.createRender(
            RenderParam.ApiType(Api.OPENGL),
            RenderParam.OpenGLInitParams(LwjglGlProc(funcProvider)),
        ) {
            println("Render update callback")
        }.getOrThrow()
    }

    fun dispose() {
        println("Disposing...")
        render.close()
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "MPV") {
        val state = rememberGLCanvasState()
        val mpvTest = remember { MpvTest() }
        GLCanvas(
            modifier = Modifier.fillMaxSize(),
            state = state,
            onDispose = {
                mpvTest.dispose()
            }
        ) {
            mpvTest.initialize()
        }
    }
}
