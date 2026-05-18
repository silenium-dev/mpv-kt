@file:Suppress("FunctionName", "PrivatePropertyName", "PropertyName")

package dev.silenium.mpv.types

import java.lang.foreign.*
import java.lang.foreign.MemoryLayout.PathElement.groupElement
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import kotlin.reflect.jvm.javaMethod
import kotlin.system.exitProcess

class LibcBindings(val arena: Arena) {
    val linker: Linker = Linker.nativeLinker()
    val lookup: SymbolLookup = linker.defaultLookup()
    val handle_setlocale: MethodHandle by lazy {
        val symbol = lookup.find("setlocale").orElseThrow()
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                ValueLayout.ADDRESS,   // return: char* (previous locale string)
                ValueLayout.JAVA_INT,  // category: int
                ValueLayout.ADDRESS,   // locale: const char*
            )
        )
    }

    fun setlocale(category: Int, locale: String): String? {
        val localeStr = arena.allocateFrom(locale)
        val previous = handle_setlocale.invoke(category, localeStr) as MemorySegment
        if (previous == MemorySegment.NULL) return null
        return previous.reinterpret(Long.MAX_VALUE).getString(0)
    }

    val LC_NUMERIC: Int by lazy {
        val os = System.getProperty("os.name").lowercase()
        when {
            os.contains("win") -> 2
            else -> 1  // Linux, macOS, *BSD, etc.
        }
    }
}

