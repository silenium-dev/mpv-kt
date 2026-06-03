package dev.silenium.mpv.native_bindings

import dev.silenium.libs.jni.NativeLoader
import dev.silenium.libs.jni.Platform

private val platformDeps = mapOf(
    Platform.OS.LINUX to listOf(
        "avutil",
        "swresample",
        "swscale",
        "avcodec",
        "avformat",
        "avfilter",
        "avdevice",
        "mpv",
    ),
    Platform.OS.WINDOWS to listOf(
        "mpv",
    ),
)

@Synchronized
internal actual fun loadMpvLib() {
    val libs = platformDeps[NativeLoader.nativePlatform.os]
        ?: error("Unsupported platform: ${NativeLoader.nativePlatform}")
    libs.forEach {
        NativeLoader.loadLibraryFromClasspath(it).getOrThrow()
    }
}
