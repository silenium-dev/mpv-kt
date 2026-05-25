package dev.silenium.mpv

import dev.silenium.mpv.core.Core
import dev.silenium.mpv.core.EventCallback
import dev.silenium.mpv.native_bindings.Error
import dev.silenium.mpv.native_bindings.LibMpvBindings
import dev.silenium.mpv.native_bindings.event.CommandReply
import dev.silenium.mpv.native_bindings.event.Event
import dev.silenium.mpv.native_bindings.event.Event.Id
import dev.silenium.mpv.native_bindings.event.EventData
import dev.silenium.mpv.native_bindings.event.EventProperty
import dev.silenium.mpv.native_bindings.mpv
import dev.silenium.mpv.native_bindings.mpvFailure
import dev.silenium.mpv.native_bindings.node.Node
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first

class Mpv : EventCallback, AutoCloseable {
    internal val core: Core = Core(this)
    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    fun initialize() =
        Result.mpv(Core.mpv.mpv_initialize(core.handle))

    fun setProperty(name: String, value: Node) =
        Result.mpv(Core.mpv.mpv_set_property(core.handle, name, value))

    fun getProperty(name: String) =
        Core.mpv.mpv_get_property(core.handle, name)

    suspend fun setPropertyAsync(name: String, value: Node): Result<Unit> = asyncRequest(Id.SET_PROPERTY_REPLY) {
        mpv_set_property_async(core.handle, it, name, value)
    }

    suspend fun getPropertyAsync(name: String): Result<Node> = asyncRequest<EventProperty>(Id.GET_PROPERTY_REPLY) {
        mpv_get_property_async(core.handle, it, name)
    }.map(EventProperty::data)

    suspend fun commandAsync(vararg args: String) = commandAsync(args.toList())
    suspend fun commandAsync(args: List<String>) = asyncRequest<CommandReply>(Id.COMMAND_REPLY) {
        mpv_command_async(core.handle, it, args)
    }.map(CommandReply::node)

    @JvmName("asyncRequest")
    private suspend inline fun <reified ED> asyncRequest(
        eventId: Id,
        crossinline request: LibMpvBindings.(requestId: ULong) -> Error,
    ): Result<ED> = coroutineScope {
        val requestId = core.nextRequestId()
        val reply = async {
            events.first { it.eventId == eventId && it.replyUserdata == requestId }
        }

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
    private suspend fun asyncRequest(eventId: Id, request: LibMpvBindings.(requestId: ULong) -> Error): Result<Unit> =
        asyncRequest<Any?>(eventId, request).map {}

    override suspend fun onEvent(event: Event) {
        _events.emit(event)
    }

    override fun close() {
        core.close()
    }
}
