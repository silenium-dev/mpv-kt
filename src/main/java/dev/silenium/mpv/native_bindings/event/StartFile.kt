package dev.silenium.mpv.native_bindings.event

import dev.silenium.mpv.native_bindings.api.NativeStructLayout
import dev.silenium.mpv.native_bindings.api.get
import java.lang.foreign.MemorySegment

data class StartFile(val playlistEntryId: Long) {
    constructor(struct: MemorySegment) : this(
        struct[Layout.playlistEntryId]
    )

    companion object Layout : NativeStructLayout<StartFile>() {
        val playlistEntryId = long("playlist_entry_id")

        override fun from(segment: MemorySegment): StartFile = StartFile(segment)
    }
}
