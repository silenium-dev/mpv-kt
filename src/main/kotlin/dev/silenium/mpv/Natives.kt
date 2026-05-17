package dev.silenium.mpv

import dev.silenium.libs.jni.NativeLoader
import dev.silenium.libs.jni.Platform
import dev.silenium.mpv.build.BuildConstants


object Natives {
    internal var loaded = false

    private val platformDeps = mapOf(
        Platform.OS.LINUX to listOf(
            "avutil",
            "swresample",
            "swscale",
            "avcodec",
            "avformat",
            "avfilter",
            "avdevice",
        ),
        Platform.OS.WINDOWS to listOf(
            "libmpv-2",
        ),
    )

    fun ensureLoaded() = synchronized(this) {
        if (!loaded) {
            val deps = platformDeps[NativeLoader.nativePlatform.os]
                ?: error("Unsupported platform: ${NativeLoader.nativePlatform}")
            deps.forEach {
                NativeLoader.loadLibraryFromClasspath(it).getOrThrow()
            }
            NativeLoader.loadLibraryFromClasspath(BuildConstants.LIBRARY_NAME).getOrThrow()
            loaded = true
        }
    }
}
