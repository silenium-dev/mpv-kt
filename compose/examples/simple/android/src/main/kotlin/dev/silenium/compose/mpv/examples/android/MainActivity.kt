package dev.silenium.compose.mpv.examples.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.silenium.compose.mpv.examples.shared.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val testVideo = assets.open("test.webm").use { input ->
            cacheDir.resolve("test.webm").apply {
                outputStream().use(input::copyTo)
            }
        }
        setContent {
            App(testVideo)
        }
    }
}
