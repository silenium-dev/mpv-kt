package dev.silenium.libs.mpv

import android.opengl.EGL14
import android.opengl.EGL15
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES32.GL_COLOR_ATTACHMENT0
import android.opengl.GLES32.GL_COLOR_BUFFER_BIT
import android.opengl.GLES32.GL_DEPTH24_STENCIL8
import android.opengl.GLES32.GL_DEPTH_STENCIL_ATTACHMENT
import android.opengl.GLES32.GL_FRAMEBUFFER
import android.opengl.GLES32.GL_FRAMEBUFFER_COMPLETE
import android.opengl.GLES32.GL_RENDERBUFFER
import android.opengl.GLES32.GL_RGBA8
import android.opengl.GLES32.glBindFramebuffer
import android.opengl.GLES32.glBindRenderbuffer
import android.opengl.GLES32.glCheckFramebufferStatus
import android.opengl.GLES32.glClear
import android.opengl.GLES32.glClearColor
import android.opengl.GLES32.glDeleteFramebuffers
import android.opengl.GLES32.glDeleteRenderbuffers
import android.opengl.GLES32.glFramebufferRenderbuffer
import android.opengl.GLES32.glGenFramebuffers
import android.opengl.GLES32.glGenRenderbuffers
import android.opengl.GLES32.glRenderbufferStorage
import android.opengl.GLES32.glViewport
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import dev.silenium.mpv.native_bindings.node.Node
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.ranges.shouldBeIn
import io.kotest.matchers.shouldBe
import io.kotest.runner.junit4.FunSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.lang.AutoCloseable
import kotlin.time.Duration.Companion.seconds

data class EGLTestContext(
    val display: EGLDisplay,
    val context: EGLContext,
    val surface: EGLSurface
) : AutoCloseable {
    override fun close() {
        detach()
        EGL14.eglDestroySurface(display, surface)
        EGL14.eglDestroyContext(display, context)
        EGL14.eglTerminate(display)
    }

    fun attach() {
        EGL14.eglMakeCurrent(display, surface, surface, context)
    }

    fun detach() {
        EGL14.eglMakeCurrent(
            display,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT
        )
    }

    companion object {
        fun create(): EGLTestContext {
            val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(eglDisplay != EGL14.EGL_NO_DISPLAY) { "Failed to initialize EGL display" }
            val version = IntArray(2)
            check(
                EGL14.eglInitialize(
                    eglDisplay,
                    version,
                    0,
                    version,
                    1
                )
            ) { "Failed to initialize EGL" }

            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL15.EGL_OPENGL_ES3_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 24,
                EGL14.EGL_NONE
            )

            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            check(
                EGL14.eglChooseConfig(
                    eglDisplay,
                    configAttribs,
                    0,
                    configs,
                    0,
                    configs.size,
                    numConfigs,
                    0
                )
            ) { "Failed to choose EGL config" }
            check(numConfigs[0] > 0) { "No EGL configs found" }
            val config = configs[0] ?: error("No EGL config chosen")

            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL14.EGL_NONE
            )
            val eglContext = EGL14.eglCreateContext(
                eglDisplay,
                config,
                EGL14.EGL_NO_CONTEXT,
                contextAttribs,
                0
            )
            check(eglContext != EGL14.EGL_NO_CONTEXT) { "Failed to create EGL context" }
            val surfaceAttribs = intArrayOf(
                EGL14.EGL_WIDTH, 1,
                EGL14.EGL_HEIGHT, 1,
                EGL14.EGL_NONE,
            )
            val eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, config, surfaceAttribs, 0)
            check(eglSurface != EGL14.EGL_NO_SURFACE) { "Failed to create EGL surface" }

            return EGLTestContext(eglDisplay, eglContext, eglSurface)
        }
    }
}

class OffscreenRenderTest : FunSpec({
    val log = LoggerFactory.getLogger(OffscreenRenderTest::class.java)
    test("offscreen render test") {
        val architecture = System.getProperty("os.arch")
        log.info("Running on $architecture")
        log.info("Supported ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}")

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testVideo = context.assets.open("test.webm").use { input ->
            context.cacheDir.resolve("test.webm").apply {
                outputStream().use(input::copyTo)
            }
        }

        EGLTestContext.create().use {
            it.attach()
            val updateChan = Channel<Unit>(Channel.CONFLATED)
            val testMpvInstance = TestMpvInstance {
                updateChan.trySend(Unit)
                    .onClosed { return@TestMpvInstance }
                    .onFailure { log.error("Could not trigger update!") }
            }

            CoroutineScope(Dispatchers.Default).launch {
                delay(1.seconds)
                testMpvInstance.mpv.commandAsync("loadfile", testVideo.absolutePath).getOrThrow()
                delay(1.seconds)
                updateChan.close()
            }
            val fbos = IntArray(1)
            val rbos = IntArray(2)
            glGenFramebuffers(1, fbos, 0)
            glGenRenderbuffers(2, rbos, 0)
            val fbo = fbos[0]
            val color = rbos[0]
            val depth = rbos[1]
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
                updateChan.receiveCatching().onClosed {
                    break
                }
            }
            val voDropped = (testMpvInstance.mpv.getPropertyAsync("frame-drop-count")
                .getOrThrow() as Node.Int64).int64
            val decoderDropped = (testMpvInstance.mpv.getPropertyAsync("decoder-frame-drop-count")
                .getOrThrow() as Node.Int64).int64
            val estimatedFramePos = (testMpvInstance.mpv.getPropertyAsync("estimated-frame-number")
                .getOrThrow() as Node.Int64).int64
            estimatedFramePos shouldBeGreaterThan voDropped
            voDropped shouldBeLessThan 5
            decoderDropped shouldBe 0
            testMpvInstance.dispose()
            glDeleteFramebuffers(fbos.size, fbos, 0)
            glDeleteRenderbuffers(rbos.size, rbos, 0)
        }
    }
})
