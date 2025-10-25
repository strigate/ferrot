package org.strigate.ferrot.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.AvailableUpdateUseCase
import org.strigate.ferrot.domain.usecase.DownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.combined.DeleteDownloadAndRelatedCombinedUseCase
import org.strigate.ferrot.domain.usecase.download.StartDownloadUseCase
import org.strigate.ferrot.domain.usecase.download.StopDownloadUseCase
import org.strigate.ferrot.domain.usecase.downloadwithmetadata.GetDownloadsWithMetadataUseCase
import org.strigate.ferrot.helper.InstallHelper
import org.strigate.ferrot.presentation.mapper.toUiData
import org.strigate.ferrot.presentation.model.AvailableUpdateUiData
import org.strigate.ferrot.presentation.model.DownloadsUiData
import org.strigate.ferrot.presentation.state.DownloadsUiState
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val analyticsLogger: AnalyticsLogger,
    private val downloadUseCase: DownloadUseCase,
    private val downloadProgressUseCase: DownloadProgressUseCase,
    private val getDownloadsWithMetadata: GetDownloadsWithMetadataUseCase,
    private val stopDownloadsUseCase: StopDownloadUseCase,
    private val startDownloadUseCase: StartDownloadUseCase,
    private val deleteDownloadAndRelatedCombinedUseCase: DeleteDownloadAndRelatedCombinedUseCase,
    private val availableUpdateUseCase: AvailableUpdateUseCase,
) : ViewModel() {
    val uiState: StateFlow<DownloadsUiState> = getUiState().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DownloadsUiState.Loading,
    )

    private fun getUiState(): Flow<DownloadsUiState> {
        val downloadsFlow = getDownloadsWithMetadata()
            .map { aggregates -> aggregates.map { it.toUiData() } }

        val availableUpdateFlow = availableUpdateUseCase.getAvailableUpdateAsFlowUseCase()
        return combine(
            downloadsFlow,
            availableUpdateFlow,
        ) { downloadItems, availableUpdate ->
            val availableUpdateUiData = availableUpdate?.let {
                AvailableUpdateUiData(
                    tag = it.tag,
                    localFilePath = it.localFilePath,
                )
            }
            DownloadsUiState.Data(
                data = DownloadsUiData(
                    downloads = downloadItems,
                    availableUpdate = availableUpdateUiData,
                ),
            ) as DownloadsUiState
        }
    }

    fun logShown() = analyticsLogger.logScreen(AnalyticsEvents.Screens.DOWNLOADS)

    fun stopDownload(downloadId: Long) {
        viewModelScope.launch {
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
    }

    fun retryDownload(downloadId: Long) {
        viewModelScope.launch {
            startDownloadUseCase(downloadId)
        }
    }

    fun deleteDownload(downloadId: Long) {
        viewModelScope.launch {
            deleteDownloadAndRelatedCombinedUseCase(downloadId)
        }
    }

    fun requestInstallAvailableUpdate(filePath: String) {
        viewModelScope.launch {
            InstallHelper.requestInstallApkIfExists(appContext, filePath)
        }
    }
}
