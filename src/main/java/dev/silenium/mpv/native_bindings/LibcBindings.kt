@file:Suppress("FunctionName", "PrivatePropertyName", "PropertyName")

package dev.silenium.mpv.native_bindings

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle

class LibcBindings {
    private val linker: Linker = Linker.nativeLinker()
    private val lookup: SymbolLookup = linker.defaultLookup()

    private val handle_setlocale: MethodHandle by lazy {
        val symbol = lookup.find("setlocale").orElseThrow()
        linker.downcallHandle(
            symbol,
            FunctionDescriptor.of(
                ValueLayout.ADDRESS,   // return: char* (previous locale string)
                ValueLayout.JAVA_INT,  // category: int
                ValueLayout.ADDRESS,   // locale: const char*
            )
        )
    }

    fun setlocale(category: Int, locale: String): String? = Arena.ofConfined().use { arena ->
        val localeStr = arena.allocateFrom(locale)
        val previous = handle_setlocale.invoke(category, localeStr) as MemorySegment
        if (previous == MemorySegment.NULL) return null
        return previous.reinterpret(Long.MAX_VALUE).getString(0)
    }

    val LC_NUMERIC: Int by lazy {
        val os = System.getProperty("os.name").lowercase()
        when {
            os.contains("win") -> 2
            else -> 1  // Linux, macOS, *BSD, etc.
        }
    }
}
