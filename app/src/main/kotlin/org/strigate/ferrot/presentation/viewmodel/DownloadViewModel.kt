package org.strigate.ferrot.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.domain.model.DownloadMediaType
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.DownloadAudioUseCase
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.DownloadVideoUseCase
import org.strigate.ferrot.domain.usecase.DownloadWithMetadataUseCase
import org.strigate.ferrot.domain.usecase.download.StartDownloadUseCase
import org.strigate.ferrot.domain.usecase.notifications.ClearNotificationsByDownloadIdUseCase
import org.strigate.ferrot.presentation.Screen
import org.strigate.ferrot.presentation.event.DownloadEvent
import org.strigate.ferrot.presentation.mapper.toPageUiData
import org.strigate.ferrot.presentation.model.DownloadPageUiData
import org.strigate.ferrot.presentation.model.DownloadUiData
import org.strigate.ferrot.presentation.state.DownloadUiState
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val analyticsLogger: AnalyticsLogger,
    private val downloadUseCase: DownloadUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val downloadAudioUseCase: DownloadAudioUseCase,
    private val downloadProgressUseCase: DownloadProgressUseCase,
    private val downloadMetadataUseCase: DownloadMetadataUseCase,
    private val clearNotificationsByDownloadIdUseCase: ClearNotificationsByDownloadIdUseCase,
    private val startDownloadUseCase: StartDownloadUseCase,
    downloadWithMetadataUseCase: DownloadWithMetadataUseCase,
) : ViewModel() {
    private val initialId: Long = checkNotNull(savedStateHandle[Screen.Download.ARG_DOWNLOAD_ID])

    private val downloadIds = downloadWithMetadataUseCase
        .getDownloadsWithMetadataAsFlowUseCase()
        .map { downloads ->
            downloads
                .asSequence()
                .filter { !it.pendingDelete }
                .map { it.id }
                .toList()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList(),
        )

    private val _selectedId = MutableStateFlow(initialId)
    val selectedId: StateFlow<Long> = _selectedId

    val uiState = getUiState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DownloadUiState.Loading,
    )

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

    private val _events = MutableSharedFlow<DownloadEvent>(
        replay = 0,
        extraBufferCapacity = 1,
    )
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            clearNotificationsByDownloadIdUseCase(initialId)
        }
        viewModelScope.launch {
            var previousIds = emptyList<Long>()
            downloadIds.collect { ids ->
                val currentSelectedId = _selectedId.value
                if (ids.isNotEmpty() && currentSelectedId !in ids) {
                    val previousIndex = previousIds.indexOf(currentSelectedId)
                    _selectedId.value = if (previousIndex >= 0 && previousIndex < ids.size) {
                        ids[previousIndex]
                    } else {
                        ids.last()
                    }
                }
                previousIds = ids
            }
        }
    }

    private fun getUiState(): Flow<DownloadUiState> {
        return combine(downloadIds, selectedId) { ids, currentSelectedId ->
            val selectedOrDefaultId = when {
                ids.isEmpty() -> null
                currentSelectedId in ids -> currentSelectedId
                initialId in ids -> initialId
                else -> ids.firstOrNull()
            }
            DownloadUiState.Data(
                DownloadUiData(
                    downloadIds = ids,
                    id = selectedOrDefaultId,
                )
            )
        }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
    }

    fun getDownloadPageUiData(downloadId: Long): Flow<DownloadPageUiData?> {
        val downloadFlow = downloadUseCase
            .getDownloadByIdAsFlowUseCase(downloadId)
        val videoFlow = downloadVideoUseCase
            .getDownloadVideoByDownloadIdAsFlowUseCase(downloadId)
        val audioFlow = downloadAudioUseCase
            .getDownloadAudioByDownloadIdAsFlowUseCase(downloadId)
        val metadataFlow = downloadMetadataUseCase
            .getDownloadMetadataByIdAsFlowUseCase(downloadId)
        val progressFlow = downloadProgressUseCase
            .getDownloadProgressByDownloadIdAsFlowUseCase(downloadId)

        return combine(
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
        }.flowOn(Dispatchers.Default)
    }

    fun logShown() = analyticsLogger.logScreen(AnalyticsEvents.Screens.DOWNLOAD)

    fun selectDownload(downloadId: Long) {
        _selectedId.value = downloadId
        if (_selectedMediaById.value[downloadId] == null) {
            _selectedMediaById.value = _selectedMediaById
                .value
                .toMutableMap()
                .also {
                    it[downloadId] = DownloadMediaType.VIDEO
                }
        }
    }

    fun markSeenIfCompleted(downloadId: Long) = viewModelScope.launch {
        val download = downloadUseCase.getDownloadByIdUseCase(downloadId) ?: return@launch
        if (download.status == DownloadStatus.COMPLETED && !download.seen) {
            downloadUseCase.updateDownloadsSeenUseCase(setOf(downloadId))
        }
    }

    fun setSelectedMedia(type: DownloadMediaType, forDownloadId: Long? = null) {
        val downloadId = forDownloadId ?: _selectedId.value
        _selectedMediaById.value = _selectedMediaById
            .value
            .toMutableMap()
            .also {
                it[downloadId] = type
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

    fun deleteDownload(id: Long? = null) = viewModelScope.launch {
        val downloadId = id ?: _selectedId.value
        val isLastDownload = downloadIds.value.count { it != downloadId } == 0
        downloadUseCase.requestDeleteDownloadsUseCase(
            downloadIds = listOf(downloadId),
        )
        if (isLastDownload) {
            _events.emit(DownloadEvent.NavigateBack)
        }
    }

    fun shareDownload(id: Long? = null) = viewModelScope.launch {
        val downloadId = id ?: _selectedId.value
        val download = getDownloadPageUiData(downloadId).first() ?: return@launch
        val path = getSelectedMediaFilePath(downloadId, download) ?: return@launch
        _events.emit(DownloadEvent.Share(path))
    }

    fun saveDownload(id: Long? = null) = viewModelScope.launch {
        val downloadId = id ?: _selectedId.value
        val download = getDownloadPageUiData(downloadId).first() ?: return@launch
        val path = getSelectedMediaFilePath(downloadId, download) ?: return@launch
        _events.emit(DownloadEvent.Save(path))
    }

    fun playDownload(id: Long? = null) = viewModelScope.launch {
        val downloadId = id ?: _selectedId.value
        val download = getDownloadPageUiData(downloadId).first() ?: return@launch
        val path = getSelectedMediaFilePath(downloadId, download) ?: return@launch
        _events.emit(DownloadEvent.Play(path))
    }

    fun retryDownload(id: Long? = null) = viewModelScope.launch {
        val downloadId = id ?: _selectedId.value
        startDownloadUseCase(downloadId)
    }

    private fun getSelectedMediaFilePath(
        downloadId: Long,
        download: DownloadPageUiData,
    ): String? {
        val mediaType = _selectedMediaById.value[downloadId] ?: DownloadMediaType.VIDEO
        return if (mediaType == DownloadMediaType.AUDIO) {
            download.audio?.filePath
        } else {
            download.video?.filePath
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
