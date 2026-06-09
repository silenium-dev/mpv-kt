package dev.silenium.mpv.native_bindings.event

import dev.silenium.libs.foreign.MemorySegment
import dev.silenium.mpv.native_bindings.api.InstantiableLayout
import dev.silenium.mpv.native_bindings.api.NativeStructLayout
import dev.silenium.mpv.native_bindings.api.get

data class StartFile(val playlistEntryId: Long): EventData {
    constructor(struct: MemorySegment) : this(
        struct[playlistEntryId]
    )

    companion object Layout : NativeStructLayout(), InstantiableLayout<StartFile> {
        val playlistEntryId = long("playlist_entry_id")

        override fun from(segment: MemorySegment): StartFile = StartFile(segment)
    }
}
