package dev.silenium.mpv.examples.android

import android.opengl.GLES32
import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import dev.silenium.mpv.Mpv
import dev.silenium.mpv.native_bindings.node.Node
import dev.silenium.mpv.native_bindings.render.RenderParam
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.time.Duration.Companion.seconds

class Renderer(val mpv: Mpv, val testVideo: File, val requestUpdate: () -> Unit) : GLSurfaceView.Renderer {
    private var render: Mpv.Render? = null
    private var size: IntSize = IntSize.Zero

    override fun onDrawFrame(gl: GL10) {
        render!!.render(
            RenderParam.OpenGLFBO(0, size.width, size.height, GLES32.GL_RGBA8),
            RenderParam.FlipY(true)
        ).getOrThrow()
    }

    override fun onSurfaceChanged(
        gl: GL10,
        width: Int,
        height: Int,
    ) {
        size = IntSize(width, height)
    }

    override fun onSurfaceCreated(
        gl: GL10,
        config: EGLConfig?,
    ) {
        render = mpv.createRender(
            RenderParam.OpenGLInitParams(EGLGetProcAddress),
            RenderParam.ApiType(RenderParam.ApiType.Api.OPENGL),
            updateCallback = requestUpdate,
        ).getOrThrow()
        CoroutineScope(Dispatchers.Default).launch {
            mpv.commandAsync("loadfile", testVideo.absolutePath).getOrThrow()
            delay(1.seconds)
            mpv.getPropertyAsync("hwdec").getOrThrow()
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val testVideo = assets.open("test.webm").use { input ->
            cacheDir.resolve("test.webm").apply {
                outputStream().use(input::copyTo)
            }
        }
        setContent {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val mpv = remember {
                    Mpv().apply {
                        setProperty("hwdec", Node.String("mediacodec")).getOrThrow()
                        setProperty("vo", Node.String("libmpv")).getOrThrow()
                        initialize().getOrThrow()
                    }
                }
                AndroidView(factory = {
                    GLSurfaceView(applicationContext).also {
                        it.setEGLContextClientVersion(3)
                        it.setRenderer(Renderer(mpv, testVideo) {
                            it.requestRender()
                        })
                        it.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                    }
                }, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
