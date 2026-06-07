package dev.silenium.libs.mpv.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.v7878.foreign.SymbolLookup
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoadingTest {
    @Test
    fun testLoading() {
        System.loadLibrary("mpv")
        val lookup = SymbolLookup.loaderLookup()
        val mpvCreate = lookup.findOrThrow("mpv_create")

        println("Loaded: ${mpvCreate.address().toHexString()}!")
    }
}
