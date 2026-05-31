@file:Suppress("FunctionName", "PrivatePropertyName", "PropertyName")

package dev.silenium.mpv.native_bindings

import dev.silenium.mpv.native_bindings.api.parse
import dev.silenium.mpv.native_bindings.event.Event
import dev.silenium.mpv.native_bindings.node.Format
import dev.silenium.mpv.native_bindings.node.Node
import dev.silenium.mpv.native_bindings.render.RenderParam
import java.lang.foreign.*
import java.lang.invoke.MethodHandles
import kotlin.reflect.jvm.javaMethod

class LibMpvBindings(val arena: Arena) {
    val lookup: SymbolLookup = SymbolLookup.libraryLookup(LIBMPV_PATH, arena)
    val linker: Linker = Linker.nativeLinker()

    private val handle_mpv_create by lazy {
        val symbol = lookup.find("mpv_create").orElseThrow()
        linker.downcallHandle(symbol, FunctionDescriptor.of(AddressLayout.ADDRESS))
    }

    private val handle_mpv_terminate_destroy by lazy {
        val symbol = lookup.find("mpv_terminate_destroy").orElseThrow()
        linker.downcallHandle(symbol, FunctionDescriptor.ofVoid(AddressLayout.ADDRESS))
    }

    private val handle_mpv_initialize by lazy {
        val symbol = lookup.find("mpv_initialize").orElseThrow()
        linker.downcallHandle(symbol, FunctionDescriptor.of(ValueLayout.JAVA_INT, AddressLayout.ADDRESS))
    }

