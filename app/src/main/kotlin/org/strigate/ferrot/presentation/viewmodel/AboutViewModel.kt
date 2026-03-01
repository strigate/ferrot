package org.strigate.ferrot.presentation.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.presentation.event.AboutNavigationEvent
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val analyticsLogger: AnalyticsLogger,
) : ViewModel() {
    private val _navigationEvent = MutableStateFlow<AboutNavigationEvent?>(null)
    val navigationEvent: StateFlow<AboutNavigationEvent?> = _navigationEvent

    fun logShown() = analyticsLogger.logScreen(AnalyticsEvents.Screens.ABOUT)

    fun onUrlClicked(url: String) {
        _navigationEvent.value = AboutNavigationEvent.OpenUrl(url)
    }

    fun onBuildClicked() {
        _navigationEvent.value = AboutNavigationEvent.OpenAppInfo
    }

    fun onNavigationEventConsumed() {
        _navigationEvent.value = null
    }
}
