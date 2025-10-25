package org.strigate.ferrot.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun LifecycleEffect(block: LifecycleObserverScope.() -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = remember { LifecycleObserverScope() }.apply(block)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            scope.callbacks[event]?.invoke()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

class LifecycleObserverScope {
    internal val callbacks = mutableMapOf<Lifecycle.Event, () -> Unit>()

    fun on(event: Lifecycle.Event, block: () -> Unit) {
        callbacks[event] = block
    }
}
