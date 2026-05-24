package dev.silenium.mpv.native_bindings

import dev.silenium.mpv.native_bindings.event.Event.Id
import dev.silenium.mpv.native_bindings.event.EventProperty
import dev.silenium.mpv.native_bindings.node.Node
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.foreign.Arena
import kotlin.system.exitProcess
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class MyCallback : LibMpvBindings.WakeupCallback {
    override fun invoke() {
        println("Wakeup!")
    }
}

class Playground {
    @Test
    fun playground() {
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
                listOf("loadfile", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
            )
            if (ret != Error.SUCCESS) {
                println("MPV command failed: $ret")
                exitProcess(1)
            }

            CoroutineScope(Dispatchers.Default).launch {
                libmpv.mpv_set_property_async(handle, 3u, "vo", Node.String("null"))
                delay(3.seconds)
                libmpv.mpv_get_property(handle, "metadata").getOrThrow().let {
                    println("Metadata: $it")
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

            libmpv.mpv_terminate_destroy(handle)
        }
    }
}
