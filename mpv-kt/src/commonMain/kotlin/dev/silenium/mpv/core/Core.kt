package dev.silenium.mpv.core

import dev.silenium.mpv.native_bindings.LibMpvBindings
import dev.silenium.mpv.native_bindings.LibcBindings
import dev.silenium.mpv.native_bindings.event.Event
import dev.silenium.mpv.native_bindings.event.Event.Id
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.Blocking
import org.slf4j.LoggerFactory
import dev.silenium.mpv.native_bindings.Handle as NativeHandle

class Core(private val callback: EventCallback) : AutoCloseable {
    internal val handle: NativeHandle = mpv.mpv_create()
    private val wakeupCh = Channel<Unit>(Channel.CONFLATED)
    private val wakeupCallback = WakeupCallback(wakeupCh)
    private val dispatcher: ExecutorCoroutineDispatcher
    private val eventLoopJob: Job

    init {
        dispatch { eventLoop() }.let {
            dispatcher = it.first
            eventLoopJob = it.second
        }
        mpv.mpv_set_wakeup_callback(handle, wakeupCallback)
    }

    private suspend fun CoroutineScope.eventLoop() {
        while (isActive) {
            val receiveResult = wakeupCh.receiveCatching()
            if (receiveResult.isClosed) break

            while (isActive) {
                val event = mpv.mpv_wait_event(handle, 0.0)
                if (event.eventId == Id.NONE) break
                withContext(Dispatchers.Default) {
                    try {
                        callback.onEvent(event)
                    } catch (c: CancellationException) {
                        throw c
                    } catch (t: Throwable) {
                        log.error("Error in event callback", t)
                    }
                }
            }
        }
    }

    @Blocking
    override fun close() = runBlocking {
        wakeupCh.close()
        eventLoopJob.cancelAndJoin()
        dispatcher.close()
        mpv.mpv_terminate_destroy(handle)
    }

    companion object: DispatcherCompanion("Mpv-Core") {
        private val log = LoggerFactory.getLogger(Core::class.java)

        internal val mpv = LibMpvBindings()
        private val libc = LibcBindings()

        init {
            libc.setlocale(libc.LC_NUMERIC, "C")
        }
    }
}


private class WakeupCallback(private val chan: Channel<Unit>) : LibMpvBindings.WakeupCallback {
    override fun invoke() {
        chan.trySend(Unit).onFailure {
            log.error("Could not trigger wake up!")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(WakeupCallback::class.java)
    }
}

interface EventCallback {
    suspend fun onEvent(event: Event)
}
