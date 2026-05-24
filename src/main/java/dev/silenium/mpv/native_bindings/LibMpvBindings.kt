@file:Suppress("FunctionName", "PrivatePropertyName", "PropertyName")

package dev.silenium.mpv.native_bindings

import dev.silenium.mpv.native_bindings.api.parse
import dev.silenium.mpv.native_bindings.event.Event
import dev.silenium.mpv.native_bindings.node.Format
import dev.silenium.mpv.native_bindings.node.Node
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

    interface WakeupCallback : () -> Unit {
    }

    private class CallbackWrapper(val callback: WakeupCallback) {
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

    companion object {
        private const val LIBMPV_PATH: String = "/nix/store/q2ca1157v5641ll2ghq926yq83sqvfkl-mpv-0.41.0/lib/libmpv.so"
    }
}
