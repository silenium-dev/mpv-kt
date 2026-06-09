package dev.silenium.mpv.native_bindings.event

import dev.silenium.libs.foreign.MemorySegment
import dev.silenium.mpv.native_bindings.api.InstantiableLayout
import dev.silenium.mpv.native_bindings.api.NativeEnum
import dev.silenium.mpv.native_bindings.api.NativeStructLayout
import dev.silenium.mpv.native_bindings.api.get
import org.slf4j.event.Level as Slf4jLevel

data class LogMessage(val prefix: String, val level: Level, val text: String) : EventData {
    constructor(struct: MemorySegment) : this(
        struct[prefix],
        struct[logLevel],
        struct[text],
    )

    enum class Level(override val value: Int, val string: String) : NativeEnum<Level> {
        NONE(0, "no"),
        FATAL(10, "fatal"),
        ERROR(20, "error"),
        WARN(30, "warn"),
        INFO(40, "info"),
        V(50, "v"),
        DEBUG(60, "debug"),
        TRACE(70, "trace");

        val asSlf4j: Slf4jLevel
            get() = when (this) {
                FATAL -> Slf4jLevel.ERROR
                ERROR -> Slf4jLevel.ERROR
                WARN -> Slf4jLevel.WARN
                INFO -> Slf4jLevel.INFO
                V -> Slf4jLevel.DEBUG
                DEBUG -> Slf4jLevel.DEBUG
                TRACE -> Slf4jLevel.TRACE
                else -> error("Unknown level $this")
            }

        companion object {
            private val map = entries.associateBy(Level::value)
            fun fromValue(value: Int) = map[value]
        }
    }

    companion object Layout : NativeStructLayout(), InstantiableLayout<LogMessage> {
        val prefix = string("prefix")
        val level = string("level")
        val text = string("text")
        val logLevel = enum<Level>("log_level")

        override fun from(segment: MemorySegment): LogMessage = LogMessage(segment)
    }
}
