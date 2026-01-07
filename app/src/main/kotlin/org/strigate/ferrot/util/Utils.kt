package org.strigate.ferrot.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner

fun isAppInForeground(): Boolean {
    return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
}
