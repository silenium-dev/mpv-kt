package dev.silenium.mpv.native_bindings.event

import dev.silenium.mpv.native_bindings.Error
import dev.silenium.mpv.native_bindings.api.*
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment

sealed interface EventData

data class Event(val eventId: Id, val error: Error, val replyUserdata: ULong, private val data: EventData? = null) {
    constructor(struct: MemorySegment) : this(
        struct[Layout.eventId],
        struct[Layout.error],
        struct[Layout.replyUserData],
        when (struct[Layout.eventId]) {
            Id.CLIENT_MESSAGE -> ClientMessage.parse(struct[Layout.data])
            Id.COMMAND_REPLY -> CommandReply.parse(struct[Layout.data])
            Id.END_FILE -> EndFile.parse(struct[Layout.data])
            Id.GET_PROPERTY_REPLY,
            Id.PROPERTY_CHANGE -> EventProperty.parse(struct[Layout.data])

            Id.HOOK -> Hook.parse(struct[Layout.data])
            Id.LOG_MESSAGE -> LogMessage.parse(struct[Layout.data])
            Id.START_FILE -> StartFile.parse(struct[Layout.data])
            else -> null
        },
    )

    enum class Id(override val value: Int) : NativeEnum<Id> {
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

    companion object Layout : NativeStructLayout(), InstantiableLayout<Event> {
        val eventId = enum<Id>("event_id")
        val error = enum<Error>("error")
        val replyUserData = ulong("reply_userdata")
        val data = pointer("data")

        override fun from(segment: MemorySegment): Event = Event(segment)
    }
}
