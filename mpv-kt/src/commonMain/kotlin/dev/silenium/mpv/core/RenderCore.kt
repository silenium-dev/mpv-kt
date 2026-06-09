package dev.silenium.mpv.core

import dev.silenium.mpv.native_bindings.Handle
import dev.silenium.mpv.native_bindings.LibMpvBindings
import dev.silenium.mpv.native_bindings.RenderContext
import dev.silenium.mpv.native_bindings.render.RenderParam
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.Blocking
import org.slf4j.LoggerFactory

class RenderCore private constructor(
    internal val context: RenderContext,
    private val updateCallback: () -> Unit
) : AutoCloseable {
    private val updateCh = Channel<Unit>(Channel.CONFLATED)
    private val callback = UpdateCallback(updateCh)
    private val dispatcher: ExecutorCoroutineDispatcher
    private val eventLoopJob: Job

    init {
        dispatch { eventLoop() }.let {
            dispatcher = it.first
            eventLoopJob = it.second
        }
        Core.mpv.mpv_render_context_set_update_callback(context, callback)
    }

    @Blocking
    override fun close() = runBlocking {
        updateCh.close()
        eventLoopJob.cancelAndJoin()
        dispatcher.close()
        Core.mpv.mpv_render_context_free(context)
    }

    private suspend fun CoroutineScope.eventLoop() {
        while (isActive) {
            val receiveResult = updateCh.receiveCatching()
            if (receiveResult.isClosed) break

            updateCallback()
        }
    }

    companion object : DispatcherCompanion("Mpv-RenderCore") {
        operator fun invoke(
            handle: Handle,
            params: List<RenderParam<*>>,
            updateCallback: () -> Unit
        ): Result<RenderCore> = Core.mpv.mpv_render_context_create(handle, params).map {
            RenderCore(it, updateCallback)
        }
    }
}

private class UpdateCallback(private val chan: Channel<Unit>) : LibMpvBindings.RenderUpdateCallback {
    override fun invoke() {
        chan.trySend(Unit).onFailure {
            log.error("Could not trigger wake up!")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(UpdateCallback::class.java)
    }
}
