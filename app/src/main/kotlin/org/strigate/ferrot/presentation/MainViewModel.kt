package org.strigate.ferrot.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.download.StartDownloadUseCase
import org.strigate.ferrot.util.UidUtil
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val downloadUseCase: DownloadUseCase,
    private val startDownloadUseCase: StartDownloadUseCase,
) : ViewModel() {
    private val _navigateRoute = MutableStateFlow<NavigationEvent?>(null)
    val navigateRoute: StateFlow<NavigationEvent?> = _navigateRoute.asStateFlow()

    fun navigateTo(route: String, popUpToDownloads: Boolean = false) {
        _navigateRoute.value = NavigationEvent.Route(
            popUpToDownloads = popUpToDownloads,
            route = route,
        )
    }

    fun navigateToDownload(downloadId: Long) {
        viewModelScope.launch {
            downloadUseCase.getDownloadByIdUseCase(downloadId)?.let { download ->
                navigateTo(
                    route = Screen.Download.route(download.id),
                    popUpToDownloads = true,
                )
            }
        }
    }

    fun startDownload(url: String) {
        viewModelScope.launch {
            val downloadId = downloadUseCase.saveDownloadUseCase(
                Download(
                    url = url,
                    uid = UidUtil.generateUid(),
                    status = DownloadStatus.QUEUED,
                ),
            )
            if (downloadId != -1L) {
                Log.d(LOG_TAG, "Saved download: downloadId=$downloadId")
                startDownloadUseCase(downloadId)
            } else {
                Log.e(LOG_TAG, "Could not save download")
            }
        }
    }

    fun resetNavigate() {
        _navigateRoute.value = null
    }
}

sealed class NavigationEvent {
    data class Route(
        val route: String,
        val popUpToDownloads: Boolean = false,
    ) : NavigationEvent()
}
