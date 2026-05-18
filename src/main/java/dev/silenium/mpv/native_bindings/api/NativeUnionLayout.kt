package dev.silenium.mpv.native_bindings.api

import java.lang.foreign.MemoryLayout

abstract class NativeUnionLayout<T> : NativeStructLayout<T>() {
    final override fun layoutCreator(entries: List<MemoryLayout>): MemoryLayout =
        MemoryLayout.unionLayout(*entries.toTypedArray())
}
