package dev.silenium.compose.mpv.examples.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.silenium.libs.mpv.compose.VideoSurface
import dev.silenium.mpv.Mpv
import dev.silenium.mpv.native_bindings.node.Node
import kotlinx.coroutines.launch
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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val mpv = remember {
                Mpv().apply {
                    setProperty("hwdec", Node.String("vaapi-copy")).getOrThrow()
                    setProperty("vo", Node.String("libmpv")).getOrThrow()
                    initialize().getOrThrow()
                }
            }
            val coroutineScope = rememberCoroutineScope()
            VideoSurface(mpv, Modifier.fillMaxSize(), onInit = {
                coroutineScope.launch {
                    mpv.commandAsync("loadfile", testVideo.absolutePath).getOrThrow()
                    mpv.setPropertyAsync("loop-file", Node.String("inf")).getOrThrow()
                }
            })
            Surface(
                color = Color.White.copy(alpha = 0.8f),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = "Composable over Video Surface",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}
