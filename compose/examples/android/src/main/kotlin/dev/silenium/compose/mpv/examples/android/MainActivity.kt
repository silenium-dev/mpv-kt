package dev.silenium.compose.mpv.examples.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.silenium.libs.mpv.compose.VideoSurface
import dev.silenium.mpv.Mpv
import dev.silenium.mpv.native_bindings.node.Node
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val testVideo = assets.open("test.webm").use { input ->
            cacheDir.resolve("test.webm").apply {
                outputStream().use(input::copyTo)
            }
        }
        setContent {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val mpv = remember {
                    Mpv().apply {
                        setProperty("hwdec", Node.String("mediacodec")).getOrThrow()
                        setProperty("vo", Node.String("libmpv")).getOrThrow()
                        initialize().getOrThrow()
                    }
                }
                val coroutineScope = rememberCoroutineScope()
                VideoSurface(mpv, Modifier.fillMaxSize(), onInit = {
                    coroutineScope.launch {
                        mpv.commandAsync("loadfile", testVideo.absolutePath).getOrThrow()
                    }
                })
            }
        }
    }
}
