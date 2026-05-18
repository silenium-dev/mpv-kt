@file:Suppress("FunctionName", "PrivatePropertyName", "PropertyName")

package dev.silenium.mpv.native_bindings

import dev.silenium.mpv.native_bindings.event.Event
import java.lang.foreign.*
import java.lang.foreign.MemoryLayout.PathElement.groupElement
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

    interface WakeupCallback : () -> Unit {
    }

    private class CallbackWrapper(val callback: WakeupCallback) {
        fun invoke(unused: MemorySegment) = callback.invoke()
    }

    fun mpv_create(): Handle = Handle(handle_mpv_create() as MemorySegment)
    fun mpv_terminate_destroy(handle: Handle) = handle_mpv_terminate_destroy(handle.pointer) as Unit
    fun mpv_initialize(handle: Handle): Int = handle_mpv_initialize(handle.pointer) as Int

    fun mpv_set_wakeup_callback(handle: Handle, callback: WakeupCallback) {
        val wrapper = CallbackWrapper(callback)
        val method = MethodHandles.lookup().unreflect(wrapper::invoke.javaMethod!!).bindTo(wrapper)
        val upcall = linker.upcallStub(method, FunctionDescriptor.ofVoid(AddressLayout.ADDRESS), arena)
        handle_mpv_set_wakeup_callback(handle.pointer, upcall, MemorySegment.NULL)
    }

    fun mpv_wait_event(handle: Handle, timeout: Double): Event {
        val rawEvent = handle_mpv_wait_event(handle.pointer, timeout)
            .let { it as MemorySegment }
            .reinterpret(Definitions.MPV_EVENT_LAYOUT.byteSize())
        return Event(rawEvent)
    }

    companion object {
        private const val LIBMPV_PATH: String = "/nix/store/q2ca1157v5641ll2ghq926yq83sqvfkl-mpv-0.41.0/lib/libmpv.so"
    }

    object Definitions {
        val MPV_NODE_LAYOUT = MemoryLayout.structLayout(
            /* u: union */
            MemoryLayout.unionLayout(
                /* string: char* */ ValueLayout.ADDRESS.withName("string"),
                /* flag: int */ ValueLayout.JAVA_INT.withName("flag"),
                /* int64: int64_t */ ValueLayout.JAVA_LONG.withName("int64"),
                /* double_: double */ ValueLayout.JAVA_DOUBLE.withName("double_"),
                /* list: mpv_node_list* */ ValueLayout.ADDRESS.withName("list"),
                /* ba: mpv_byte_array* */ ValueLayout.ADDRESS.withName("ba"),
            ).withName("u"),
            /* format: mpv_format */ ValueLayout.JAVA_INT.withName("format"),
        )
        val MPV_NODE_LIST_LAYOUT = MemoryLayout.structLayout(
            /* num: int */ ValueLayout.JAVA_INT.withName("num"),
            /* values: mpv_node* */ ValueLayout.ADDRESS.withName("values"),
            /* keys: char** */ ValueLayout.ADDRESS.withName("keys"),
        )
        val MPV_BYTE_ARRAY_LAYOUT = MemoryLayout.structLayout(
            /* data: void* */ ValueLayout.ADDRESS.withName("data"),
            /* size: size_t */ ValueLayout.JAVA_LONG.withName("size"),
        )

        val MPV_EVENT_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("event_id"),  // offset 0,  4 bytes
            ValueLayout.JAVA_INT.withName("error"),                 // offset 4,  4 bytes
            ValueLayout.JAVA_LONG.withName("reply_userdata"),       // offset 8,  8 bytes
            ValueLayout.ADDRESS.withName("data"),                   // offset 16, 8 bytes (on 64-bit)
        )                                                                   // total: 24 bytes

        val MPV_EVENT_PROPERTY_LAYOUT = MemoryLayout.structLayout(
            /* name: char* */ ValueLayout.ADDRESS.withName("name"),
            /* format: mpv_format */ValueLayout.JAVA_INT.withName("format"),
            MemoryLayout.paddingLayout(4),
            /* value: void* */ ValueLayout.ADDRESS.withName("value"),
        )
        val MPV_EVENT_LOG_MESSAGE_LAYOUT = MemoryLayout.structLayout(
            /* prefix: char* */ ValueLayout.ADDRESS.withName("prefix"),
            /* level: char* */ ValueLayout.ADDRESS.withName("level"),
            /* text: char* */ ValueLayout.ADDRESS.withName("text"),
            /* log_level: mpv_log_level */ValueLayout.JAVA_INT.withName("log_level"),
        )
        val MPV_EVENT_CLIENT_MESSAGE_LAYOUT = MemoryLayout.structLayout(
            /* num_args: int */ ValueLayout.JAVA_INT.withName("num_args"),
            /* args: void** */ ValueLayout.ADDRESS.withName("args"),
        )
        val MPV_EVENT_START_FILE_LAYOUT = MemoryLayout.structLayout(
            /* playlist_entry_id: int64_t */ValueLayout.JAVA_LONG.withName("playlist_entry_id"),
        )
        val MPV_EVENT_END_FILE_LAYOUT = MemoryLayout.structLayout(
            /* reason: mpv_end_file_reason */ValueLayout.JAVA_INT.withName("reason"),
            /* error: int */ ValueLayout.JAVA_INT.withName("error"),
            /* playlist_entry_id: int64_t */ ValueLayout.JAVA_LONG.withName("playlist_entry_id"),
            /* playlist_insert_id: int64_t */ ValueLayout.JAVA_LONG.withName("playlist_insert_id"),
            /* playlist_insert_num_entries: int64_t */ ValueLayout.JAVA_LONG.withName("playlist_insert_num_entries"),
        )
        val MPV_EVENT_HOOK_LAYOUT = MemoryLayout.structLayout(
            /* name: char* */ ValueLayout.ADDRESS.withName("name"),
            /* id: uint64_t */ ValueLayout.JAVA_LONG.withName("id"),
        )
        val MPV_EVENT_COMMAND_REPLY_LAYOUT = MemoryLayout.structLayout(
            /* result: mpv_node */ MPV_NODE_LAYOUT.withName("result"),
        )
    }
}
