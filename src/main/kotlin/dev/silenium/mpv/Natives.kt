package dev.silenium.mpv

//import dev.silenium.libs.jni.NativeLoader
import dev.silenium.libs.jni.Platform
//import dev.silenium.mpv.build.BuildConstants
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString


object Natives {
    private var loaded = false

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

    @Synchronized
    fun ensureLoaded() {
        if (!loaded) {
//            val deps = platformDeps[NativeLoader.nativePlatform.os]
//                ?: error("Unsupported platform: ${NativeLoader.nativePlatform}")
//            deps.forEach {
//                NativeLoader.loadLibraryFromClasspath(it).getOrThrow()
//            }
//            NativeLoader.loadLibraryFromClasspath(BuildConstants.LIBRARY_NAME).getOrThrow()
            System.load("/nix/store/q2ca1157v5641ll2ghq926yq83sqvfkl-mpv-0.41.0/lib/libmpv.so")
            System.load(Path("natives/target/debug/libmpv_jni_rs.so").absolutePathString())
            loaded = true
        }
    }
}
