package dev.silenium.mpv.native_bindings.event

import dev.silenium.mpv.native_bindings.api.InstantiableLayout
import dev.silenium.mpv.native_bindings.api.get
import dev.silenium.mpv.native_bindings.api.NativeStructLayout
import java.lang.foreign.MemorySegment

data class LogMessage(val prefix: String, val level: String, val text: String, val logLevel: UInt): EventData {
    constructor(struct: MemorySegment) : this(
        struct[Layout.prefix],
        struct[Layout.level],
        struct[Layout.text],
        struct[Layout.logLevel],
    )

    enum class Level(val value: Int) {
        NONE(0),
        FATAL(10),
        ERROR(20),
        WARN(30),
        INFO(40),
        V(50),
        DEBUG(60),
        TRACE(70),
    }

    companion object Layout : NativeStructLayout(), InstantiableLayout<LogMessage> {
        val prefix = string("prefix")
        val level = string("level")
        val text = string("text")
        val logLevel = uint("log_level")

        override fun from(segment: MemorySegment): LogMessage = LogMessage(segment)
    }
}
