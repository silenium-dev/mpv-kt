package dev.silenium.mpv

import dev.silenium.mpv.native_bindings.node.Node
import dev.silenium.mpv.native_bindings.render.GLGetProcAddress
import dev.silenium.mpv.native_bindings.render.RenderParam
import dev.silenium.mpv.native_bindings.render.RenderParam.ApiType.Api
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL32
import org.lwjgl.system.FunctionProvider
import java.lang.foreign.MemorySegment
import kotlin.time.Duration.Companion.seconds

class LwjglGlProc(val provider: FunctionProvider) : GLGetProcAddress {
    override fun getProcAddress(name: String): MemorySegment {
        val address = provider.getFunctionAddress(name)
        return MemorySegment.ofAddress(address)
    }
}

class MpvTest(val updateCallback: () -> Unit = {}) {
    val mpv = Mpv().apply {
        setProperty("vo", Node.String("libmpv")).getOrThrow()
        setProperty("hwdec", Node.String("auto")).getOrThrow()
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
            RenderParam.ApiType(Api.OPENGL),
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

fun main() {
    check(GLFW.glfwInit())
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
    GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE)
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2)

    val window = GLFW.glfwCreateWindow(1280, 720, "MPV", 0L, 0L)
    check(window != 0L) {
        "Failed to create GLFW window"
    }
    GLFW.glfwMakeContextCurrent(window)
    GL.createCapabilities()
    val mpvTest = MpvTest {
        GLFW.glfwPostEmptyEvent()
    }

    GL32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    CoroutineScope(Dispatchers.Default).launch {
        delay(1.seconds)
        mpvTest.mpv.commandAsync("loadfile", "src/test/resources/test.webm").getOrThrow()
    }
    while (true) {
        GL32.glClear(GL32.GL_COLOR_BUFFER_BIT)
        GLFW.glfwSwapBuffers(window)
        if (GLFW.glfwWindowShouldClose(window)) break
        mpvTest.render(GL32.glGetInteger(GL32.GL_FRAMEBUFFER_BINDING), 1280, 720, GL32.GL_RGBA8)
        GLFW.glfwWaitEvents()
    }
    mpvTest.dispose()
    GLFW.glfwTerminate()
}
