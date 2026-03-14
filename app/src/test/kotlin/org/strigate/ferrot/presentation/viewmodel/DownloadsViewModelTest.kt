package org.strigate.ferrot.presentation.viewmodel

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.domain.model.AvailableUpdate
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.model.DownloadWithMetadata
import org.strigate.ferrot.domain.usecase.AvailableUpdateUseCase
import org.strigate.ferrot.domain.usecase.DownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.DownloadWithMetadataUseCase
import org.strigate.ferrot.domain.usecase.availableupdate.GetAvailableUpdateAsFlowUseCase
import org.strigate.ferrot.domain.usecase.download.RequestDeleteDownloadsUseCase
import org.strigate.ferrot.domain.usecase.download.StartDownloadUseCase
import org.strigate.ferrot.domain.usecase.download.StopDownloadUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadStatusByIdUseCase
import org.strigate.ferrot.domain.usecase.downloadprogress.UpdateDownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.downloadwithmetadata.GetDownloadsWithMetadataAsFlowUseCase
import org.strigate.ferrot.presentation.state.DownloadsUiState
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsViewModelTest {
    private lateinit var autoCloseable: AutoCloseable
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var analyticsLogger: AnalyticsLogger

    @Mock
    private lateinit var downloadUseCase: DownloadUseCase

    @Mock
    private lateinit var stopDownloadUseCase: StopDownloadUseCase

    @Mock
    private lateinit var startDownloadUseCase: StartDownloadUseCase

    @Mock
    private lateinit var availableUpdateUseCase: AvailableUpdateUseCase

    @Mock
    private lateinit var downloadProgressUseCase: DownloadProgressUseCase

    @Mock
    private lateinit var downloadWithMetadataUseCase: DownloadWithMetadataUseCase

    @Mock
    private lateinit var getDownloadsWithMetadataAsFlowUseCase: GetDownloadsWithMetadataAsFlowUseCase

    @Mock
    private lateinit var getAvailableUpdateAsFlowUseCase: GetAvailableUpdateAsFlowUseCase

    @Mock
    private lateinit var updateDownloadStatusByIdUseCase: UpdateDownloadStatusByIdUseCase

    @Mock
    private lateinit var updateDownloadProgressUseCase: UpdateDownloadProgressUseCase

    @Mock
    private lateinit var requestDeleteDownloadsUseCase: RequestDeleteDownloadsUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        autoCloseable.close()
    }

    @Test
    fun uiState_exposesMappedDownloadsAndAvailableUpdate() = runTest(testDispatcher) {
        val downloadsFlow = MutableStateFlow(
            listOf(
                createDownload(id = 1L, title = "Download 1"),
                createDownload(id = 2L, title = "Download 2"),
            )
        )
        val updateFlow = MutableStateFlow<AvailableUpdate?>(
            AvailableUpdate(
                tag = "v1.2.3",
                localFilePath = "/tmp/update.apk",
            )
        )
        val viewModel = createViewModel(
            downloadsFlow = downloadsFlow,
            updateFlow = updateFlow,
        )

        val collector = backgroundScope.launch {
            viewModel.uiState.collect()
        }
        waitForUiState(viewModel) { state ->
            val data = state as? DownloadsUiState.Data ?: return@waitForUiState false
            data.data.downloads.size == 2 && data.data.availableUpdate?.tag == "v1.2.3"
        }

        val state = viewModel.uiState.value as DownloadsUiState.Data
        assertEquals(listOf(1L, 2L), state.data.downloads.map { it.id })
        assertEquals("Download 1", state.data.downloads[0].title)
        assertEquals("v1.2.3", state.data.availableUpdate?.tag)
        assertEquals("/tmp/update.apk", state.data.availableUpdate?.localFilePath)

        collector.cancel()
    }

    @Test
    fun updateSearchQuery_trimsInputAndFiltersDownloads() = runTest(testDispatcher) {
        val downloadsFlow = MutableStateFlow(
            listOf(
                createDownload(id = 1L, title = "Download 1"),
                createDownload(id = 2L, title = "Download 2"),
            )
        )
        val updateFlow = MutableStateFlow<AvailableUpdate?>(null)
        val viewModel = createViewModel(
            downloadsFlow = downloadsFlow,
            updateFlow = updateFlow,
        )

        val collector = backgroundScope.launch {
            viewModel.uiState.collect()
        }
        waitForUiState(viewModel) { it is DownloadsUiState.Data }

        val longQuery = "A".repeat(150)
        viewModel.updateSearchQuery(TextFieldValue(longQuery, TextRange(longQuery.length)))

        assertEquals(100, viewModel.searchQuery.value.text.length)
        assertEquals(100, viewModel.searchQuery.value.selection.start)

        viewModel.updateSearchQuery(TextFieldValue("download 2", TextRange(10)))
        waitForUiState(viewModel) { state ->
            val data = state as? DownloadsUiState.Data ?: return@waitForUiState false
            data.data.downloads.size == 1 && data.data.downloads.single().id == 2L
        }

        val state = viewModel.uiState.value as DownloadsUiState.Data
        assertEquals(listOf(2L), state.data.downloads.map { it.id })
        assertNull(state.data.availableUpdate)
        assertTrue(state.data.downloads.single().title.contains("Download 2"))

        collector.cancel()
    }

    @Test
    fun logShown_logsDownloadsScreen() {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(emptyList()),
            updateFlow = MutableStateFlow(null),
        )

        viewModel.logShown()

        verify(analyticsLogger).logScreen(AnalyticsEvents.Screens.DOWNLOADS)
    }

    @Test
    fun stopDownload_updatesStatusAndProgress_thenStopsWorker() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(emptyList()),
            updateFlow = MutableStateFlow(null),
        )

        viewModel.stopDownload(42L)
        advanceUntilIdle()

        verify(updateDownloadStatusByIdUseCase).invoke(42L, DownloadStatus.STOPPED)
        verify(updateDownloadProgressUseCase).invoke(
            id = 42L,
            progressPercent = 0F,
            bytesDownloaded = 0L,
            etaSeconds = null,
        )
        verify(stopDownloadUseCase).invoke(42L)
    }

    @Test
    fun stopDownload_stillStopsWorker_whenStatusUpdateFails() = runTest(testDispatcher) {
        `when`(updateDownloadStatusByIdUseCase.invoke(7L, DownloadStatus.STOPPED))
            .thenThrow(RuntimeException("update failed"))
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(emptyList()),
            updateFlow = MutableStateFlow(null),
        )

        viewModel.stopDownload(7L)
        advanceUntilIdle()

        verify(updateDownloadStatusByIdUseCase).invoke(7L, DownloadStatus.STOPPED)
        verify(updateDownloadProgressUseCase, never()).invoke(
            id = 7L,
            progressPercent = 0F,
            bytesDownloaded = 0L,
            etaSeconds = null,
        )
        verify(stopDownloadUseCase).invoke(7L)
    }

    @Test
    fun retryDownload_startsDownload() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(emptyList()),
            updateFlow = MutableStateFlow(null),
        )

        viewModel.retryDownload(11L)
        advanceUntilIdle()

        verify(startDownloadUseCase).invoke(11L)
    }

    @Test
    fun deleteDownloads_requestsDeletionForGivenIds() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(emptyList()),
            updateFlow = MutableStateFlow(null),
        )
        val ids = setOf(3L, 5L)

        viewModel.deleteDownloads(ids)
        advanceUntilIdle()

        verify(downloadUseCase.requestDeleteDownloadsUseCase).invoke(ids)
    }

    private fun createViewModel(
        downloadsFlow: MutableStateFlow<List<DownloadWithMetadata>>,
        updateFlow: MutableStateFlow<AvailableUpdate?>,
    ): DownloadsViewModel {
        `when`(getDownloadsWithMetadataAsFlowUseCase.invoke())
            .thenReturn(downloadsFlow)
        `when`(getAvailableUpdateAsFlowUseCase.invoke())
            .thenReturn(updateFlow)
        `when`(downloadWithMetadataUseCase.getDownloadsWithMetadataAsFlowUseCase)
            .thenReturn(getDownloadsWithMetadataAsFlowUseCase)
        `when`(availableUpdateUseCase.getAvailableUpdateAsFlowUseCase)
            .thenReturn(getAvailableUpdateAsFlowUseCase)
        `when`(downloadUseCase.updateDownloadStatusByIdUseCase)
            .thenReturn(updateDownloadStatusByIdUseCase)
        `when`(downloadUseCase.requestDeleteDownloadsUseCase)
            .thenReturn(requestDeleteDownloadsUseCase)
        `when`(downloadProgressUseCase.updateDownloadProgressUseCase)
            .thenReturn(updateDownloadProgressUseCase)

        return DownloadsViewModel(
            analyticsLogger = analyticsLogger,
            downloadUseCase = downloadUseCase,
            stopDownloadsUseCase = stopDownloadUseCase,
            startDownloadUseCase = startDownloadUseCase,
            availableUpdateUseCase = availableUpdateUseCase,
            downloadProgressUseCase = downloadProgressUseCase,
            downloadWithMetadataUseCase = downloadWithMetadataUseCase,
        )
    }

    private suspend fun waitForUiState(
        viewModel: DownloadsViewModel,
        predicate: (DownloadsUiState) -> Boolean,
    ) {
        withTimeout(2.seconds) {
            while (!predicate(viewModel.uiState.value)) {
                kotlinx.coroutines.yield()
            }
        }
    }

    private fun createDownload(
        id: Long,
        title: String,
    ) = DownloadWithMetadata(
        id = id,
        url = "https://example.com/$id",
        title = title,
        thumbnailFilePath = null,
        status = DownloadStatus.DOWNLOADING,
        seen = false,
        progressPercent = 50f,
        etaSeconds = 10L,
        bytesDownloaded = 500L,
        expectedBytes = 1000L,
        completedAtMillis = null,
    )
}
