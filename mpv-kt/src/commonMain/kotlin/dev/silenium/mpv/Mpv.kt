package dev.silenium.mpv

import dev.silenium.mpv.core.Core
import dev.silenium.mpv.core.EventCallback
import dev.silenium.mpv.core.RenderCore
import dev.silenium.mpv.native_bindings.Error
import dev.silenium.mpv.native_bindings.LibMpvBindings
import dev.silenium.mpv.native_bindings.event.CommandReply
import dev.silenium.mpv.native_bindings.event.Event
import dev.silenium.mpv.native_bindings.event.Event.Id
import dev.silenium.mpv.native_bindings.event.EventProperty
import dev.silenium.mpv.native_bindings.event.LogMessage
import dev.silenium.mpv.native_bindings.event.LogMessage.Level
import dev.silenium.mpv.native_bindings.mpv
import dev.silenium.mpv.native_bindings.mpvFailure
import dev.silenium.mpv.native_bindings.node.Node
import dev.silenium.mpv.native_bindings.render.RenderParam
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.EnumSet

class Mpv : EventCallback, AutoCloseable {
    internal val core: Core = Core(this)
    private val requestIdCounter = atomic(0UL)
    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    private val logReceiver = CoroutineScope(Dispatchers.Default).launch {
        val ret = Core.mpv.mpv_request_log_messages(core.handle, Level.DEBUG)
        check(ret == Error.SUCCESS)
        events
            .filter { it.eventId == Id.LOG_MESSAGE }
            .map { it.data as LogMessage }
            .collect {
                mpvLog(it.prefix.replace("/", "."))
                    .atLevel(it.level.asSlf4j)
                    .log(it.text.trim())
            }
    }

    fun initialize() =
        Result.mpv(Core.mpv.mpv_initialize(core.handle))

    fun setProperty(name: String, value: Node) =
        Result.mpv(Core.mpv.mpv_set_property(core.handle, name, value))

    fun getProperty(name: String) =
        Core.mpv.mpv_get_property(core.handle, name)

    suspend fun setPropertyAsync(name: String, value: Node): Result<Unit> =
        asyncRequest(Id.SET_PROPERTY_REPLY) {
            mpv_set_property_async(core.handle, it, name, value)
        }

    suspend fun getPropertyAsync(name: String): Result<Node> =
        asyncRequest<EventProperty>(Id.GET_PROPERTY_REPLY) {
            mpv_get_property_async(core.handle, it, name)
        }.map(EventProperty::data)

    inline fun <reified T : Node> observe(name: String): Result<Flow<T>> {
        return observeProperty(name).map { flow -> flow.map { it as T } }
    }

    fun observeProperty(name: String): Result<Flow<Node>> {
        val requestId = nextRequestId()
        val result = Core.mpv.mpv_observe_property(core.handle, requestId, name)
        return Result.mpv(result) {
            channelFlow {
                events
                    .filter { it.eventId == Id.PROPERTY_CHANGE && it.replyUserdata == requestId }
                    .collect { send((it.data as EventProperty).data) }
            }.onCompletion {
                val result = Core.mpv.mpv_unobserve_property(core.handle, requestId)
                if (result != Error.SUCCESS) {
                    log.error("Failed to unobserve property: {}", result)
                }
            }
        }
    }

    suspend fun commandAsync(vararg args: String) = commandAsync(args.toList())
    suspend fun commandAsync(args: List<String>) = asyncRequest<CommandReply>(Id.COMMAND_REPLY) {
        mpv_command_async(core.handle, it, args)
    }.map(CommandReply::node)

    @JvmName("asyncRequest")
    private suspend inline fun <reified ED> asyncRequest(
        eventId: Id,
        crossinline request: LibMpvBindings.(requestId: ULong) -> Error,
    ): Result<ED> = coroutineScope {
        val requestId = nextRequestId()
        val subscribed = CompletableDeferred<Unit>()
        val reply = async {
            events
                .onStart { subscribed.complete(Unit) }
                .first { it.eventId == eventId && it.replyUserdata == requestId }
        }

        subscribed.await()
        val ret = Core.mpv.request(requestId)
        if (ret != Error.SUCCESS) {
            reply.cancelAndJoin()
            Result.mpvFailure(ret)
        } else {
            val reply = reply.await()
            Result.mpv(reply.error) {
                reply.data as ED
            }
        }
    }

    @JvmName("asyncRequestUnit")
    private suspend fun asyncRequest(
        eventId: Id,
        request: LibMpvBindings.(requestId: ULong) -> Error
    ): Result<Unit> = asyncRequest<Any?>(eventId, request).map {}

    private fun nextRequestId(): ULong = requestIdCounter.updateAndGet { it + 1u }

    override suspend fun onEvent(event: Event) = _events.emit(event)

    override fun close() {
        logReceiver.cancel()
        core.close()
    }

    fun createRender(vararg params: RenderParam.Create<*>, updateCallback: () -> Unit = {}) =
        RenderCore(core.handle, params.toList(), updateCallback).map(::Render)

    class Render(private val renderCore: RenderCore) : AutoCloseable {
        override fun close() = renderCore.close()

        enum class UpdateFlags(val value: ULong) {
            UpdateFrame(1UL shl 0),
        }

        fun update(): EnumSet<UpdateFlags> {
            val raw = Core.mpv.mpv_render_context_update(renderCore.context)
            val result = EnumSet.noneOf(UpdateFlags::class.java)
            for (flag in UpdateFlags.entries) {
                if (raw and flag.value != 0UL) result.add(flag)
            }
            return result
        }

        fun render(vararg params: RenderParam.Render<*>) =
            Core.mpv.mpv_render_context_render(renderCore.context, params.toList())

        fun reportSwap() = Core.mpv.mpv_render_context_report_swap(renderCore.context)
    }

    companion object {
        private fun mpvLog(prefix: String) = LoggerFactory.getLogger("mpv.$prefix")
        private val log = LoggerFactory.getLogger(Mpv::class.java)
    }
}
