package dev.silenium.mpv.native_bindings

import dev.silenium.mpv.Mpv
import dev.silenium.mpv.native_bindings.event.Event.Id
import dev.silenium.mpv.native_bindings.event.EventProperty
import dev.silenium.mpv.native_bindings.node.Node
import dev.silenium.mpv.native_bindings.render.RenderParam
import dev.silenium.mpv.native_bindings.render.RenderParam.ApiType.Api
import dev.silenium.mpv.native_bindings.render.RenderParam.SWFormat
import kotlinx.coroutines.*
import java.lang.foreign.Arena
import kotlin.system.exitProcess
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class MyCallback : LibMpvBindings.WakeupCallback {
    override fun invoke() {
        println("Wakeup!")
    }
}

class MyUpdateCallback : LibMpvBindings.RenderUpdateCallback {
    override fun invoke() {
        println("Render Update!")
    }
}

@OptIn(FlowPreview::class)
class Playground {
    @Test
    fun nativePlayground() {
        Arena.ofShared().use { arena ->
            val libc = LibcBindings()
            libc.setlocale(libc.LC_NUMERIC, "C")
            val libmpv = LibMpvBindings(arena)
            val handle = libmpv.mpv_create()
            val callback = MyCallback()
            var ret = libmpv.mpv_set_property(handle, "vo", Node.String("null"))
            if (ret != Error.SUCCESS) {
                println("MPV property set failed: $ret")
                exitProcess(1)
            }
            ret = libmpv.mpv_initialize(handle)
            println("MPV Initialized: $ret")
            if (ret != Error.SUCCESS) {
                println("MPV initialization failed: $ret")
                exitProcess(1)
            }
            libmpv.mpv_set_wakeup_callback(handle, callback)
            println("MPV Handle: $handle")
            ret = libmpv.mpv_command_async(
                handle, 1u,
                listOf("loadfile", "src/test/resources/test.webm"),
            )
            if (ret != Error.SUCCESS) {
                println("MPV command failed: $ret")
                exitProcess(1)
            }

            CoroutineScope(Dispatchers.Default).launch {
                libmpv.mpv_set_property_async(handle, 3u, "vo", Node.String("null"))
                delay(3.seconds)
                libmpv.mpv_get_property(handle, "metadata").onSuccess {
                    println("Metadata: $it")
                }.onFailure {
                    println("Failed to get metadata: ${it.message}")
                }
                libmpv.mpv_get_property_async(handle, 2u, "metadata")
            }
            while (true) {
                val event = libmpv.mpv_wait_event(handle, 10.0)
                if (event.eventId == Id.GET_PROPERTY_REPLY) {
                    val reply = event.data as EventProperty
                    println("Metadata: ${reply.data}")
                    break
                }
                println("MPV Event: $event")
            }

            val renderHandle = libmpv.mpv_render_context_create(
                handle, listOf(
                    RenderParam.ApiType(Api.SW),
                    RenderParam.SWFormat(SWFormat.Format.FormatRGB0),
                )
            ).getOrThrow()
            println("RenderContext Handle: $renderHandle")
            val renderCallback = MyUpdateCallback()
            libmpv.mpv_render_context_set_update_callback(renderHandle, renderCallback)
            libmpv.mpv_render_context_free(renderHandle)

            libmpv.mpv_terminate_destroy(handle)
        }
    }

    @Test
    fun apiPlayground(): Unit = runBlocking {
        val mpv = Mpv()
        mpv.setProperty("vo", Node.String("null")).getOrThrow()
        mpv.initialize().getOrThrow()
        val eventJob = launch {
            mpv.events.collect {
                println("Event: $it")
            }
        }
        mpv.commandAsync("loadfile", "src/test/resources/test.webm").getOrThrow()
        val metadata = mpv.getPropertyAsync("metadata").getOrThrow()
        println("Metadata: $metadata")
        mpv.setPropertyAsync("vo", Node.String("null")).getOrThrow()
        delay(5.seconds)
        eventJob.cancel()
        mpv.close()
    }
}
