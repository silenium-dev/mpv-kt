package dev.silenium.libs.mpv.android

import com.v7878.foreign.SymbolLookup
import io.kotest.runner.junit4.FunSpec

class LoadingTest : FunSpec({
    test("loads library successfully") {
        System.loadLibrary("mpv")
        val lookup = SymbolLookup.loaderLookup()
        val mpvCreate = lookup.findOrThrow("mpv_create")

        println("Loaded: ${mpvCreate.address().toHexString()}!")
    }
})
