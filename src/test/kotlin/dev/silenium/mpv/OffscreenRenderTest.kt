package dev.silenium.mpv

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL32
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class OffscreenRenderTest {
    @Test
    fun offscreenRenderTest() {
        val errorCallback = GLFWErrorCallback.createPrint(System.err).set()

        check(GLFW.glfwInit()) {
            "Failed to initialize GLFW"
        }
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE)
        GLFW.glfwSetErrorCallback(errorCallback)

        val window = GLFW.glfwCreateWindow(1280, 720, "MPV", 0L, 0L)
        check(window != 0L) {
            "Failed to create GLFW window"
        }
        GLFW.glfwMakeContextCurrent(window)
        GL.createCapabilities()
        val testMpvInstance = TestMpvInstance {
            GLFW.glfwPostEmptyEvent()
        }

        GL32.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        CoroutineScope(Dispatchers.Default).launch {
            delay(1.seconds)
            testMpvInstance.mpv.commandAsync("loadfile", "src/test/resources/test.webm").getOrThrow()
            delay(5.seconds)
            GLFW.glfwSetWindowShouldClose(window, true)
        }
        while (true) {
            GL32.glClear(GL32.GL_COLOR_BUFFER_BIT)
            GLFW.glfwSwapBuffers(window)
            if (GLFW.glfwWindowShouldClose(window)) break
            testMpvInstance.render(GL32.glGetInteger(GL32.GL_FRAMEBUFFER_BINDING), 1280, 720, GL32.GL_RGBA8)
            GLFW.glfwWaitEvents()
        }
        testMpvInstance.dispose()
        GLFW.glfwTerminate()
    }
}
