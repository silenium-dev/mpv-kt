@file:Suppress("FunctionName", "PrivatePropertyName", "PropertyName")

package dev.silenium.mpv.native_bindings

import dev.silenium.libs.foreign.Arena
import dev.silenium.libs.foreign.FunctionDescriptor
import dev.silenium.libs.foreign.Linker
import dev.silenium.libs.foreign.MemorySegment
import dev.silenium.libs.foreign.SymbolLookup
import dev.silenium.libs.foreign.ValueLayout
import dev.silenium.libs.foreign.upcallStub
import dev.silenium.mpv.native_bindings.api.parse
import dev.silenium.mpv.native_bindings.event.Event
import dev.silenium.mpv.native_bindings.event.LogMessage
import dev.silenium.mpv.native_bindings.node.Format
import dev.silenium.mpv.native_bindings.node.Node
import dev.silenium.mpv.native_bindings.render.RenderParam
import java.lang.invoke.MethodHandles
import kotlin.reflect.jvm.javaMethod

class LibMpvBindings {
    private val arena = Arena.ofAuto()
    val lookup: SymbolLookup = SymbolLookup.loaderLookup()
    val linker: Linker = Linker.nativeLinker()

    private val handle_mpv_create by lazy {
        val symbol = lookup.findOrThrow("mpv_create")
        linker.downcallHandle(symbol, FunctionDescriptor.of(ValueLayout.ADDRESS))
    }

