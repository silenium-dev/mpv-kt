package dev.silenium.mpv

import kotlin.test.Test

class NativesTest {
    @Test
    fun test() {
        TestNatives.ensureLoaded()
        Bindings.testN()
    }
}

object Bindings {
    @JvmStatic
    external fun testN()

    @JvmStatic
    @Suppress("unused") // used by native code
    fun test(s: String) = println(s)
}
