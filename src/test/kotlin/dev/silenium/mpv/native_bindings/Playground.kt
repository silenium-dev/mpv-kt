package dev.silenium.mpv.native_bindings

import java.lang.foreign.Arena
import kotlin.system.exitProcess
import kotlin.use

class MyCallback : LibMpvBindings.WakeupCallback {
    override fun invoke() {
        println("Wakeup!")
    }
}

fun main() {
    Arena.ofConfined().use { arena ->
        val libc = LibcBindings()
        libc.setlocale(libc.LC_NUMERIC, "C")
        val libmpv = LibMpvBindings(arena)
        val handle = libmpv.mpv_create()
        val callback = MyCallback()
        val ret = libmpv.mpv_initialize(handle)
        println("MPV Initialized: $ret")
        if (ret < 0) {
            println("MPV initialization failed with error code: $ret")
            exitProcess(1)
        }
        libmpv.mpv_set_wakeup_callback(handle, callback)
        println("MPV Handle: $handle")
        val event = libmpv.mpv_wait_event(handle, 10.0)
        println("MPV Event: $event")

        libmpv.mpv_terminate_destroy(handle)
    }
}
