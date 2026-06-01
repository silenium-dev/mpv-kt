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

        GLFW.glfwInitHint(GLFW.GLFW_PLATFORM, GLFW.GLFW_PLATFORM_NULL)
        check(GLFW.glfwInit()) {
            "Failed to initialize GLFW"
        }
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, GLFW.GLFW_EGL_CONTEXT_API)
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
            if (GLFW.glfwWindowShouldClose(window)) break
            GL32.glClear(GL32.GL_COLOR_BUFFER_BIT)
            GL32.glViewport(0, 0, 1280, 720)
            testMpvInstance.render(0, 1280, 720, 0)
            GLFW.glfwSwapBuffers(window)
            GLFW.glfwWaitEvents()
        }
        testMpvInstance.dispose()
        GLFW.glfwTerminate()
    }
}
