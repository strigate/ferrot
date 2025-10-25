package org.strigate.ferrot.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.combined.DeleteDownloadAndRelatedCombinedUseCase
import org.strigate.ferrot.domain.usecase.download.StartDownloadUseCase
import org.strigate.ferrot.domain.usecase.notifications.ClearNotificationsByDownloadIdUseCase
import org.strigate.ferrot.helper.PlayHelper
import org.strigate.ferrot.helper.SaveHelper
import org.strigate.ferrot.helper.ShareHelper
import org.strigate.ferrot.presentation.Screen
import org.strigate.ferrot.presentation.mapper.toUiData
import org.strigate.ferrot.presentation.state.DownloadUiState
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val analyticsLogger: AnalyticsLogger,
    private val downloadUseCase: DownloadUseCase,
    private val downloadProgressUseCase: DownloadProgressUseCase,
    private val downloadMetadataUseCase: DownloadMetadataUseCase,
    private val deleteDownloadAndRelatedCombinedUseCase: DeleteDownloadAndRelatedCombinedUseCase,
    private val clearNotificationsByDownloadIdUseCase: ClearNotificationsByDownloadIdUseCase,
    private val startDownloadUseCase: StartDownloadUseCase,
) : ViewModel() {
    private val downloadId: Long = checkNotNull(savedStateHandle[Screen.Download.ARG_DOWNLOAD_ID])

    val uiState = getUiState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DownloadUiState.Loading,
    )

    init {
        viewModelScope.launch {
            clearNotificationsByDownloadIdUseCase(downloadId)
        }
    }

    private fun getUiState(id: Long = downloadId): Flow<DownloadUiState> {
        return combine(
            downloadUseCase.getDownloadByIdAsFlowUseCase(id),
            downloadMetadataUseCase.getDownloadMetadataByIdAsFlowUseCase(id),
            downloadProgressUseCase.getDownloadProgressByDownloadIdAsFlowUseCase(id),
        ) { download, metadata, progress ->
            if (download == null) {
                DownloadUiState.Error
            } else {
                DownloadUiState.Data(
                    data = download.toUiData(
                        metadata = metadata,
                        progress = progress,
                    ),
                )
            }
        }
    }

    fun logShown() = analyticsLogger.logScreen(AnalyticsEvents.Screens.DOWNLOAD)

    fun deleteDownload() {
        viewModelScope.launch {
            val success = deleteDownloadAndRelatedCombinedUseCase(downloadId)
            Log.d(LOG_TAG, "Deleted download id=$downloadId success=$success")
        }
    }

    fun shareDownload() {
        viewModelScope.launch {
            val download = downloadUseCase.getDownloadByIdUseCase(downloadId) ?: return@launch
            ShareHelper.shareFileIfExists(appContext, download.filePath)
        }
    }

    fun saveDownload() {
        viewModelScope.launch {
            val download = downloadUseCase.getDownloadByIdUseCase(downloadId) ?: return@launch
            SaveHelper.saveToDownloads(appContext, download.filePath)
        }
    }

    fun playDownload() {
        viewModelScope.launch {
            val download = downloadUseCase.getDownloadByIdUseCase(downloadId) ?: return@launch
            PlayHelper.playFileIfExists(appContext, download.filePath)
        }
    }

    fun retryDownload() {
        viewModelScope.launch {
            startDownloadUseCase(downloadId)
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
