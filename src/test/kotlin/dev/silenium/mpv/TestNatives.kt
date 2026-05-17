package dev.silenium.mpv

import dev.silenium.libs.jni.NativeLoader
import kotlin.io.path.absolutePathString

object TestNatives {
    fun ensureLoaded(): Unit = synchronized(Natives) {
        if (Natives.loaded) return
        System.load(NativeLoader.extractFileFromClasspath("lib/libmpv_jni_rs.so").getOrThrow().absolutePathString())
        System.load("/nix/store/q2ca1157v5641ll2ghq926yq83sqvfkl-mpv-0.41.0/lib/libmpv.so")
        Natives.loaded = true
    }
}
