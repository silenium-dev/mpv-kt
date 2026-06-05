package dev.silenium.libs.mpv.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoadingTest {
    @Test
    fun testLoading() {
        System.loadLibrary("avutil")
        System.loadLibrary("avformat")
        System.loadLibrary("avcodec")
        System.loadLibrary("avdevice")
        System.loadLibrary("swscale")
        System.loadLibrary("swresample")
        System.loadLibrary("mpv")
        println("Loaded!")
    }
}
