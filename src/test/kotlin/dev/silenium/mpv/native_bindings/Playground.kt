package dev.silenium.mpv.native_bindings

import dev.silenium.mpv.native_bindings.event.Event.Id
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.foreign.Arena
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class MyCallback : LibMpvBindings.WakeupCallback {
    override fun invoke() {
        println("Wakeup!")
    }
}

fun main() {
    Arena.ofShared().use { arena ->
        val libc = LibcBindings()
        libc.setlocale(libc.LC_NUMERIC, "C")
        val libmpv = LibMpvBindings(arena)
        val handle = libmpv.mpv_create()
        val callback = MyCallback()
        var ret = libmpv.mpv_initialize(handle)
        println("MPV Initialized: $ret")
        if (ret != Error.SUCCESS) {
            println("MPV initialization failed: $ret")
            exitProcess(1)
        }
        libmpv.mpv_set_wakeup_callback(handle, callback)
        println("MPV Handle: $handle")
        ret = libmpv.mpv_command_async(handle, 1u, listOf("loadfile", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        if (ret != Error.SUCCESS) {
            println("MPV command failed: $ret")
            exitProcess(1)
        }

        CoroutineScope(Dispatchers.Default).launch {
            delay(3.seconds)
            libmpv.mpv_get_property_async(handle, 2u, "metadata")
        }
        while (true) {
            val event = libmpv.mpv_wait_event(handle, 10.0)
            if (event.eventId == Id.NONE) break
            println("MPV Event: $event")
        }

        libmpv.mpv_terminate_destroy(handle)
    }
}