    private val handle_mpv_terminate_destroy by lazy {
        val symbol = lookup.findOrThrow("mpv_terminate_destroy")
        linker.downcallHandle(symbol, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS))
    }

    private val handle_mpv_initialize by lazy {
        val symbol = lookup.findOrThrow("mpv_initialize")
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)
        )
    }

    private val handle_mpv_set_wakeup_callback by lazy {
        val symbol = lookup.findOrThrow("mpv_set_wakeup_callback")
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.ofVoid(
                ValueLayout.ADDRESS, // mpv_handle*
                ValueLayout.ADDRESS, // callback
                ValueLayout.ADDRESS, // opaque
            )
        )
    }

    private val handle_mpv_wait_event by lazy {
        val symbol = lookup.findOrThrow("mpv_wait_event")
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                ValueLayout.ADDRESS,   // mpv_event*
                ValueLayout.ADDRESS,   // mpv_handle*
                ValueLayout.JAVA_DOUBLE, // timeout
            )
        )
    }

    private val handle_mpv_get_property_async by lazy {
        val symbol = lookup.findOrThrow("mpv_get_property_async")
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_LONG,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
            )
        )
    }

    private val handle_mpv_set_property by lazy {
        val symbol = lookup.findOrThrow("mpv_set_property")
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_get_property by lazy {
        val symbol = lookup.findOrThrow("mpv_get_property")
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_set_property_async by lazy {
        val symbol = lookup.findOrThrow("mpv_set_property_async")
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_LONG,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_command_async by lazy {
        val symbol = lookup.findOrThrow("mpv_command_async")
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_LONG,
                ValueLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_free_node_contents by lazy {
        val symbol = lookup.findOrThrow("mpv_free_node_contents")
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.ofVoid(
                ValueLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_request_log_messages by lazy {
        val symbol = lookup.findOrThrow("mpv_request_log_messages")
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_render_context_create by lazy {
        val symbol = lookup.findOrThrow("mpv_render_context_create")
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_render_context_free by lazy {
        val symbol = lookup.findOrThrow("mpv_render_context_free")
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.ofVoid(
                ValueLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_render_context_set_update_callback by lazy {
        val symbol = lookup.findOrThrow("mpv_render_context_set_update_callback")
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.ofVoid(
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_render_context_update by lazy {
        val symbol = lookup.findOrThrow("mpv_render_context_update")
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                ValueLayout.JAVA_LONG,
                ValueLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_render_context_report_swap by lazy {
        val symbol = lookup.findOrThrow("mpv_render_context_report_swap")
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.ofVoid(
                ValueLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_render_context_render by lazy {
        val symbol = lookup.findOrThrow("mpv_render_context_render")
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS,
            )
        )
    }

    interface WakeupCallback : () -> Unit

    interface RenderUpdateCallback : () -> Unit

    private class CallbackWrapper(val callback: WakeupCallback) {
        fun invoke(@Suppress("unused") unused: MemorySegment) = callback.invoke()
    }

    private class UpdateCallbackWrapper(val callback: RenderUpdateCallback) {
        fun invoke(@Suppress("unused") unused: MemorySegment) = callback.invoke()
    }

    fun mpv_create(): Handle = Handle(handle_mpv_create() as MemorySegment)
    fun mpv_terminate_destroy(handle: Handle) {
        handle_mpv_terminate_destroy(handle.pointer)
    }

    fun mpv_initialize(handle: Handle): Error =
        Error.fromValue(handle_mpv_initialize(handle.pointer) as Int)

    fun mpv_set_wakeup_callback(handle: Handle, callback: WakeupCallback) {
        val wrapper = CallbackWrapper(callback)
        val method = MethodHandles.lookup().unreflect(wrapper::invoke.javaMethod!!).bindTo(wrapper)
        val upcall =
            linker.upcallStub(method, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS), arena)
        handle_mpv_set_wakeup_callback(handle.pointer, upcall, MemorySegment.NULL)
    }

    fun mpv_wait_event(handle: Handle, timeout: Double): Event {
        val rawEvent = handle_mpv_wait_event(handle.pointer, timeout)
            .let { it as MemorySegment }
        return Event.parse(rawEvent)
    }

    fun mpv_get_property_async(handle: Handle, userData: ULong, property: String): Error =
        Arena.ofConfined().use { arena ->
            val propertyStr = arena.allocateFrom(property)
            val ret = handle_mpv_get_property_async(
                handle.pointer,
                userData.toLong(),
                propertyStr,
                Format.Node.value
            )
            return Error.fromValue(ret as Int)
        }

    fun mpv_set_property_async(handle: Handle, userData: ULong, name: String, data: Node): Error =
        Arena.ofConfined().use { arena ->
            val name = arena.allocateFrom(name)
            val rawNode = data.into(arena)
            val ret = handle_mpv_set_property_async(
                handle.pointer,
                userData.toLong(),
                name,
                Format.Node.value,
                rawNode
            )
            return Error.fromValue(ret as Int)
        }

    fun mpv_set_property(handle: Handle, name: String, data: Node): Error =
        Arena.ofConfined().use { arena ->
            val name = arena.allocateFrom(name)
            val rawNode = data.into(arena)
            val ret = handle_mpv_set_property(handle.pointer, name, Format.Node.value, rawNode)
            return Error.fromValue(ret as Int)
        }

    fun mpv_get_property(handle: Handle, name: String): Result<Node> =
        Arena.ofConfined().use { arena ->
            val name = arena.allocateFrom(name)
            val rawNode = arena.allocate(Node.layout)
            val ret = handle_mpv_get_property(handle.pointer, name, Format.Node.value, rawNode)
            val result = Result.mpv(Error.fromValue(ret as Int)) {
                Node.parse(rawNode).also {
                    mpv_free_node_contents(rawNode)
                }
            }
            return result
        }

    fun mpv_command_async(handle: Handle, userData: ULong, command: List<String>): Error =
        Arena.ofConfined().use { arena ->
            val commandArray = arena.allocate(ValueLayout.ADDRESS, (command.size + 1).toLong())
            command.forEachIndexed { idx, cmd ->
                commandArray.setAtIndex(ValueLayout.ADDRESS, idx.toLong(), arena.allocateFrom(cmd))
            }
            commandArray.setAtIndex(ValueLayout.ADDRESS, command.size.toLong(), MemorySegment.NULL)
            val ret = handle_mpv_command_async(handle.pointer, userData.toLong(), commandArray)
            return Error.fromValue(ret as Int)
        }

    private fun mpv_free_node_contents(node: MemorySegment) {
        handle_mpv_free_node_contents(node)
    }

    fun mpv_request_log_messages(handle: Handle, minLevel: LogMessage.Level?): Error =
        Arena.ofConfined().use { arena ->
            val levelString = arena.allocateFrom(minLevel?.string ?: "terminal-default")
            val ret = handle_mpv_request_log_messages(handle.pointer, levelString)
            return Error.fromValue(ret as Int)
        }

    fun mpv_render_context_create(
        handle: Handle,
        params: List<RenderParam<*>>
    ): Result<RenderContext> =
        Arena.ofConfined().use { arena ->
            val paramsArray = params.into(arena)
            val output = arena.allocate(ValueLayout.ADDRESS)
            val ret = handle_mpv_render_context_create(output, handle.pointer, paramsArray)
            Result.mpv(Error.fromValue(ret as Int)) {
                RenderContext(output.get(ValueLayout.ADDRESS, 0))
            }
        }

    fun mpv_render_context_free(context: RenderContext) {
        handle_mpv_render_context_free(context.pointer)
    }

    fun mpv_render_context_set_update_callback(
        context: RenderContext,
        callback: RenderUpdateCallback
    ) {
        val wrapper = UpdateCallbackWrapper(callback)
        val method = MethodHandles.lookup().unreflect(wrapper::invoke.javaMethod!!).bindTo(wrapper)
        val upcall =
            linker.upcallStub(method, FunctionDescriptor.ofVoid(ValueLayout.ADDRESS), arena)
        handle_mpv_render_context_set_update_callback(context.pointer, upcall, MemorySegment.NULL)
    }

    fun mpv_render_context_update(context: RenderContext): ULong {
        return (handle_mpv_render_context_update(context.pointer) as Long).toULong()
    }

    fun mpv_render_context_report_swap(context: RenderContext) {
        handle_mpv_render_context_report_swap(context.pointer)
    }

    fun mpv_render_context_render(context: RenderContext, params: List<RenderParam<*>>) =
        Arena.ofConfined().use { arena ->
            val paramsArray = params.into(arena)
            val ret = handle_mpv_render_context_render(context.pointer, paramsArray)
            Result.mpv(Error.fromValue(ret as Int))
        }

    private fun List<RenderParam<*>>.into(arena: Arena): MemorySegment {
        val paramsArray = arena.allocate(RenderParam.layout, size.toLong() + 1)
        forEachIndexed { idx, param ->
            val target =
                paramsArray.asSlice(RenderParam.layout.byteSize * idx, RenderParam.layout.byteSize)
            target.copyFrom(param.into(arena))
        }
        return paramsArray
    }

    companion object {
        init {
            loadMpvLib()
        }
    }
}

internal expect fun loadMpvLib()
