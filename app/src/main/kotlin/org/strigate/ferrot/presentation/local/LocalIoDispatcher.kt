package org.strigate.ferrot.presentation.local

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineDispatcher

val LocalIoDispatcher = staticCompositionLocalOf<CoroutineDispatcher> {
    error("An IO dispatcher must be provided by the activity")
}
