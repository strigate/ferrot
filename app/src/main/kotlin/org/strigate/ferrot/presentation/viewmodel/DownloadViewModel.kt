package org.strigate.ferrot.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.domain.model.DownloadMediaType
import org.strigate.ferrot.domain.usecase.DownloadAudioUseCase
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.DownloadVideoUseCase
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
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val downloadAudioUseCase: DownloadAudioUseCase,
    private val downloadProgressUseCase: DownloadProgressUseCase,
    private val downloadMetadataUseCase: DownloadMetadataUseCase,
    private val deleteDownloadAndRelatedCombinedUseCase: DeleteDownloadAndRelatedCombinedUseCase,
    private val clearNotificationsByDownloadIdUseCase: ClearNotificationsByDownloadIdUseCase,
    private val startDownloadUseCase: StartDownloadUseCase,
) : ViewModel() {
    private val downloadId: Long = checkNotNull(savedStateHandle[Screen.Download.ARG_DOWNLOAD_ID])

    private val selectedMedia = MutableStateFlow(DownloadMediaType.VIDEO)
    val selectedMediaFlow: Flow<DownloadMediaType> = selectedMedia

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

    private fun getUiState(id: Long = downloadId) = combine(
        downloadUseCase.getDownloadByIdAsFlowUseCase(id),
        downloadVideoUseCase.getDownloadVideoAsFlowUseCase(id),
        downloadAudioUseCase.getDownloadAudioAsFlowUseCase(id),
        downloadMetadataUseCase.getDownloadMetadataByIdAsFlowUseCase(id),
        downloadProgressUseCase.getDownloadProgressByDownloadIdAsFlowUseCase(id),
    ) { download, video, audio, metadata, progress ->
        if (download == null) {
            DownloadUiState.Error
        } else {
            DownloadUiState.Data(
                data = download.toUiData(
                    video = video,
                    audio = audio,
                    metadata = metadata,
                    progress = progress,
                ),
            )
        }
    }

    fun logShown() = analyticsLogger.logScreen(AnalyticsEvents.Screens.DOWNLOAD)

    fun setSelectedMedia(type: DownloadMediaType) {
        selectedMedia.value = type
    }

    fun deleteDownload() {
        viewModelScope.launch {
            val success = deleteDownloadAndRelatedCombinedUseCase(downloadId)
            Log.d(LOG_TAG, "Deleted download id=$downloadId success=$success")
        }
    }

    fun shareDownload() = viewModelScope.launch {
        val path = currentSelectedFilePath() ?: return@launch
        ShareHelper.shareFileIfExists(appContext, path)
    }

    fun saveDownload() = viewModelScope.launch {
        val path = currentSelectedFilePath() ?: return@launch
        SaveHelper.saveToDownloads(appContext, path)
    }

    fun playDownload() = viewModelScope.launch {
        val path = currentSelectedFilePath() ?: return@launch
        PlayHelper.playFileIfExists(appContext, path)
    }

    fun retryDownload() = viewModelScope.launch {
        startDownloadUseCase(downloadId)
    }

    private fun currentSelectedFilePath(): String? {
        val state = uiState.value
        if (state !is DownloadUiState.Data) return null
        return when (selectedMedia.value) {
            DownloadMediaType.VIDEO -> state.data.video?.filePath
            DownloadMediaType.AUDIO -> state.data.audio?.filePath
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
