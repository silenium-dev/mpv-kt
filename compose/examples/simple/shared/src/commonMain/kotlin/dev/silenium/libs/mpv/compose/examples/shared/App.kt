package dev.silenium.libs.mpv.compose.examples.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.silenium.libs.mpv.compose.VideoSurface
import dev.silenium.mpv.Mpv
import dev.silenium.mpv.native_bindings.node.Node
import dev.silenium.mpv.native_bindings.render.RenderParam
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun App(testVideo: File) =
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val mpv = remember {
            Mpv().apply {
                setProperty("hwdec", Node.String("auto")).getOrThrow()
                setProperty("vo", Node.String("libmpv")).getOrThrow()
                initialize().getOrThrow()
            }
        }
        val coroutineScope = rememberCoroutineScope()
        VideoSurface(
            mpv,
            Modifier.fillMaxSize(),
            additionalRenderParams = listOf(
                RenderParam.BlockForTargetTime(false)
            ),
            onInit = {
                coroutineScope.launch {
                    mpv.commandAsync("loadfile", testVideo.absolutePath).getOrThrow()
                    mpv.setPropertyAsync("loop-file", Node.String("inf")).getOrThrow()
                }
            },
        )
        Surface(
            color = Color.White.copy(alpha = 0.8f),
            shape = MaterialTheme.shapes.small,
        ) {
            Column {
                Text(
                    text = "Composable over Video Surface",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(8.dp),
                )
                Button(onClick = {
                    coroutineScope.launch {
                        mpv.commandAsync("cycle", "pause").getOrThrow()
                    }
                }) {
                    Text("Toggle Play/Pause")
                }
            }
        }
    }
