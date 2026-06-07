package dev.silenium.mpv.native_bindings

import dev.silenium.mpv.Mpv
import dev.silenium.mpv.native_bindings.event.Event.Id
import dev.silenium.mpv.native_bindings.event.EventProperty
import dev.silenium.mpv.native_bindings.node.Node
import dev.silenium.mpv.native_bindings.render.RenderParam
import dev.silenium.mpv.native_bindings.render.RenderParam.ApiType.Api
import dev.silenium.mpv.native_bindings.render.RenderParam.SWFormat.Format
import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filterNot
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

class MyCallback : LibMpvBindings.WakeupCallback {
    override fun invoke() {
        log.info("Wakeup!")
    }

    companion object {
        private val log = LoggerFactory.getLogger(MyCallback::class.java)
    }
}

class MyUpdateCallback : LibMpvBindings.RenderUpdateCallback {
    override fun invoke() {
        log.info("Render Update!")
    }

    companion object {
        private val log = LoggerFactory.getLogger(MyUpdateCallback::class.java)
    }
}

abstract class SmokeTest(videoFile: Path) : FunSpec({
    val log = LoggerFactory.getLogger(SmokeTest::class.java)
    test("nativeSmokeTest") {
        val libc = LibcBindings()
        libc.setlocale(libc.LC_NUMERIC, "C")
        val libmpv = LibMpvBindings()
        val handle = libmpv.mpv_create()
        val callback = MyCallback()
        var ret = libmpv.mpv_set_property(handle, "vo", Node.String("null"))
        if (ret != Error.SUCCESS) {
            log.error("MPV property set failed: {}", ret)
            exitProcess(1)
        }
        ret = libmpv.mpv_initialize(handle)
        log.info("MPV Initialized: {}", ret)
        if (ret != Error.SUCCESS) {
            log.error("MPV initialization failed: {}", ret)
            exitProcess(1)
        }
        libmpv.mpv_set_wakeup_callback(handle, callback)
        log.info("MPV Handle: $handle")
        ret = libmpv.mpv_command_async(
            handle, 1u,
            listOf("loadfile", videoFile.absolutePathString()),
        )
        if (ret != Error.SUCCESS) {
            log.error("MPV command failed: {}", ret)
            exitProcess(1)
        }

        println(Path(".").absolutePathString())
        CoroutineScope(Dispatchers.Default).launch {
            libmpv.mpv_set_property_async(handle, 3u, "vo", Node.String("null"))
            delay(3.seconds)
            libmpv.mpv_get_property(handle, "metadata").onSuccess {
                log.info("Media Metadata: {}", it)
            }.onFailure {
                log.error("Failed to get metadata", it)
            }
            libmpv.mpv_get_property_async(handle, 2u, "metadata")
        }
        while (true) {
            val event = libmpv.mpv_wait_event(handle, 10.0)
            if (event.eventId == Id.GET_PROPERTY_REPLY) {
                val reply = event.data as EventProperty
                log.info("Async Media Metadata: {}", reply.data)
                break
            }
            log.debug("MPV Event: {}", event)
        }

        val renderHandle = libmpv.mpv_render_context_create(
            handle, listOf(
                RenderParam.ApiType(Api.SW),
                RenderParam.SWFormat(Format.FormatRGB0),
            )
        ).getOrThrow()
        log.info("RenderContext Handle: {}", renderHandle)
        val renderCallback = MyUpdateCallback()
        libmpv.mpv_render_context_set_update_callback(renderHandle, renderCallback)
        libmpv.mpv_render_context_free(renderHandle)

        libmpv.mpv_terminate_destroy(handle)
    }

    test("apiSmokeTest") {
        val mpv = Mpv()
        mpv.setProperty("vo", Node.String("null")).getOrThrow()
        mpv.initialize().getOrThrow()
        val eventJob = launch {
            mpv.events.filterNot { it.eventId == Id.LOG_MESSAGE }.collect {
                log.debug("Event: {}", it)
            }
        }
        mpv.commandAsync("loadfile", videoFile.absolutePathString()).getOrThrow()
        val metadata = mpv.getPropertyAsync("metadata").getOrThrow()
        log.info("Metadata: {}", metadata)
        mpv.setPropertyAsync("vo", Node.String("null")).getOrThrow()
        delay(5.seconds)
        eventJob.cancel()
        mpv.close()
    }
})
