package org.strigate.ferrot.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.strigate.ferrot.R
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.domain.model.CookieSetWithDomains
import org.strigate.ferrot.domain.usecase.CookieSetUseCase
import org.strigate.ferrot.presentation.event.CookiesEvent
import org.strigate.ferrot.presentation.mapper.toUiData
import org.strigate.ferrot.presentation.state.CookiesUiState
import javax.inject.Inject

@HiltViewModel
class CookiesViewModel @Inject constructor(
    private val analyticsLogger: AnalyticsLogger,
    private val cookieSetUseCase: CookieSetUseCase,
) : ViewModel() {
    private val _event = MutableSharedFlow<CookiesEvent>()
    val event = _event.asSharedFlow()

    val uiState: StateFlow<CookiesUiState> = getUiState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CookiesUiState.Loading,
    )

    private fun getUiState(): Flow<CookiesUiState> {
        return cookieSetUseCase
            .getCookieSetsWithDomainsAsFlowUseCase()
            .map<List<CookieSetWithDomains>, CookiesUiState> { cookieSets ->
                CookiesUiState.Data(
                    cookieSets = cookieSets.map { it.toUiData() },
                )
            }
            .catch { emit(CookiesUiState.Error) }
    }

    fun logShown() = analyticsLogger.logScreen(AnalyticsEvents.Screens.COOKIES)

    fun importCookieFile(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                cookieSetUseCase.createCookieSetFromFileUseCase(
                    name = "",
                    uri = uri,
                    rawDomains = "",
                    includeSubdomains = true,
                )
            }.onSuccess {
                _event.emit(CookiesEvent.ShowToast(R.string.toast_cookie_set_saved))
            }.onFailure {
                _event.emit(CookiesEvent.ShowToast(R.string.toast_cookie_set_failed))
            }
        }
    }

    fun deleteCookieSet(cookieSetId: Long) {
        viewModelScope.launch {
            cookieSetUseCase.deleteCookieSetUseCase(cookieSetId)
            _event.emit(CookiesEvent.ShowToast(R.string.toast_cookie_set_deleted))
        }
    }

    fun showCookieText(cookieSetId: Long, title: String) {
        viewModelScope.launch {
            val text = cookieSetUseCase.getCookieSetCookiesTextUseCase(cookieSetId)
            if (text.isNullOrBlank()) {
                _event.emit(CookiesEvent.ShowToast(R.string.toast_cookie_set_preview_failed))
            } else {
                _event.emit(CookiesEvent.ShowCookieText(title, text))
            }
        }
    }

    companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
