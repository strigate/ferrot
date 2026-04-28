package org.strigate.ferrot.presentation.viewmodel

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
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
import org.strigate.ferrot.domain.model.DownloadSwipeAction
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.model.DownloadWithMetadata
import org.strigate.ferrot.domain.usecase.AvailableUpdateUseCase
import org.strigate.ferrot.domain.usecase.DownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.DownloadWithMetadataUseCase
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.availableupdate.GetAvailableUpdateAsFlowUseCase
import org.strigate.ferrot.domain.usecase.download.RequestDeletePendingDownloadsDelayedUseCase
import org.strigate.ferrot.domain.usecase.download.RequestDeletePendingDownloadsImmediateUseCase
import org.strigate.ferrot.domain.usecase.download.StartDownloadUseCase
import org.strigate.ferrot.domain.usecase.download.StopDownloadUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadStatusUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadsPendingDeleteUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadsSeenUseCase
import org.strigate.ferrot.domain.usecase.settings.GetLeftSwipeActionSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.GetRightSwipeActionSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.downloadprogress.UpdateDownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.downloadwithmetadata.GetDownloadsWithMetadataAsFlowUseCase
import org.strigate.ferrot.domain.usecase.notifications.ClearNotificationsByDownloadIdUseCase
import org.strigate.ferrot.presentation.event.DownloadsEvent
import org.strigate.ferrot.presentation.model.DownloadSwipeActionUiData
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
    private lateinit var updateDownloadStatusUseCase: UpdateDownloadStatusUseCase

    @Mock
    private lateinit var updateDownloadProgressUseCase: UpdateDownloadProgressUseCase

    @Mock
    private lateinit var requestDeletePendingDownloadsDelayedUseCase: RequestDeletePendingDownloadsDelayedUseCase

    @Mock
    private lateinit var requestDeletePendingDownloadsImmediateUseCase: RequestDeletePendingDownloadsImmediateUseCase

    @Mock
    private lateinit var updateDownloadsPendingDeleteUseCase: UpdateDownloadsPendingDeleteUseCase

    @Mock
    private lateinit var updateDownloadsSeenUseCase: UpdateDownloadsSeenUseCase

    @Mock
    private lateinit var clearNotificationsByDownloadIdUseCase: ClearNotificationsByDownloadIdUseCase

    @Mock
    private lateinit var settingsUseCase: SettingsUseCase

    @Mock
    private lateinit var getLeftSwipeActionSettingAsFlowUseCase: GetLeftSwipeActionSettingAsFlowUseCase

    @Mock
    private lateinit var getRightSwipeActionSettingAsFlowUseCase: GetRightSwipeActionSettingAsFlowUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
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
        assertTrue(state.data.pendingDeleteIds.isEmpty())
        assertEquals(DownloadSwipeActionUiData.ARCHIVE, state.data.leftSwipeAction)
        assertEquals(DownloadSwipeActionUiData.DELETE, state.data.rightSwipeAction)

        collector.cancel()
    }

    @Test
    fun uiState_hidesPendingDelete_andExposesFlag() = runTest(testDispatcher) {
        val downloadsFlow = MutableStateFlow(
            listOf(
                createDownload(id = 1L, title = "Visible Download"),
                createDownload(id = 2L, title = "Pending Delete", pendingDelete = true),
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
        waitForUiState(viewModel) { state ->
            val data = state as? DownloadsUiState.Data ?: return@waitForUiState false
            data.data.downloads.size == 1 && data.data.pendingDeleteIds.isNotEmpty()
        }

        val state = viewModel.uiState.value as DownloadsUiState.Data
        assertEquals(listOf(1L), state.data.downloads.map { it.id })
        assertEquals(setOf(2L), state.data.pendingDeleteIds)

        collector.cancel()
    }

    @Test
    fun updateSearchQuery_trimsInputAndFiltersDownloadsByTitle() = runTest(testDispatcher) {
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

        verify(analyticsLogger)
            .logScreen(AnalyticsEvents.Screens.DOWNLOADS)
    }

    @Test
    fun stopDownload_updatesStatusAndProgress_thenStopsWorker() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(emptyList()),
            updateFlow = MutableStateFlow(null),
        )

        viewModel.stopDownload(42L)
        advanceUntilIdle()

        verify(updateDownloadStatusUseCase)
            .invoke(42L, DownloadStatus.STOPPED)
        verify(updateDownloadProgressUseCase)
            .invoke(
                id = 42L,
                progressPercent = 0F,
                bytesDownloaded = 0L,
                etaSeconds = null,
            )
        verify(stopDownloadUseCase)
            .invoke(42L)
    }

    @Test
    fun stopDownload_stillStopsWorker_whenStatusUpdateFails() = runTest(testDispatcher) {
        `when`(updateDownloadStatusUseCase.invoke(7L, DownloadStatus.STOPPED))
            .thenThrow(RuntimeException("update failed"))

        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(emptyList()),
            updateFlow = MutableStateFlow(null),
        )

        viewModel.stopDownload(7L)
        advanceUntilIdle()

        verify(updateDownloadStatusUseCase)
            .invoke(7L, DownloadStatus.STOPPED)
        verify(updateDownloadProgressUseCase, never())
            .invoke(
                id = 7L,
                progressPercent = 0F,
                bytesDownloaded = 0L,
                etaSeconds = null,
            )
        verify(stopDownloadUseCase)
            .invoke(7L)
    }

    @Test
    fun stopAllDownloads_stopsOnlyActiveDownloads() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(
                listOf(
                    createDownload(id = 1L, title = "Queued", status = DownloadStatus.QUEUED),
                    createDownload(
                        id = 2L,
                        title = "Downloading",
                        status = DownloadStatus.DOWNLOADING
                    ),
                    createDownload(id = 3L, title = "Failed", status = DownloadStatus.FAILED),
                    createDownload(id = 4L, title = "Stopped", status = DownloadStatus.STOPPED),
                )
            ),
            updateFlow = MutableStateFlow(null),
        )

        val collector = backgroundScope.launch {
            viewModel.uiState.collect()
        }
        waitForUiState(viewModel) { it is DownloadsUiState.Data }

        viewModel.stopAllDownloads()
        advanceUntilIdle()

        verify(updateDownloadStatusUseCase)
            .invoke(1L, DownloadStatus.STOPPED)
        verify(updateDownloadStatusUseCase)
            .invoke(2L, DownloadStatus.STOPPED)
        verify(updateDownloadStatusUseCase, never())
            .invoke(3L, DownloadStatus.STOPPED)
        verify(updateDownloadStatusUseCase, never())
            .invoke(4L, DownloadStatus.STOPPED)
        verify(stopDownloadUseCase)
            .invoke(1L)
        verify(stopDownloadUseCase)
            .invoke(2L)
        verify(stopDownloadUseCase, never())
            .invoke(3L)
        verify(stopDownloadUseCase, never())
            .invoke(4L)

        collector.cancel()
    }

    @Test
    fun retryDownload_startsDownload() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(emptyList()),
            updateFlow = MutableStateFlow(null),
        )

        viewModel.retryDownload(11L)
        advanceUntilIdle()

        verify(startDownloadUseCase)
            .invoke(11L)
    }

    @Test
    fun retryFailedDownloads_startsOnlyFailedDownloads() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(
                listOf(
                    createDownload(id = 1L, title = "Failed", status = DownloadStatus.FAILED),
                    createDownload(id = 2L, title = "Stopped", status = DownloadStatus.STOPPED),
                    createDownload(id = 3L, title = "Completed", status = DownloadStatus.COMPLETED),
                    createDownload(id = 4L, title = "Failed 2", status = DownloadStatus.FAILED),
                )
            ),
            updateFlow = MutableStateFlow(null),
        )

        val collector = backgroundScope.launch {
            viewModel.uiState.collect()
        }
        waitForUiState(viewModel) { it is DownloadsUiState.Data }

        viewModel.retryFailedDownloads()
        advanceUntilIdle()

        verify(startDownloadUseCase)
            .invoke(1L)
        verify(startDownloadUseCase)
            .invoke(4L)
        verify(startDownloadUseCase, never())
            .invoke(2L)
        verify(startDownloadUseCase, never())
            .invoke(3L)
        collector.cancel()
    }

    @Test
    fun toggleDownloadsSeen_marksAllSeen_whenAnySelectedIsUnseen() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(
                listOf(
                    createDownload(id = 1L, title = "Seen", seen = true),
                    createDownload(id = 2L, title = "Unseen", seen = false),
                    createDownload(id = 3L, title = "Seen 2", seen = true),
                )
            ),
            updateFlow = MutableStateFlow(null),
        )

        val collector = backgroundScope.launch {
            viewModel.uiState.collect()
        }
        waitForUiState(viewModel) { it is DownloadsUiState.Data }

        viewModel.toggleDownloadsSeen(setOf(1L, 2L, 3L))
        advanceUntilIdle()

        verify(updateDownloadsSeenUseCase)
            .invoke(setOf(1L, 2L, 3L), true)
        verify(clearNotificationsByDownloadIdUseCase)
            .invoke(1L)
        verify(clearNotificationsByDownloadIdUseCase)
            .invoke(2L)
        verify(clearNotificationsByDownloadIdUseCase)
            .invoke(3L)
        collector.cancel()
    }

    @Test
    fun toggleDownloadsSeen_marksAllUnseen_whenAllSelectedAreSeen() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(
                listOf(
                    createDownload(id = 1L, title = "Seen", seen = true),
                    createDownload(id = 2L, title = "Seen 2", seen = true),
                )
            ),
            updateFlow = MutableStateFlow(null),
        )

        val collector = backgroundScope.launch {
            viewModel.uiState.collect()
        }
        waitForUiState(viewModel) { it is DownloadsUiState.Data }

        viewModel.toggleDownloadsSeen(setOf(1L, 2L))
        advanceUntilIdle()

        verify(updateDownloadsSeenUseCase)
            .invoke(setOf(1L, 2L), false)
        verify(clearNotificationsByDownloadIdUseCase, never())
            .invoke(1L)
        verify(clearNotificationsByDownloadIdUseCase, never())
            .invoke(2L)
        collector.cancel()
    }

    @Test
    fun markDownloadsPendingDelete_marksIds_andRequestsDelayedWorker() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(emptyList()),
            updateFlow = MutableStateFlow(null),
        )
        val ids = setOf(7L)

        viewModel.markDownloadsPendingDelete(ids)
        advanceUntilIdle()

        verify(updateDownloadsPendingDeleteUseCase)
            .invoke(ids, true)
        verify(requestDeletePendingDownloadsDelayedUseCase)
            .invoke()
    }

    @Test
    fun markDownloadsPendingDelete_false_skipsDelayedWorker() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(emptyList()),
            updateFlow = MutableStateFlow(null),
        )
        val ids = setOf(7L)

        viewModel.markDownloadsPendingDelete(ids, pendingDelete = false)
        advanceUntilIdle()

        verify(updateDownloadsPendingDeleteUseCase)
            .invoke(ids, false)
        verify(requestDeletePendingDownloadsDelayedUseCase, never())
            .invoke()
    }

    @Test
    fun requestDeletePendingDownloadsImmediate_requestsImmediateWorker() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(emptyList()),
            updateFlow = MutableStateFlow(null),
        )

        viewModel.requestDeletePendingDownloadsImmediate()
        advanceUntilIdle()

        verify(requestDeletePendingDownloadsImmediateUseCase)
            .invoke()
    }

    @Test
    fun installAvailableUpdate_emitsInstallEvent() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(emptyList()),
            updateFlow = MutableStateFlow(
                AvailableUpdate(
                    tag = "v1.2.3",
                    localFilePath = "/tmp/update.apk",
                )
            ),
        )

        val collector = backgroundScope.launch {
            viewModel.uiState.collect()
        }
        waitForUiState(viewModel) { state ->
            val data = state as? DownloadsUiState.Data ?: return@waitForUiState false
            data.data.availableUpdate?.localFilePath == "/tmp/update.apk"
        }

        val eventDeferred = backgroundScope.async { viewModel.events.first() }

        viewModel.installAvailableUpdate()
        advanceUntilIdle()

        assertEquals(
            DownloadsEvent.InstallUpdate("/tmp/update.apk"),
            eventDeferred.await(),
        )
        collector.cancel()
    }

    @Test
    fun installAvailableUpdate_doesNothingWithoutLocalFilePath() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(emptyList()),
            updateFlow = MutableStateFlow(
                AvailableUpdate(
                    tag = "v1.2.3",
                    localFilePath = null,
                )
            ),
        )

        val collector = backgroundScope.launch {
            viewModel.uiState.collect()
        }
        waitForUiState(viewModel) { it is DownloadsUiState.Data }

        var emittedEvent: DownloadsEvent? = null
        val eventCollector = backgroundScope.launch {
            viewModel.events.collect { event ->
                emittedEvent = event
            }
        }

        viewModel.installAvailableUpdate()
        advanceUntilIdle()

        assertNull(emittedEvent)
        eventCollector.cancel()
        collector.cancel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        autoCloseable.close()
    }

    private fun createViewModel(
        downloadsFlow: MutableStateFlow<List<DownloadWithMetadata>>,
        updateFlow: MutableStateFlow<AvailableUpdate?>,
    ): DownloadsViewModel {
        `when`(getDownloadsWithMetadataAsFlowUseCase.invoke(false))
            .thenReturn(downloadsFlow)
        `when`(getAvailableUpdateAsFlowUseCase.invoke())
            .thenReturn(updateFlow)
        `when`(downloadWithMetadataUseCase.getDownloadsWithMetadataAsFlowUseCase)
            .thenReturn(getDownloadsWithMetadataAsFlowUseCase)
        `when`(availableUpdateUseCase.getAvailableUpdateAsFlowUseCase)
            .thenReturn(getAvailableUpdateAsFlowUseCase)
        `when`(downloadUseCase.updateDownloadStatusUseCase)
            .thenReturn(updateDownloadStatusUseCase)
        `when`(downloadUseCase.updateDownloadsSeenUseCase)
            .thenReturn(updateDownloadsSeenUseCase)
        `when`(downloadUseCase.requestDeletePendingDownloadsDelayedUseCase)
            .thenReturn(requestDeletePendingDownloadsDelayedUseCase)
        `when`(downloadUseCase.requestDeletePendingDownloadsImmediateUseCase)
            .thenReturn(requestDeletePendingDownloadsImmediateUseCase)
        `when`(downloadUseCase.updateDownloadsPendingDeleteUseCase)
            .thenReturn(updateDownloadsPendingDeleteUseCase)
        `when`(downloadProgressUseCase.updateDownloadProgressUseCase)
            .thenReturn(updateDownloadProgressUseCase)
        `when`(getLeftSwipeActionSettingAsFlowUseCase.invoke())
            .thenReturn(MutableStateFlow(DownloadSwipeAction.ARCHIVE))
        `when`(getRightSwipeActionSettingAsFlowUseCase.invoke())
            .thenReturn(MutableStateFlow(DownloadSwipeAction.DELETE))
        `when`(settingsUseCase.getLeftSwipeActionSettingAsFlowUseCase)
            .thenReturn(getLeftSwipeActionSettingAsFlowUseCase)
        `when`(settingsUseCase.getRightSwipeActionSettingAsFlowUseCase)
            .thenReturn(getRightSwipeActionSettingAsFlowUseCase)

        return DownloadsViewModel(
            savedStateHandle = SavedStateHandle(),
            analyticsLogger = analyticsLogger,
            downloadUseCase = downloadUseCase,
            stopDownloadsUseCase = stopDownloadUseCase,
            startDownloadUseCase = startDownloadUseCase,
            availableUpdateUseCase = availableUpdateUseCase,
            downloadProgressUseCase = downloadProgressUseCase,
            downloadWithMetadataUseCase = downloadWithMetadataUseCase,
            clearNotificationsByDownloadIdUseCase = clearNotificationsByDownloadIdUseCase,
            settingsUseCase = settingsUseCase,
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
        url: String = "https://example.com/$id",
        status: DownloadStatus = DownloadStatus.DOWNLOADING,
        seen: Boolean = false,
        pendingDelete: Boolean = false,
    ) = DownloadWithMetadata(
        id = id,
        url = url,
        title = title,
        thumbnailFilePath = null,
        status = status,
        seen = seen,
        pendingDelete = pendingDelete,
        progressPercent = 50f,
        etaSeconds = 10L,
        bytesDownloaded = 500L,
        expectedBytes = 1000L,
        completedAtMillis = null,
    )
}
