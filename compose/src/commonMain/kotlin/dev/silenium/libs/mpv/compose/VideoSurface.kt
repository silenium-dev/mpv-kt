package dev.silenium.libs.mpv.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.silenium.compose.gl.canvas.GLCanvas
import dev.silenium.compose.gl.canvas.GLProcAddressProvider
import dev.silenium.compose.gl.canvas.rememberGLCanvasState
import dev.silenium.libs.foreign.MemorySegment
import dev.silenium.mpv.Mpv
import dev.silenium.mpv.native_bindings.render.GLGetProcAddress
import dev.silenium.mpv.native_bindings.render.RenderParam

class ComposeGLProcProvider(val wrapped: GLProcAddressProvider) : GLGetProcAddress {
    override fun getProcAddress(name: String): MemorySegment =
        MemorySegment.ofAddress(wrapped.getGlProcAddress(name))
}

@Composable
fun VideoSurface(
    mpv: Mpv,
    modifier: Modifier = Modifier,
    onInit: (Mpv) -> Unit = {},
    onDispose: (Mpv) -> Unit = {}
) {
    var render: Mpv.Render? by remember { mutableStateOf(null) }
    DisposableEffect(mpv) {
        onDispose {
            onDispose(mpv)
            render?.close()
            render = null
        }
    }
    val state = rememberGLCanvasState()
    GLCanvas(
        state = state,
        modifier = modifier,
        onDispose = {
            render?.close()
            render = null
        },
    ) {
        if (render == null) {
            render = mpv.createRender(
                RenderParam.ApiType(RenderParam.ApiType.Api.OPENGL),
                RenderParam.OpenGLInitParams(ComposeGLProcProvider(this)),
                updateCallback = state::requestUpdate,
            ).getOrThrow()
            onInit(mpv)
        }
        render?.update()
        render?.render(
            RenderParam.OpenGLFBO(
                fbo.id,
                fbo.size.width,
                fbo.size.height,
                GL_RGBA8,
            ),
            RenderParam.FlipY(true),
        )?.getOrThrow()
    }
}

const val GL_RGBA8 = 0x8058
