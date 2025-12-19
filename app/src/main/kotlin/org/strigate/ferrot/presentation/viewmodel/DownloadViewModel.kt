package org.strigate.ferrot.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
import org.strigate.ferrot.domain.usecase.downloadwithmetadata.GetDownloadIdsWithMetadataUseCase
import org.strigate.ferrot.domain.usecase.notifications.ClearNotificationsByDownloadIdUseCase
import org.strigate.ferrot.helper.PlayHelper
import org.strigate.ferrot.helper.SaveHelper
import org.strigate.ferrot.helper.ShareHelper
import org.strigate.ferrot.presentation.Screen
import org.strigate.ferrot.presentation.mapper.toPageUiData
import org.strigate.ferrot.presentation.model.DownloadStatusUiData
import org.strigate.ferrot.presentation.model.DownloadUiData
import org.strigate.ferrot.presentation.state.DownloadUiState
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
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
    private val getDownloadIdsWithMetadataUseCase: GetDownloadIdsWithMetadataUseCase,
    private val clearNotificationsByDownloadIdUseCase: ClearNotificationsByDownloadIdUseCase,
    private val deleteDownloadAndRelatedCombinedUseCase: DeleteDownloadAndRelatedCombinedUseCase,
    private val startDownloadUseCase: StartDownloadUseCase,
) : ViewModel() {
    private val initialId: Long = checkNotNull(savedStateHandle[Screen.Download.ARG_DOWNLOAD_ID])

    val uiState = getUiState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DownloadUiState.Loading,
    )

    private val _selectedId = MutableStateFlow(initialId)
    val selectedId: StateFlow<Long> = _selectedId

    private val _selectedMediaById = MutableStateFlow<Map<Long, DownloadMediaType>>(emptyMap())
    val selectedMedia: StateFlow<DownloadMediaType> = combine(
        selectedId,
        _selectedMediaById,
    ) { id, map ->
        map[id] ?: DownloadMediaType.VIDEO
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DownloadMediaType.VIDEO,
    )

    init {
        viewModelScope.launch {
            clearNotificationsByDownloadIdUseCase(initialId)
        }
    }

    private fun getUiState(id: Long = initialId) =
        getDownloadIdsWithMetadataUseCase()
            .flatMapLatest { ids ->
                if (ids.isEmpty()) {
                    flowOf(
                        DownloadUiState.Data(
                            DownloadUiData(
                                downloads = emptyList(),
                                id = null,
                            )
                        )
                    )
                } else {
                    val flows = ids.map { downloadId ->
                        val downloadFlow = downloadUseCase
                            .getDownloadByIdAsFlowUseCase(downloadId)
                        val videoFlow = downloadVideoUseCase
                            .getDownloadVideoAsFlowUseCase(downloadId)
                        val audioFlow = downloadAudioUseCase
                            .getDownloadAudioAsFlowUseCase(downloadId)
                        val metadataFlow = downloadMetadataUseCase
                            .getDownloadMetadataByIdAsFlowUseCase(downloadId)
                        val progressFlow = downloadProgressUseCase
                            .getDownloadProgressByDownloadIdAsFlowUseCase(downloadId)

                        combine(
                            downloadFlow,
                            videoFlow,
                            audioFlow,
                            metadataFlow,
                            progressFlow,
                        ) { base, video, audio, metadata, progress ->
                            base?.toPageUiData(
                                video = video,
                                audio = audio,
                                metadata = metadata,
                                progress = progress,
                            )
                        }.filterNotNull()
                    }
                    combine(flows) { pagesArray ->
                        val pagesList = pagesArray.map { it }
                        DownloadUiState.Data(
                            DownloadUiData(
                                downloads = pagesList,
                                id = id,
                            )
                        )
                    }
                }
            }

    fun logShown() = analyticsLogger.logScreen(AnalyticsEvents.Screens.DOWNLOAD)

    fun selectDownload(id: Long) {
        _selectedId.value = id
        if (_selectedMediaById.value[id] == null) {
            _selectedMediaById.value = _selectedMediaById
                .value
                .toMutableMap()
                .also {
                    it[id] = DownloadMediaType.VIDEO
                }
        }
    }

    fun markSeenIfCompleted(downloadId: Long) {
        viewModelScope.launch {
            val state = uiState.value
            if (state !is DownloadUiState.Data) {
                return@launch
            }
            val download = state.data.downloads.firstOrNull { it.id == downloadId } ?: return@launch
            if (download.status == DownloadStatusUiData.COMPLETED && !download.seen) {
                downloadUseCase.updateDownloadSeenByIdUseCase(downloadId)
            }
        }
    }

    fun setSelectedMedia(type: DownloadMediaType, forDownloadId: Long? = null) {
        val id = forDownloadId ?: _selectedId.value
        _selectedMediaById.value = _selectedMediaById
            .value
            .toMutableMap()
            .also {
                it[id] = type
            }
    }

    fun setDefaultsForIds(ids: List<Long>) {
        val current = _selectedMediaById.value.toMutableMap()
        var changed = false
        ids.forEach { id ->
            if (current[id] == null) {
                current[id] = DownloadMediaType.VIDEO
                changed = true
            }
        }
        if (changed) {
            _selectedMediaById.value = current
        }
    }

    fun deleteDownload(id: Long? = null) {
        viewModelScope.launch {
            val downloadId = id ?: _selectedId.value
            val success = deleteDownloadAndRelatedCombinedUseCase(downloadId)
            Log.d(LOG_TAG, "Deleted download id=$downloadId success=$success")
        }
    }

    fun shareDownload(id: Long? = null) = viewModelScope.launch {
        val downloadId = id ?: _selectedId.value
        val path = getSelectedMediaFilePath(downloadId) ?: return@launch
        ShareHelper.shareFileIfExists(appContext, path)
    }

    fun saveDownload(id: Long? = null) = viewModelScope.launch {
        val downloadId = id ?: _selectedId.value
        val path = getSelectedMediaFilePath(downloadId) ?: return@launch
        SaveHelper.saveToDownloads(appContext, path)
    }

    fun playDownload(id: Long? = null) = viewModelScope.launch {
        val downloadId = id ?: _selectedId.value
        val path = getSelectedMediaFilePath(downloadId) ?: return@launch
        PlayHelper.playFileIfExists(appContext, path)
    }

    fun retryDownload(id: Long? = null) = viewModelScope.launch {
        val downloadId = id ?: _selectedId.value
        startDownloadUseCase(downloadId)
    }

    private fun getSelectedMediaFilePath(downloadId: Long): String? {
        val state = uiState.value
        if (state !is DownloadUiState.Data) {
            return null
        }
        val download = state.data.downloads.firstOrNull { it.id == downloadId } ?: return null
        val mediaType = selectedMedia.value
        return when (mediaType) {
            DownloadMediaType.VIDEO -> download.video?.filePath
            DownloadMediaType.AUDIO -> download.audio?.filePath
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
