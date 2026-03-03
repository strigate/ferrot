package org.strigate.ferrot.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.presentation.event.AboutEvent
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val analyticsLogger: AnalyticsLogger,
) : ViewModel() {
    private val _event = MutableSharedFlow<AboutEvent>()
    val event = _event.asSharedFlow()

    fun logShown() = analyticsLogger.logScreen(AnalyticsEvents.Screens.ABOUT)

    fun onUrlClicked(url: String) {
        viewModelScope.launch {
            _event.emit(AboutEvent.OpenUrl(url))
        }
    }

    fun onBuildClicked() {
        viewModelScope.launch {
            _event.emit(AboutEvent.OpenAppInfo)
        }
    }
}
