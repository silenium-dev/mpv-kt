package dev.silenium.mpv.native_bindings.api

import dev.silenium.libs.foreign.MemoryLayout

abstract class NativeUnionLayout : NativeStructLayout() {
    final override fun layoutCreator(entries: List<MemoryLayout>): MemoryLayout =
        MemoryLayout.unionLayout(entries)
}
