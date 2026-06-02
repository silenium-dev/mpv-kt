package dev.silenium.mpv.core

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

abstract class DispatcherCompanion(val name: String) {
    private val loopGroup = ThreadGroup(name)
    private val loopIndex = atomic(0UL)
    private fun loopDispatcher(): ExecutorCoroutineDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(loopGroup, r, "$name-${loopIndex.updateAndGet { it + 1u }}")
    }.asCoroutineDispatcher()

    protected fun dispatch(eventLoop: suspend CoroutineScope.() -> Unit) = loopDispatcher().let {
        it to CoroutineScope(it).launch(block = eventLoop)
    }
}
