package org.strigate.ferrot.presentation.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val analyticsLogger: AnalyticsLogger,
) : ViewModel() {
    private val _openUrl = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val openUrl: SharedFlow<String> = _openUrl

    fun logShown() = analyticsLogger.logScreen(AnalyticsEvents.Screens.ABOUT)

    fun onUrlClicked(url: String) {
        _openUrl.tryEmit(url)
    }
}
