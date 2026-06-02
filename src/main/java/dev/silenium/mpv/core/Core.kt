package dev.silenium.mpv.core

import dev.silenium.mpv.native_bindings.LibMpvBindings
import dev.silenium.mpv.native_bindings.LibcBindings
import dev.silenium.mpv.native_bindings.event.Event
import dev.silenium.mpv.native_bindings.event.Event.Id
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import org.jetbrains.annotations.Blocking
import org.slf4j.LoggerFactory
import java.lang.foreign.Arena
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import dev.silenium.mpv.native_bindings.Handle as NativeHandle

class Core(private val callback: EventCallback) : AutoCloseable {
    internal val handle: NativeHandle = mpv.mpv_create()
    private val wakeupCh = Channel<Unit>(Channel.CONFLATED)
    private val wakeupCallback = WakeupCallback(wakeupCh)
    private val dispatcher = loopDispatcher()
    private val eventLoopJob = CoroutineScope(dispatcher).launch { eventLoop() }

    private val requestIdCounter = atomic(0UL)

    init {
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
        mpv.mpv_terminate_destroy(handle)
    }

    internal fun nextRequestId(): ULong = requestIdCounter.updateAndGet { it + 1u }

    companion object {
        private val log = LoggerFactory.getLogger(Core::class.java)

        internal val mpv = LibMpvBindings()
        private val libc = LibcBindings()

        init {
            libc.setlocale(libc.LC_NUMERIC, "C")
        }

        private val loopGroup = ThreadGroup("Mpv-EventLoop")
        private val loopIndex = AtomicInteger(0)
        private fun loopDispatcher(): ExecutorCoroutineDispatcher = Executors.newSingleThreadExecutor {
            Thread(loopGroup, it, "Mpv-EventLoop-${loopIndex.incrementAndGet()}")
        }.asCoroutineDispatcher()
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
