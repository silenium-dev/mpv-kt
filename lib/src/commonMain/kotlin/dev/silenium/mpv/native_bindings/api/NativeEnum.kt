package dev.silenium.mpv.native_bindings.api

interface NativeEnum<E : Enum<E>> {
    val value: Int
}
