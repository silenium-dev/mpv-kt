package dev.silenium.mpv

import dev.silenium.mpv.native_bindings.node.Node
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ranges.shouldBeIn
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.lwjgl.egl.EGL15.EGL_CONTEXT_MAJOR_VERSION
import org.lwjgl.egl.EGL15.EGL_CONTEXT_MINOR_VERSION
import org.lwjgl.egl.EGL15.EGL_DEFAULT_DISPLAY
import org.lwjgl.egl.EGL15.EGL_NONE
import org.lwjgl.egl.EGL15.EGL_NO_CONTEXT
import org.lwjgl.egl.EGL15.EGL_NO_DISPLAY
import org.lwjgl.egl.EGL15.EGL_NO_SURFACE
import org.lwjgl.egl.EGL15.EGL_OPENGL_API
import org.lwjgl.egl.EGL15.eglBindAPI
import org.lwjgl.egl.EGL15.eglCreateContext
import org.lwjgl.egl.EGL15.eglDestroyContext
import org.lwjgl.egl.EGL15.eglGetDisplay
import org.lwjgl.egl.EGL15.eglInitialize
import org.lwjgl.egl.EGL15.eglMakeCurrent
import org.lwjgl.egl.EGL15.eglTerminate
import org.lwjgl.egl.KHRNoConfigContext.EGL_NO_CONFIG_KHR
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL32.GL_COLOR_ATTACHMENT0
import org.lwjgl.opengl.GL32.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL32.GL_DEPTH24_STENCIL8
import org.lwjgl.opengl.GL32.GL_DEPTH_STENCIL_ATTACHMENT
import org.lwjgl.opengl.GL32.GL_FRAMEBUFFER
import org.lwjgl.opengl.GL32.GL_FRAMEBUFFER_COMPLETE
import org.lwjgl.opengl.GL32.GL_RENDERBUFFER
import org.lwjgl.opengl.GL32.GL_RGBA8
import org.lwjgl.opengl.GL32.glBindFramebuffer
import org.lwjgl.opengl.GL32.glBindRenderbuffer
import org.lwjgl.opengl.GL32.glCheckFramebufferStatus
import org.lwjgl.opengl.GL32.glClear
import org.lwjgl.opengl.GL32.glClearColor
import org.lwjgl.opengl.GL32.glDeleteFramebuffers
import org.lwjgl.opengl.GL32.glDeleteRenderbuffers
import org.lwjgl.opengl.GL32.glFramebufferRenderbuffer
import org.lwjgl.opengl.GL32.glGenFramebuffers
import org.lwjgl.opengl.GL32.glGenRenderbuffers
import org.lwjgl.opengl.GL32.glRenderbufferStorage
import org.lwjgl.opengl.GL32.glViewport
import org.lwjgl.system.Configuration
import org.lwjgl.system.MemoryUtil
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

class OffscreenRenderTest : FunSpec({
    val log = LoggerFactory.getLogger(OffscreenRenderTest::class.java)

    test("offscreen render test") {
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
        glFramebufferRenderbuffer(
            GL_FRAMEBUFFER,
            GL_DEPTH_STENCIL_ATTACHMENT,
            GL_RENDERBUFFER,
            depth
        )
        check(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE)
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        while (isActive) {
            glClear(GL_COLOR_BUFFER_BIT)
            glViewport(0, 0, 1280, 720)
            testMpvInstance.render(fbo, 1280, 720, GL_RGBA8)
            testMpvInstance.render.reportSwap()
            updateChan.receiveCatching().onClosed {
                break
            }
        }
        val pos = (testMpvInstance.mpv.getPropertyAsync("time-pos/full")
            .getOrThrow() as Node.Double).double.seconds
        pos shouldBeIn 0.5.seconds..1.5.seconds
        val voDropped = (testMpvInstance.mpv.getPropertyAsync("frame-drop-count")
            .getOrThrow() as Node.Int64).int64
        val decoderDropped = (testMpvInstance.mpv.getPropertyAsync("decoder-frame-drop-count")
            .getOrThrow() as Node.Int64).int64
        voDropped shouldBe 0
        decoderDropped shouldBe 0
        testMpvInstance.dispose()
        glDeleteFramebuffers(fbo)
        glDeleteRenderbuffers(intArrayOf(color, depth))

        eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)
        eglDestroyContext(eglDisplay, eglContext)
        eglTerminate(eglDisplay)
    }
})
