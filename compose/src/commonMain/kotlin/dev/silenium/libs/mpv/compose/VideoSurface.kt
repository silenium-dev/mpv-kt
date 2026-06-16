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

/**
 * Renders the video output of a [Mpv] player into a compose-gl [GLCanvas]
 * The mpv property `vo` has to be set to `libmpv` to make mpv render the video into this surface
 *
 * Additional parameters can be specified to customize the creation and rendering,
 * already preset parameters cannot be overridden
 *
 */
@Composable
fun VideoSurface(
    /**
     * [Mpv] player instance
     */
    mpv: Mpv,
    modifier: Modifier = Modifier,
    /**
     * Is called after the [Mpv.Render] was created
     */
    onInit: (Mpv) -> Unit = {},
    /**
     * Is called before the [Mpv.Render] is destroyed
     */
    onDispose: (Mpv) -> Unit = {},
    /**
     * Allows specification of additional renderer creation parameters passed to [Mpv.createRender]
     * Preset parameters which cannot be overridden:
     * - [RenderParam.ApiType] = [RenderParam.ApiType.Api.OPENGL]
     * - [RenderParam.OpenGLInitParams] = [ComposeGLProcProvider]
     * - [RenderParam.AdvancedControl] = true
     */
    additionalCreateParams: List<RenderParam.Create<*>> = emptyList(),
    /**
     * Allows specification of additional render parameters passed to [Mpv.Render.render]
     * Preset parameters which cannot be overridden:
     * - [RenderParam.OpenGLFBO] = [dev.silenium.compose.gl.canvas.GLDrawScope.fbo]
     */
    additionalRenderParams: List<RenderParam.Render<*>> = emptyList(),
) {
    val filterRenderParams = remember(additionalRenderParams) {
        additionalRenderParams.filterNot {
            it is RenderParam.OpenGLFBO
        }
    }

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
            val createParams = additionalCreateParams.filterNot {
                it is RenderParam.OpenGLInitParams
                        || it is RenderParam.ApiType
                        || it is RenderParam.AdvancedControl
            } + listOf(
                RenderParam.ApiType(RenderParam.ApiType.Api.OPENGL),
                RenderParam.OpenGLInitParams(ComposeGLProcProvider(this)),
                RenderParam.AdvancedControl(true),
            )
            render = mpv.createRender(
                params = createParams.toTypedArray(),
                updateCallback = state::requestUpdate,
            ).getOrThrow()
            onInit(mpv)
        }
        render?.update()
        val renderParams = filterRenderParams + listOf(
            RenderParam.OpenGLFBO(fbo.id, fbo.size.width, fbo.size.height, GL_RGBA8),
        )
        render?.render(params = renderParams.toTypedArray())?.getOrThrow()
    }
}

const val GL_RGBA8 = 0x8058
