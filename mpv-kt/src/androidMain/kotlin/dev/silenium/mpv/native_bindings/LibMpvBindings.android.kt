package dev.silenium.mpv.native_bindings

internal actual fun loadMpvLib() {
    System.loadLibrary("mpv")
}