    private val handle_mpv_set_wakeup_callback by lazy {
        val symbol = lookup.find("mpv_set_wakeup_callback").orElseThrow()
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.ofVoid(
                AddressLayout.ADDRESS, // mpv_handle*
                AddressLayout.ADDRESS, // callback
                AddressLayout.ADDRESS, // opaque
            )
        )
    }

    private val handle_mpv_wait_event by lazy {
        val symbol = lookup.find("mpv_wait_event").orElseThrow()
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                AddressLayout.ADDRESS,   // mpv_event*
                AddressLayout.ADDRESS,   // mpv_handle*
                ValueLayout.JAVA_DOUBLE, // timeout
            )
        )
    }

    private val handle_mpv_get_property_async by lazy {
        val symbol = lookup.find("mpv_get_property_async").orElseThrow()
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                AddressLayout.ADDRESS,
                AddressLayout.JAVA_LONG,
                AddressLayout.ADDRESS,
                AddressLayout.JAVA_INT,
            )
        )
    }

    private val handle_mpv_set_property by lazy {
        val symbol = lookup.find("mpv_set_property").orElseThrow()
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                AddressLayout.ADDRESS,
                AddressLayout.ADDRESS,
                AddressLayout.JAVA_INT,
                AddressLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_get_property by lazy {
        val symbol = lookup.find("mpv_get_property").orElseThrow()
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                AddressLayout.ADDRESS,
                AddressLayout.ADDRESS,
                AddressLayout.JAVA_INT,
                AddressLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_set_property_async by lazy {
        val symbol = lookup.find("mpv_set_property_async").orElseThrow()
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                AddressLayout.ADDRESS,
                ValueLayout.JAVA_LONG,
                AddressLayout.ADDRESS,
                AddressLayout.JAVA_INT,
                AddressLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_command_async by lazy {
        val symbol = lookup.find("mpv_command_async").orElseThrow()
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                AddressLayout.ADDRESS,
                ValueLayout.JAVA_LONG,
                AddressLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_free_node_contents by lazy {
        val symbol = lookup.find("mpv_free_node_contents").orElseThrow()
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.ofVoid(
                AddressLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_render_context_create by lazy {
        val symbol = lookup.find("mpv_render_context_create").orElseThrow()
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                AddressLayout.ADDRESS,
                AddressLayout.ADDRESS,
                AddressLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_render_context_free by lazy {
        val symbol = lookup.find("mpv_render_context_free").orElseThrow()
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.ofVoid(
                ValueLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_render_context_set_update_callback by lazy {
        val symbol = lookup.find("mpv_render_context_set_update_callback").orElseThrow()
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.ofVoid(
                AddressLayout.ADDRESS,
                AddressLayout.ADDRESS,
                AddressLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_render_context_update by lazy {
        val symbol = lookup.find("mpv_render_context_update").orElseThrow()
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                ValueLayout.JAVA_LONG,
                AddressLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_render_context_report_swap by lazy {
        val symbol = lookup.find("mpv_render_context_report_swap").orElseThrow()
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.ofVoid(
                AddressLayout.ADDRESS,
            )
        )
    }

    private val handle_mpv_render_context_render by lazy {
        val symbol = lookup.find("mpv_render_context_render").orElseThrow()
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                AddressLayout.ADDRESS,
                AddressLayout.ADDRESS,
            )
        )
    }

    interface WakeupCallback : () -> Unit {
    }

    interface RenderUpdateCallback : () -> Unit {
    }

    private class CallbackWrapper(val callback: WakeupCallback) {
        fun invoke(unused: MemorySegment) = callback.invoke()
    }

    private class UpdateCallbackWrapper(val callback: RenderUpdateCallback) {
        fun invoke(unused: MemorySegment) = callback.invoke()
    }

    fun mpv_create(): Handle = Handle(handle_mpv_create() as MemorySegment)
    fun mpv_terminate_destroy(handle: Handle) = handle_mpv_terminate_destroy(handle.pointer) as Unit
    fun mpv_initialize(handle: Handle): Error = Error.fromValue(handle_mpv_initialize(handle.pointer) as Int)

    fun mpv_set_wakeup_callback(handle: Handle, callback: WakeupCallback) {
        val wrapper = CallbackWrapper(callback)
        val method = MethodHandles.lookup().unreflect(wrapper::invoke.javaMethod!!).bindTo(wrapper)
        val upcall = linker.upcallStub(method, FunctionDescriptor.ofVoid(AddressLayout.ADDRESS), arena)
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
            val ret = handle_mpv_get_property_async(handle.pointer, userData.toLong(), propertyStr, Format.Node.value)
            return Error.fromValue(ret as Int)
        }

    fun mpv_set_property_async(handle: Handle, userData: ULong, name: String, data: Node): Error =
        Arena.ofConfined().use { arena ->
            val name = arena.allocateFrom(name)
            val rawNode = data.into(arena)
            val ret = handle_mpv_set_property_async(handle.pointer, userData.toLong(), name, Format.Node.value, rawNode)
            return Error.fromValue(ret as Int)
        }

    fun mpv_set_property(handle: Handle, name: String, data: Node): Error =
        Arena.ofConfined().use { arena ->
            val name = arena.allocateFrom(name)
            val rawNode = data.into(arena)
            val ret = handle_mpv_set_property(handle.pointer, name, Format.Node.value, rawNode)
            return Error.fromValue(ret as Int)
        }

    fun mpv_get_property(handle: Handle, name: String): Result<Node> = Arena.ofConfined().use { arena ->
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
            val commandArray = arena.allocate(AddressLayout.ADDRESS, (command.size + 1).toLong())
            command.forEachIndexed { idx, cmd ->
                commandArray.setAtIndex(AddressLayout.ADDRESS, idx.toLong(), arena.allocateFrom(cmd))
            }
            commandArray.setAtIndex(AddressLayout.ADDRESS, command.size.toLong(), MemorySegment.NULL)
            val ret = handle_mpv_command_async(handle.pointer, userData.toLong(), commandArray)
            return Error.fromValue(ret as Int)
        }

    private fun mpv_free_node_contents(node: MemorySegment) {
        handle_mpv_free_node_contents(node)
    }

    fun mpv_render_context_create(handle: Handle, params: List<RenderParam<*>>): Result<RenderContext> =
        Arena.ofConfined().use { arena ->
            val paramsArray = params.into(arena)
            val output = arena.allocate(AddressLayout.ADDRESS)
            val ret = handle_mpv_render_context_create(output, handle.pointer, paramsArray)
            Result.mpv(Error.fromValue(ret as Int)) {
                RenderContext(output.get(AddressLayout.ADDRESS, 0))
            }
        }

    fun mpv_render_context_free(context: RenderContext) {
        handle_mpv_render_context_free(context.pointer)
    }

    fun mpv_render_context_set_update_callback(context: RenderContext, callback: RenderUpdateCallback) {
        val wrapper = UpdateCallbackWrapper(callback)
        val method = MethodHandles.lookup().unreflect(wrapper::invoke.javaMethod!!).bindTo(wrapper)
        val upcall = linker.upcallStub(method, FunctionDescriptor.ofVoid(AddressLayout.ADDRESS), arena)
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
            val target = paramsArray.asSlice(RenderParam.layout.byteSize() * idx, RenderParam.layout.byteSize())
            target.copyFrom(param.into(arena))
        }
        return paramsArray
    }

    companion object {
        private const val LIBMPV_PATH: String = "/nix/store/5cli1434b5882638yksclkpipw09inix-mpv-0.41.0/lib/libmpv.so"
    }
}
