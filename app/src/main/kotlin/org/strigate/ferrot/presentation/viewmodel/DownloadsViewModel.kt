package org.strigate.ferrot.presentation.viewmodel

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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
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
import org.strigate.ferrot.domain.usecase.StateUseCase
import org.strigate.ferrot.domain.usecase.download.StartDownloadUseCase
import org.strigate.ferrot.domain.usecase.download.StopDownloadUseCase
import org.strigate.ferrot.domain.usecase.notifications.ClearNotificationsByDownloadIdUseCase
import org.strigate.ferrot.presentation.Screen
import org.strigate.ferrot.presentation.event.DownloadsEvent
import org.strigate.ferrot.presentation.mapper.toUiData
import org.strigate.ferrot.presentation.model.AvailableUpdateUiData
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
    private val stateUseCase: StateUseCase,
) : ViewModel() {
    private val _archived = MutableStateFlow(savedStateHandle[Screen.ARG_ARCHIVED] ?: false)
    val isArchived: StateFlow<Boolean> = _archived

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _events = MutableSharedFlow<DownloadsEvent>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<DownloadsUiState> = getUiState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DownloadsUiState.Loading,
    )

    private fun getUiState(): Flow<DownloadsUiState> {
        val searchTextFlow = searchQuery
        val archivedFlow = isArchived
        val downloadsWithMetadataFlow = archivedFlow
            .flatMapLatest { archived ->
                downloadWithMetadataUseCase.getDownloadsWithMetadataAsFlowUseCase(archived = archived)
            }
        val availableUpdateFlow = availableUpdateUseCase.getAvailableUpdateAsFlowUseCase()
        val leftSwipeActionFlow = settingsUseCase.getLeftSwipeActionSettingAsFlowUseCase()
        val rightSwipeActionFlow = settingsUseCase.getRightSwipeActionSettingAsFlowUseCase()
        val layoutAndSwipeActionsFlow = combine(
            archivedFlow.flatMapLatest { archived ->
                if (archived) {
                    stateUseCase.getArchivedDownloadsGridLayoutEnabledUseCase()
                } else {
                    stateUseCase.getDownloadsGridLayoutEnabledUseCase()
                }
            },
            leftSwipeActionFlow,
            rightSwipeActionFlow,
        ) { gridLayoutEnabled, leftSwipeAction, rightSwipeAction ->
            Triple(
                gridLayoutEnabled,
                leftSwipeAction.toUiData(),
                rightSwipeAction.toUiData(),
            )
        }

        return combine(
            archivedFlow,
            downloadsWithMetadataFlow,
            availableUpdateFlow,
            searchTextFlow,
            layoutAndSwipeActionsFlow,
        ) { archived, downloadsWithMetadata, availableUpdate, query, layoutAndSwipeActions ->
            val (gridLayoutEnabled, leftSwipeAction, rightSwipeAction) = layoutAndSwipeActions
            val pendingDeleteIds = downloadsWithMetadata
                .asSequence()
                .filter { it.pendingDelete }
                .map { it.id }
                .toSet()
            val visibleDownloads = downloadsWithMetadata
                .asSequence()
                .filter { !it.pendingDelete }
                .filter {
                    query.isBlank() || it.title.contains(query, ignoreCase = true)
                }
                .toList()
            val retryFailedDownloadIds = visibleDownloads
                .asSequence()
                .filter { it.status == DownloadStatus.FAILED }
                .map { it.id }
                .toSet()
            val filteredDownloads = visibleDownloads
                .asSequence()
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
                    retryFailedDownloadIds = retryFailedDownloadIds,
                    gridLayoutEnabled = gridLayoutEnabled,
                    leftSwipeAction = leftSwipeAction,
                    rightSwipeAction = rightSwipeAction,
                ),
            )
        }.flowOn(Dispatchers.Default)
    }

    fun logShown() = analyticsLogger.logScreen(AnalyticsEvents.Screens.DOWNLOADS)

    fun updateSearchQuery(value: String) {
        val query = value.take(MAX_SEARCH_LENGTH)
        if (_searchQuery.value == query) {
            return
        }
        _searchQuery.value = query
    }

    fun setArchived(archived: Boolean) {
        if (_archived.value == archived) {
            return
        }
        _archived.value = archived
    }

    fun toggleGridLayoutEnabled() = viewModelScope.launch {
        if (isArchived.value) {
            stateUseCase.toggleArchivedDownloadsGridLayoutEnabledUseCase()
        } else {
            stateUseCase.toggleDownloadsGridLayoutEnabledUseCase()
        }
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
        analyticsLogger.logEvent(AnalyticsEvents.DOWNLOAD_RETRY)
        startDownloadUseCase(downloadId)
    }

    fun retryFailedDownloads() {
        val data = uiState.value as? DownloadsUiState.Data ?: return
        val failedDownloadIds = data.data.retryFailedDownloadIds.toList()
        if (failedDownloadIds.isEmpty()) {
            return
        }
        viewModelScope.launch {
            analyticsLogger.logEvent(AnalyticsEvents.DOWNLOADS_RETRY)
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
        val selectedDownloadIds = selectedDownloads.mapTo(mutableSetOf()) { it.id }
        val shouldMarkSeen = selectedDownloads.any { !it.seen }
        viewModelScope.launch {
            downloadUseCase.updateDownloadsSeenUseCase(selectedDownloadIds, shouldMarkSeen)
            if (shouldMarkSeen) {
                selectedDownloadIds.forEach(clearNotificationsByDownloadIdUseCase::invoke)
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
