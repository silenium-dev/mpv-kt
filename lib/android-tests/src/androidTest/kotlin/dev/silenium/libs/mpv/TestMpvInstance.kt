package dev.silenium.libs.mpv

import dev.silenium.mpv.Mpv
import dev.silenium.mpv.native_bindings.node.Node
import dev.silenium.mpv.native_bindings.render.RenderParam
import org.slf4j.LoggerFactory

class TestMpvInstance(val updateCallback: () -> Unit = {}) {
    val mpv = Mpv().apply {
        setProperty("vo", Node.String("libmpv")).getOrThrow()
        setProperty("hwdec", Node.String("auto")).getOrThrow()
        initialize().getOrThrow()
    }
    lateinit var render: Mpv.Render
    var initialized = false

    fun initialize() {
        if (initialized) return
        log.info("Initializing...")
        render = mpv.createRender(
            RenderParam.ApiType(RenderParam.ApiType.Api.OPENGL),
            RenderParam.AdvancedControl(true),
            RenderParam.OpenGLInitParams(EGLGetProcAddress),
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
        log.info("Disposing...")
        render.close()
    }

    companion object {
        private val log = LoggerFactory.getLogger(TestMpvInstance::class.java)
    }
}
