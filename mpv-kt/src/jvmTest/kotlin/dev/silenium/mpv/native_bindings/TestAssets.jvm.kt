package dev.silenium.mpv.native_bindings

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.outputStream

object TestAssets {
    val testVideo: Path by lazy {
        val output = Files.createTempFile("test", ".webm")
        output.outputStream().use {
            javaClass.getResourceAsStream("/test.webm")!!.copyTo(it)
        }
        output
    }
}

class JvmSmokeTest: SmokeTest(TestAssets.testVideo)
