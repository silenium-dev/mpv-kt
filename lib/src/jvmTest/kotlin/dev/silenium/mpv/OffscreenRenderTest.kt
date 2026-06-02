package dev.silenium.mpv

import dev.silenium.mpv.native_bindings.node.Node
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onFailure
import org.lwjgl.egl.EGL15.*
import org.lwjgl.egl.KHRNoConfigContext.EGL_NO_CONFIG_KHR
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL32.*
import org.lwjgl.system.Configuration
import org.lwjgl.system.MemoryUtil
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class OffscreenRenderTest {
    @Test
    fun offscreenRenderTest() {
        Configuration.DISABLE_CHECKS.set(true) // Required for EGL surfaceless & configless

        val eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY)
        check(eglDisplay != EGL_NO_DISPLAY)
        val major = MemoryUtil.memAllocInt(1)
        val minor = MemoryUtil.memAllocInt(1)
        check(eglInitialize(eglDisplay, major, minor))
        log.info("EGL initialized: ${major.get()}, ${minor.get()}")

        check(eglBindAPI(EGL_OPENGL_API))

        val ctxAttribs = intArrayOf(
            EGL_CONTEXT_MAJOR_VERSION, 3,
            EGL_CONTEXT_MINOR_VERSION, 2,
            EGL_NONE,
        )
        val eglContext = eglCreateContext(eglDisplay, EGL_NO_CONFIG_KHR, EGL_NO_CONTEXT, ctxAttribs)
        check(eglContext != EGL_NO_CONTEXT)

        eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, eglContext)

        GL.createCapabilities()
        val updateChan = Channel<Unit>(Channel.CONFLATED)
        val testMpvInstance = TestMpvInstance {
            updateChan.trySend(Unit)
                .onClosed { return@TestMpvInstance }
                .onFailure { log.error("Could not trigger update!") }
        }

        CoroutineScope(Dispatchers.Default).launch {
            delay(1.seconds)
            testMpvInstance.mpv.commandAsync("loadfile", TestMpvInstance.TEST_VIDEO).getOrThrow()
            delay(1.seconds)
            updateChan.close()
        }
        val fbo = glGenFramebuffers()
        val color = glGenRenderbuffers()
        val depth = glGenRenderbuffers()
        glBindFramebuffer(GL_FRAMEBUFFER, fbo)
        glBindRenderbuffer(GL_RENDERBUFFER, color)
        glRenderbufferStorage(GL_RENDERBUFFER, GL_RGBA8, 1280, 720)
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, color)
        glBindRenderbuffer(GL_RENDERBUFFER, depth)
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, 1280, 720)
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depth)
        check(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE)
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        runBlocking {
            while (isActive) {
                glClear(GL_COLOR_BUFFER_BIT)
                glViewport(0, 0, 1280, 720)
                testMpvInstance.render(fbo, 1280, 720, GL_RGBA8)
                testMpvInstance.render.reportSwap()
                updateChan.receiveCatching().onClosed {
                    break
                }
            }
            val pos = (testMpvInstance.mpv.getPropertyAsync("time-pos/full").getOrThrow() as Node.Double).double.seconds
            assert(pos in 0.5.seconds..1.5.seconds)
            val voDropped = (testMpvInstance.mpv.getPropertyAsync("frame-drop-count").getOrThrow() as Node.Int64).int64
            val decoderDropped = (testMpvInstance.mpv.getPropertyAsync("decoder-frame-drop-count").getOrThrow() as Node.Int64).int64
            assert(voDropped == 0.toLong())
            assert(decoderDropped == 0.toLong())
        }
        testMpvInstance.dispose()
        glDeleteFramebuffers(fbo)
        glDeleteRenderbuffers(intArrayOf(color, depth))

        eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)
        eglDestroyContext(eglDisplay, eglContext)
        eglTerminate(eglDisplay)
    }

    companion object {
        private val log = LoggerFactory.getLogger(OffscreenRenderTest::class.java)
    }
}
