package org.strigate.ferrot.presentation.viewmodel

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.AvailableUpdateUseCase
import org.strigate.ferrot.domain.usecase.DownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.DownloadWithMetadataUseCase
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.download.StartDownloadUseCase
import org.strigate.ferrot.domain.usecase.download.StopDownloadUseCase
import org.strigate.ferrot.domain.usecase.notifications.ClearNotificationsByDownloadIdUseCase
import org.strigate.ferrot.presentation.Screen
import org.strigate.ferrot.presentation.event.DownloadsEvent
import org.strigate.ferrot.presentation.mapper.toUiData
import org.strigate.ferrot.presentation.model.AvailableUpdateUiData
import org.strigate.ferrot.presentation.model.DownloadStatusUiData
import org.strigate.ferrot.presentation.model.DownloadsUiData
import org.strigate.ferrot.presentation.model.isActive
import org.strigate.ferrot.presentation.state.DownloadsUiState
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val analyticsLogger: AnalyticsLogger,
    private val downloadUseCase: DownloadUseCase,
    private val stopDownloadsUseCase: StopDownloadUseCase,
    private val startDownloadUseCase: StartDownloadUseCase,
    private val availableUpdateUseCase: AvailableUpdateUseCase,
    private val downloadProgressUseCase: DownloadProgressUseCase,
    private val downloadWithMetadataUseCase: DownloadWithMetadataUseCase,
    private val clearNotificationsByDownloadIdUseCase: ClearNotificationsByDownloadIdUseCase,
    private val settingsUseCase: SettingsUseCase,
) : ViewModel() {
    private val _archived = MutableStateFlow(savedStateHandle[Screen.ARG_ARCHIVED] ?: false)
    val isArchived: StateFlow<Boolean> = _archived

    private val _searchQuery = MutableStateFlow(
        TextFieldValue(text = "", selection = TextRange(0))
    )
    val searchQuery: StateFlow<TextFieldValue> = _searchQuery

    private val _events = MutableSharedFlow<DownloadsEvent>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<DownloadsUiState> = getUiState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DownloadsUiState.Loading,
    )

    private fun getUiState(): Flow<DownloadsUiState> {
        val searchTextFlow = searchQuery
            .map { it.text }
            .distinctUntilChanged()
        val archivedFlow = isArchived
        val downloadsWithMetadataFlow = archivedFlow
            .flatMapLatest { archived ->
                downloadWithMetadataUseCase.getDownloadsWithMetadataAsFlowUseCase(archived = archived)
            }
        val availableUpdateFlow = availableUpdateUseCase.getAvailableUpdateAsFlowUseCase()
        val leftSwipeActionFlow = settingsUseCase.getLeftSwipeActionSettingAsFlowUseCase()
        val rightSwipeActionFlow = settingsUseCase.getRightSwipeActionSettingAsFlowUseCase()
        val swipeActionsFlow = combine(
            leftSwipeActionFlow,
            rightSwipeActionFlow,
        ) { leftSwipeAction, rightSwipeAction ->
            leftSwipeAction.toUiData() to rightSwipeAction.toUiData()
        }

        return combine(
            archivedFlow,
            downloadsWithMetadataFlow,
            availableUpdateFlow,
            searchTextFlow,
            swipeActionsFlow,
        ) { archived, downloadsWithMetadata, availableUpdate, query, swipeActions ->
            val (leftSwipeAction, rightSwipeAction) = swipeActions
            val pendingDeleteIds = downloadsWithMetadata
                .asSequence()
                .filter { it.pendingDelete }
                .map { it.id }
                .toSet()
            val filteredDownloads = downloadsWithMetadata
                .asSequence()
                .filter { !it.pendingDelete }
                .filter {
                    query.isBlank() || it.title.contains(query, ignoreCase = true)
                }
                .map { it.toUiData() }
                .toList()

            val availableUpdateUiData = if (archived) {
                null
            } else {
                availableUpdate?.let {
                    AvailableUpdateUiData(
                        localFilePath = it.localFilePath,
                        tag = it.tag,
                    )
                }
            }
            DownloadsUiState.Data(
                data = DownloadsUiData(
                    downloads = filteredDownloads,
                    availableUpdate = availableUpdateUiData,
                    pendingDeleteIds = pendingDeleteIds,
                    leftSwipeAction = leftSwipeAction,
                    rightSwipeAction = rightSwipeAction,
                ),
            )
        }.flowOn(Dispatchers.Default)
    }

    fun logShown() = analyticsLogger.logScreen(AnalyticsEvents.Screens.DOWNLOADS)

    fun updateSearchQuery(value: TextFieldValue) {
        val trimmed = value.text.take(MAX_SEARCH_LENGTH)
        val normalizedValue = TextFieldValue(
            text = trimmed,
            selection = TextRange(trimmed.length),
        )
        if (_searchQuery.value == normalizedValue) {
            return
        }
        _searchQuery.value = normalizedValue
    }

    fun setArchived(archived: Boolean) {
        if (_archived.value == archived) {
            return
        }
        _archived.value = archived
    }

    fun stopDownload(downloadId: Long) = viewModelScope.launch {
        stopDownloadAndResetProgress(downloadId)
    }

    fun stopAllDownloads() {
        val data = uiState.value as? DownloadsUiState.Data ?: return
        val activeDownloadIds = data.data.downloads
            .asSequence()
            .filter { it.status.isActive }
            .map { it.id }
            .toList()

        if (activeDownloadIds.isEmpty()) {
            return
        }
        viewModelScope.launch {
            activeDownloadIds.forEach { downloadId ->
                stopDownloadAndResetProgress(downloadId)
            }
        }
    }

    fun retryDownload(downloadId: Long) = viewModelScope.launch {
        startDownloadUseCase(downloadId)
    }

    fun retryFailedDownloads() {
        val data = uiState.value as? DownloadsUiState.Data ?: return
        val failedDownloadIds = data.data.downloads
            .asSequence()
            .filter { it.status == DownloadStatusUiData.FAILED }
            .map { it.id }
            .toList()

        if (failedDownloadIds.isEmpty()) {
            return
        }
        viewModelScope.launch {
            failedDownloadIds.forEach { downloadId ->
                startDownloadUseCase(downloadId)
            }
        }
    }

    fun toggleDownloadsSeen(downloadIds: Set<Long>) {
        val data = uiState.value as? DownloadsUiState.Data ?: return
        val selectedDownloads = data.data.downloads.filter { it.id in downloadIds }
        if (selectedDownloads.isEmpty()) {
            return
        }
        val shouldMarkSeen = selectedDownloads.any { !it.seen }
        viewModelScope.launch {
            downloadUseCase.updateDownloadsSeenUseCase(downloadIds, shouldMarkSeen)
            if (shouldMarkSeen) {
                downloadIds.forEach(clearNotificationsByDownloadIdUseCase::invoke)
            }
        }
    }

    fun markDownloadsPendingDelete(downloadIds: Set<Long>, pendingDelete: Boolean = true) {
        viewModelScope.launch {
            downloadUseCase.updateDownloadsPendingDeleteUseCase(downloadIds, pendingDelete)
            if (pendingDelete && downloadIds.isNotEmpty()) {
                downloadUseCase.requestDeletePendingDownloadsDelayedUseCase()
            }
        }
    }

    fun updateDownloadsArchived(downloadIds: Set<Long>, archived: Boolean = true) {
        viewModelScope.launch {
            downloadUseCase.updateDownloadsArchivedUseCase(downloadIds, archived)
            if (archived) {
                downloadIds.forEach(clearNotificationsByDownloadIdUseCase::invoke)
            }
        }
    }

    fun requestDeletePendingDownloadsImmediate() {
        viewModelScope.launch {
            downloadUseCase.requestDeletePendingDownloadsImmediateUseCase()
        }
    }

    fun installAvailableUpdate() {
        val data = uiState.value as? DownloadsUiState.Data ?: return
        val localFilePath = data.data.availableUpdate?.localFilePath
        if (localFilePath.isNullOrBlank()) {
            return
        }
        viewModelScope.launch {
            _events.emit(DownloadsEvent.InstallUpdate(localFilePath))
        }
    }

    private suspend fun stopDownloadAndResetProgress(downloadId: Long) {
        runCatching {
            downloadUseCase.updateDownloadStatusUseCase(downloadId, DownloadStatus.STOPPED)
            downloadProgressUseCase.updateDownloadProgressUseCase(
                id = downloadId,
                progressPercent = 0F,
                bytesDownloaded = 0L,
                etaSeconds = null,
            )
        }
        stopDownloadsUseCase(downloadId)
    }

    companion object {
        private const val MAX_SEARCH_LENGTH = 100
        private const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
