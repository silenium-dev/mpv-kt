package dev.silenium.compose.mpv.examples.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.silenium.libs.mpv.compose.examples.shared.App
import java.nio.file.Files

fun main() = application {
    val cacheDir = Files.createTempDirectory("mpv-compose-example-desktop-cache-").toFile()
        .also { it.deleteOnExit() }
    val testVideo = javaClass.classLoader.getResourceAsStream("test.webm")!!.use { input ->
        cacheDir.resolve("test.webm").apply {
            outputStream().use(input::copyTo)
        }
    }

    Window(onCloseRequest = ::exitApplication) {
        App(testVideo)
    }
}
