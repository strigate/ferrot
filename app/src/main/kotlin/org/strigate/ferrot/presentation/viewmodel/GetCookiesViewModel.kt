package org.strigate.ferrot.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.strigate.ferrot.R
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.domain.usecase.CookieSetUseCase
import org.strigate.ferrot.presentation.event.GetCookiesEvent
import javax.inject.Inject

@HiltViewModel
class GetCookiesViewModel @Inject constructor(
    private val analyticsLogger: AnalyticsLogger,
    private val cookieSetUseCase: CookieSetUseCase,
) : ViewModel() {
    private val _event = MutableSharedFlow<GetCookiesEvent>()
    val event = _event.asSharedFlow()

    fun logShown() = analyticsLogger.logScreen(AnalyticsEvents.Screens.GET_COOKIES)

    fun saveCookies(
        url: String,
        cookieHeader: String,
        confirmOverwrite: Boolean = true,
    ) {
        viewModelScope.launch {
            val saved = runCatching {
                if (confirmOverwrite) {
                    val existingDomain =
                        cookieSetUseCase.getExistingCookieSetDomainForWebViewUrlUseCase(url)
                    if (existingDomain != null) {
                        _event.emit(
                            GetCookiesEvent.ShowOverwriteConfirmation(
                                url = url,
                                domain = existingDomain,
                            )
                        )
                        return@launch
                    }
                }
                cookieSetUseCase.createCookieSetFromWebViewUseCase(
                    url = url,
                    rawCookieHeader = cookieHeader,
                ) != null
            }.getOrDefault(false)
            if (saved) {
                _event.emit(GetCookiesEvent.ShowToast(R.string.toast_cookie_set_saved))
                _event.emit(GetCookiesEvent.Saved)
            } else {
                _event.emit(GetCookiesEvent.ShowToast(R.string.toast_cookie_set_failed))
            }
        }
    }
}