@JvmInline
value class MpvHandle(internal val pointer: MemorySegment) {
    override fun toString(): String = "MpvHandle(0x%016x)".format(pointer.address())
}

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

    data class MpvEvent(val eventId: Id, val error: Int, val replyUserdata: ULong, val data: MemorySegment) {
        enum class Id(val value: Int) {
            NONE(0),
            SHUTDOWN(1),
            LOG_MESSAGE(2),
            GET_PROPERTY_REPLY(3),
            SET_PROPERTY_REPLY(4),
            COMMAND_REPLY(5),
            START_FILE(6),
            END_FILE(7),
            FILE_LOADED(8),
            IDLE(11),
            TICK(14),
            CLIENT_MESSAGE(16),
            VIDEO_RECONFIG(17),
            AUDIO_RECONFIG(18),
            SEEK(20),
            PLAYBACK_RESTART(21),
            PROPERTY_CHANGE(22),
            QUEUE_OVERFLOW(24),
            HOOK(25);

            companion object {
                private val valueMap = entries.associateBy(Id::value)

                fun fromValue(value: Int): Id = valueMap[value] ?: error("Unknown event ID: $value")
            }
        }

        override fun toString(): String {
            return "MpvEvent(eventId=$eventId, error=$error, replyUserdata=$replyUserdata)"
        }
    }

    private class CallbackWrapper(val callback: WakeupCallback) {
        fun invoke(unused: MemorySegment) = callback.invoke()
    }

    fun mpv_create(): MpvHandle = MpvHandle(handle_mpv_create() as MemorySegment)
    fun mpv_terminate_destroy(handle: MpvHandle) = handle_mpv_terminate_destroy(handle.pointer) as Unit
    fun mpv_initialize(handle: MpvHandle): Int = handle_mpv_initialize(handle.pointer) as Int

    fun mpv_set_wakeup_callback(handle: MpvHandle, callback: WakeupCallback) {
        val wrapper = CallbackWrapper(callback)
        val method = MethodHandles.lookup().unreflect(wrapper::invoke.javaMethod!!).bindTo(wrapper)
        val upcall = linker.upcallStub(method, FunctionDescriptor.ofVoid(AddressLayout.ADDRESS), arena)
        handle_mpv_set_wakeup_callback(handle.pointer, upcall, MemorySegment.NULL)
    }

    fun mpv_wait_event(handle: MpvHandle, timeout: Double): MpvEvent {
        val rawEvent = handle_mpv_wait_event(handle.pointer, timeout)
            .let { it as MemorySegment }
            .reinterpret(MPV_EVENT_LAYOUT.byteSize())
        val eventId: Int = MPV_EVENT_LAYOUT.varHandle(groupElement("event_id")).get(rawEvent, 0L) as Int
        val error: Int = MPV_EVENT_LAYOUT.varHandle(groupElement("error")).get(rawEvent, 0L) as Int
        val replyUserdata: ULong = MPV_EVENT_LAYOUT.varHandle(groupElement("reply_userdata")).get(rawEvent, 0L)
            .let { (it as Long).toULong() }
        val dataPtr: MemorySegment = MPV_EVENT_LAYOUT.varHandle(groupElement("data")).get(rawEvent, 0L) as MemorySegment
        return MpvEvent(MpvEvent.Id.fromValue(eventId), error, replyUserdata, dataPtr)
    }

    companion object {
        private const val LIBMPV_PATH: String = "/nix/store/q2ca1157v5641ll2ghq926yq83sqvfkl-mpv-0.41.0/lib/libmpv.so"

        private val MPV_NODE_LAYOUT = MemoryLayout.structLayout(
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
        private val MPV_NODE_LIST_LAYOUT = MemoryLayout.structLayout(
            /* num: int */ ValueLayout.JAVA_INT.withName("num"),
            /* values: mpv_node* */ ValueLayout.ADDRESS.withName("values"),
            /* keys: char** */ ValueLayout.ADDRESS.withName("keys"),
        )
        private val MPV_BYTE_ARRAY_LAYOUT = MemoryLayout.structLayout(
            /* data: void* */ ValueLayout.ADDRESS.withName("data"),
            /* size: size_t */ ValueLayout.JAVA_LONG.withName("size"),
        )

        private val MPV_EVENT_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("event_id"),  // offset 0,  4 bytes
            ValueLayout.JAVA_INT.withName("error"),                 // offset 4,  4 bytes
            ValueLayout.JAVA_LONG.withName("reply_userdata"),       // offset 8,  8 bytes
            ValueLayout.ADDRESS.withName("data"),                   // offset 16, 8 bytes (on 64-bit)
        )                                                                   // total: 24 bytes

        private val MPV_EVENT_PROPERTY_LAYOUT = MemoryLayout.structLayout(
            /* name: char* */ ValueLayout.ADDRESS.withName("name"),
            /* format: mpv_format */ValueLayout.JAVA_INT.withName("format"),
            MemoryLayout.paddingLayout(4),
            /* value: void* */ ValueLayout.ADDRESS.withName("value"),
        )
        private val MPV_EVENT_LOG_MESSAGE_LAYOUT = MemoryLayout.structLayout(
            /* prefix: char* */ ValueLayout.ADDRESS.withName("prefix"),
            /* level: char* */ ValueLayout.ADDRESS.withName("level"),
            /* text: char* */ ValueLayout.ADDRESS.withName("text"),
            /* log_level: mpv_log_level */ValueLayout.JAVA_INT.withName("log_level"),
        )
        private val MPV_EVENT_CLIENT_MESSAGE_LAYOUT = MemoryLayout.structLayout(
            /* num_args: int */ ValueLayout.JAVA_INT.withName("num_args"),
            /* args: void** */ ValueLayout.ADDRESS.withName("args"),
        )
        private val MPV_EVENT_START_FILE_LAYOUT = MemoryLayout.structLayout(
            /* playlist_entry_id: int64_t */ValueLayout.JAVA_LONG.withName("playlist_entry_id"),
        )
        private val MPV_EVENT_END_FILE_LAYOUT = MemoryLayout.structLayout(
            /* reason: mpv_end_file_reason */ValueLayout.JAVA_INT.withName("reason"),
            /* error: int */ ValueLayout.JAVA_INT.withName("error"),
            /* playlist_entry_id: int64_t */ ValueLayout.JAVA_LONG.withName("playlist_entry_id"),
            /* playlist_insert_id: int64_t */ ValueLayout.JAVA_LONG.withName("playlist_insert_id"),
            /* playlist_insert_num_entries: int64_t */ ValueLayout.JAVA_LONG.withName("playlist_insert_num_entries"),
        )
        private val MPV_EVENT_HOOK_LAYOUT = MemoryLayout.structLayout(
            /* name: char* */ ValueLayout.ADDRESS.withName("name"),
            /* id: uint64_t */ ValueLayout.JAVA_LONG.withName("id"),
        )
        private val MPV_EVENT_COMMAND_REPLY_LAYOUT = MemoryLayout.structLayout(
            /* result: mpv_node */ MPV_NODE_LAYOUT.withName("result"),
        )
    }
}

class MyCallback : LibMpvBindings.WakeupCallback {
    override fun invoke() {
        println("Wakeup!")
    }
}

fun main() {
    Arena.ofConfined().use { arena ->
        val libc = LibcBindings(arena)
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
