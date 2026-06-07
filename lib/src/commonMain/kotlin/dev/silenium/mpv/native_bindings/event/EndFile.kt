package dev.silenium.mpv.native_bindings.event

import dev.silenium.mpv.native_bindings.Error
import dev.silenium.mpv.native_bindings.api.InstantiableLayout
import dev.silenium.mpv.native_bindings.api.NativeEnum
import dev.silenium.mpv.native_bindings.api.NativeStructLayout
import dev.silenium.mpv.native_bindings.api.get
import dev.silenium.libs.foreign.MemorySegment

data class EndFile(
    val reason: Reason,
    val error: Error,
    val playlistEntryId: Long,
    val playlistInsertId: Long,
    val playlistInsertNumEntries: Long
): EventData {
    constructor(struct: MemorySegment) : this(
        struct[Layout.reason],
        struct[Layout.error],
        struct[Layout.playlistEntryId],
        struct[Layout.playlistInsertId],
        struct[Layout.playlistInsertNumEntries],
    )

    enum class Reason(override val value: Int) : NativeEnum<Reason> {
        EOF(0),
        STOP(2),
        QUIT(3),
        ERROR(4),
        REDIRECT(5),
    }

    companion object Layout : NativeStructLayout(), InstantiableLayout<EndFile> {
        val reason = enum<Reason>("reason")
        val error = enum<Error>("error")
        val playlistEntryId = long("playlist_entry_id")
        val playlistInsertId = long("playlist_insert_id")
        val playlistInsertNumEntries = long("playlist_insert_num_entries")

        override fun from(segment: MemorySegment): EndFile = EndFile(segment)
    }
}
