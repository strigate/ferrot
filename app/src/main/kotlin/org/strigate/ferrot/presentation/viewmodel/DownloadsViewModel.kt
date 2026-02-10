package org.strigate.ferrot.presentation.viewmodel

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.AvailableUpdateUseCase
import org.strigate.ferrot.domain.usecase.DownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.DownloadWithMetadataUseCase
import org.strigate.ferrot.domain.usecase.download.StartDownloadUseCase
import org.strigate.ferrot.domain.usecase.download.StopDownloadUseCase
import org.strigate.ferrot.presentation.mapper.toUiData
import org.strigate.ferrot.presentation.model.AvailableUpdateUiData
import org.strigate.ferrot.presentation.model.DownloadsUiData
import org.strigate.ferrot.presentation.state.DownloadsUiState
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val analyticsLogger: AnalyticsLogger,
    private val downloadUseCase: DownloadUseCase,
    private val stopDownloadsUseCase: StopDownloadUseCase,
    private val startDownloadUseCase: StartDownloadUseCase,
    private val availableUpdateUseCase: AvailableUpdateUseCase,
    private val downloadProgressUseCase: DownloadProgressUseCase,
    private val downloadWithMetadataUseCase: DownloadWithMetadataUseCase,
) : ViewModel() {
    private val _searchQuery = MutableStateFlow(
        TextFieldValue(text = "", selection = TextRange(0))
    )
    val searchQuery: StateFlow<TextFieldValue> = _searchQuery

    val uiState: StateFlow<DownloadsUiState> = getUiState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DownloadsUiState.Loading,
    )

    private fun getUiState(): Flow<DownloadsUiState> {
        val downloadsWithMetadataFlow = downloadWithMetadataUseCase
            .getDownloadsWithMetadataAsFlowUseCase()
        val availableUpdateFlow = availableUpdateUseCase
            .getAvailableUpdateAsFlowUseCase()

        return combine(
            downloadsWithMetadataFlow,
            availableUpdateFlow,
            _searchQuery,
        ) { downloadsWithMetadata, availableUpdate, query ->
            val text = query.text
            val filteredDownloads = downloadsWithMetadata
                .map { it.toUiData() }
                .filter {
                    text.isBlank() || it.title.contains(text, ignoreCase = true)
                }

            val availableUpdateUiData = availableUpdate?.let {
                AvailableUpdateUiData(
                    localFilePath = it.localFilePath,
                    tag = it.tag,
                )
            }
            DownloadsUiState.Data(
                data = DownloadsUiData(
                    downloads = filteredDownloads,
                    availableUpdate = availableUpdateUiData,
                ),
            )
        }
    }

    fun logShown() = analyticsLogger.logScreen(AnalyticsEvents.Screens.DOWNLOADS)

    fun updateSearchQuery(value: TextFieldValue) {
        val trimmed = value.text.take(MAX_SEARCH_LENGTH)
        _searchQuery.value = TextFieldValue(
            text = trimmed,
            selection = TextRange(trimmed.length),
        )
    }

    fun stopDownload(downloadId: Long) = viewModelScope.launch {
        runCatching {
            downloadUseCase.updateDownloadStatusByIdUseCase(downloadId, DownloadStatus.STOPPED)
            downloadProgressUseCase.updateDownloadProgressUseCase(
                id = downloadId,
                progressPercent = 0F,
                bytesDownloaded = 0L,
                etaSeconds = null,
            )
        }
        stopDownloadsUseCase(downloadId)
    }

    fun retryDownload(downloadId: Long) = viewModelScope.launch {
        startDownloadUseCase(downloadId)
    }

    fun deleteDownloads(downloadIds: Set<Long>) = viewModelScope.launch {
        downloadUseCase.requestDeleteDownloadsUseCase(
            downloadIds = downloadIds,
        )
    }

    companion object {
        private const val MAX_SEARCH_LENGTH = 100
    }
}
