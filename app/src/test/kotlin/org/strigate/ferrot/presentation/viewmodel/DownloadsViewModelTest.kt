package org.strigate.ferrot.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
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
import org.strigate.ferrot.domain.model.DownloadSwipeAction
import org.strigate.ferrot.domain.model.DownloadWithMetadata
import org.strigate.ferrot.domain.usecase.AvailableUpdateUseCase
import org.strigate.ferrot.domain.usecase.DownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.DownloadWithMetadataUseCase
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.StateUseCase
import org.strigate.ferrot.domain.usecase.availableupdate.GetAvailableUpdateAsFlowUseCase
import org.strigate.ferrot.domain.usecase.download.RequestDeletePendingDownloadsDelayedUseCase
import org.strigate.ferrot.domain.usecase.download.RequestDeletePendingDownloadsImmediateUseCase
import org.strigate.ferrot.domain.usecase.download.StartDownloadUseCase
import org.strigate.ferrot.domain.usecase.download.StopDownloadUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadStatusUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadsPendingDeleteUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadsSeenUseCase
import org.strigate.ferrot.domain.usecase.downloadprogress.UpdateDownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.downloadwithmetadata.GetDownloadsWithMetadataAsFlowUseCase
import org.strigate.ferrot.domain.usecase.notifications.ClearNotificationsByDownloadIdUseCase
import org.strigate.ferrot.domain.usecase.settings.GetLeftSwipeActionSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.GetRightSwipeActionSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.state.GetArchivedDownloadsGridLayoutEnabledUseCase
import org.strigate.ferrot.domain.usecase.state.GetDownloadsGridLayoutEnabledUseCase
import org.strigate.ferrot.domain.usecase.state.ToggleArchivedDownloadsGridLayoutEnabledUseCase
import org.strigate.ferrot.domain.usecase.state.ToggleDownloadsGridLayoutEnabledUseCase
import org.strigate.ferrot.presentation.Screen
import org.strigate.ferrot.presentation.event.DownloadsEvent
import org.strigate.ferrot.presentation.model.DownloadSwipeActionUiData
import org.strigate.ferrot.presentation.state.DownloadsUiState
import org.strigate.ferrot.test.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsViewModelTest {
    private lateinit var autoCloseable: AutoCloseable

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val testDispatcher: TestDispatcher = mainDispatcherRule.testDispatcher

    private val viewModels = mutableListOf<DownloadsViewModel>()

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

    @Mock
    private lateinit var stateUseCase: StateUseCase

    @Mock
    private lateinit var getDownloadsGridLayoutEnabledUseCase: GetDownloadsGridLayoutEnabledUseCase

    @Mock
    private lateinit var toggleDownloadsGridLayoutEnabledUseCase: ToggleDownloadsGridLayoutEnabledUseCase

    @Mock
    private lateinit var getArchivedDownloadsGridLayoutEnabledUseCase: GetArchivedDownloadsGridLayoutEnabledUseCase

    @Mock
    private lateinit var toggleArchivedDownloadsGridLayoutEnabledUseCase: ToggleArchivedDownloadsGridLayoutEnabledUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun uiState_exposesMappedDownloadsAndAvailableUpdate() = runTest(testDispatcher) {
        val downloadsFlow = MutableStateFlow(
            value = listOf(
                createDownload(id = 1L, title = "Download 1"),
                createDownload(id = 2L, title = "Download 2"),
            ),
        )
        val updateFlow = MutableStateFlow<AvailableUpdate?>(
            value = AvailableUpdate(
                tag = "v1.2.3",
                localFilePath = "/tmp/update.apk",
            ),
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
            data.data.downloads.size == 2
                    && data.data.availableUpdate?.tag == "v1.2.3"
                    && data.data.leftSwipeAction == DownloadSwipeActionUiData.ARCHIVE
                    && data.data.rightSwipeAction == DownloadSwipeActionUiData.DELETE
        }

        val state = viewModel.uiState.value as DownloadsUiState.Data
        assertEquals(listOf(1L, 2L), state.data.downloads.map { it.id })
        assertEquals("Download 1", state.data.downloads[0].title)
        assertEquals("v1.2.3", state.data.availableUpdate?.tag)
        assertEquals("/tmp/update.apk", state.data.availableUpdate?.localFilePath)
        assertTrue(state.data.pendingDeleteIds.isEmpty())
        assertTrue(state.data.retryFailedDownloadIds.isEmpty())
        assertEquals(DownloadSwipeActionUiData.ARCHIVE, state.data.leftSwipeAction)
        assertEquals(DownloadSwipeActionUiData.DELETE, state.data.rightSwipeAction)

        collector.cancel()
    }

    @Test
    fun toggleGridLayoutEnabled_togglesLayoutForCurrentPage() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(emptyList()),
            updateFlow = MutableStateFlow(null),
        )

        viewModel.toggleGridLayoutEnabled()
        advanceUntilIdle()

        verify(toggleDownloadsGridLayoutEnabledUseCase)
            .invoke()

        val archivedViewModel = createViewModel(
            downloadsFlow = MutableStateFlow(emptyList()),
            updateFlow = MutableStateFlow(null),
            archived = true,
        )
        archivedViewModel.toggleGridLayoutEnabled()
        advanceUntilIdle()

        verify(toggleArchivedDownloadsGridLayoutEnabledUseCase)
            .invoke()
    }

    @Test
    fun uiState_updatesWhenGridLayoutPreferenceChanges() = runTest(testDispatcher) {
        val gridLayoutEnabledFlow = MutableStateFlow(false)
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(emptyList()),
            updateFlow = MutableStateFlow(null),
            gridLayoutEnabledFlow = gridLayoutEnabledFlow,
        )
        val collector = backgroundScope.launch {
            viewModel.uiState.collect()
        }
        waitForUiState(viewModel) { it is DownloadsUiState.Data }

        gridLayoutEnabledFlow.value = true
        waitForUiState(viewModel) {
            (it as? DownloadsUiState.Data)?.data?.gridLayoutEnabled == true
        }

        assertTrue((viewModel.uiState.value as DownloadsUiState.Data).data.gridLayoutEnabled)
        collector.cancel()
    }

    @Test
    fun uiState_usesArchivedGridLayoutPreferenceForArchivedPage() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(emptyList()),
            updateFlow = MutableStateFlow(null),
            archivedGridLayoutEnabledFlow = MutableStateFlow(true),
            archived = true,
        )
        val collector = backgroundScope.launch {
            viewModel.uiState.collect()
        }
        waitForUiState(viewModel) { it is DownloadsUiState.Data }

        waitForUiState(viewModel) {
            (it as? DownloadsUiState.Data)?.data?.gridLayoutEnabled == true
        }

        assertTrue((viewModel.uiState.value as DownloadsUiState.Data).data.gridLayoutEnabled)
        collector.cancel()
    }

    @Test
    fun uiState_hidesPendingDelete_andExposesFlag() = runTest(testDispatcher) {
        val downloadsFlow = MutableStateFlow(
            value = listOf(
                createDownload(id = 1L, title = "Visible Download"),
                createDownload(id = 2L, title = "Pending Delete", pendingDelete = true),
            ),
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
        assertTrue(state.data.retryFailedDownloadIds.isEmpty())

        collector.cancel()
    }

    @Test
    fun updateSearchQuery_trimsInputAndFiltersDownloadsByTitle() = runTest(testDispatcher) {
        val downloadsFlow = MutableStateFlow(
            value = listOf(
                createDownload(id = 1L, title = "Download 1"),
                createDownload(id = 2L, title = "Download 2"),
            ),
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
        viewModel.updateSearchQuery(longQuery)

        assertEquals(100, viewModel.searchQuery.value.length)

        viewModel.updateSearchQuery("download 2")
        assertEquals("download 2", viewModel.searchQuery.value)

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
    fun stopAllDownloads_respectsSearchAndStopsOnlyActiveDownloads() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(
                value = listOf(
                    createDownload(id = 1L, title = "Queued", status = DownloadStatus.QUEUED),
                    createDownload(
                        id = 2L,
                        title = "Downloading",
                        status = DownloadStatus.DOWNLOADING,
                    ),
                    createDownload(id = 3L, title = "Failed", status = DownloadStatus.FAILED),
                    createDownload(id = 4L, title = "Stopped", status = DownloadStatus.STOPPED),
                ),
            ),
            updateFlow = MutableStateFlow(null),
        )

        val collector = backgroundScope.launch {
            viewModel.uiState.collect()
        }
        waitForUiState(viewModel) { it is DownloadsUiState.Data }
        viewModel.updateSearchQuery("Queued")
        waitForUiState(viewModel) { state ->
            (state as? DownloadsUiState.Data)?.data?.downloads?.map { it.id } == listOf(1L)
        }

        viewModel.stopAllDownloads()
        advanceUntilIdle()

        verify(updateDownloadStatusUseCase)
            .invoke(1L, DownloadStatus.STOPPED)
        verify(updateDownloadStatusUseCase, never())
            .invoke(2L, DownloadStatus.STOPPED)
        verify(updateDownloadStatusUseCase, never())
            .invoke(3L, DownloadStatus.STOPPED)
        verify(updateDownloadStatusUseCase, never())
            .invoke(4L, DownloadStatus.STOPPED)
        verify(stopDownloadUseCase)
            .invoke(1L)
        verify(stopDownloadUseCase, never())
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

        verify(analyticsLogger)
            .logEvent(AnalyticsEvents.DOWNLOAD_RETRY)
        verify(startDownloadUseCase)
            .invoke(11L)
    }

    @Test
    fun retryFailedDownloads_startsOnlyFailedDownloads() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(
                value = listOf(
                    createDownload(id = 1L, title = "Failed", status = DownloadStatus.FAILED),
                    createDownload(id = 2L, title = "Stopped", status = DownloadStatus.STOPPED),
                    createDownload(id = 3L, title = "Completed", status = DownloadStatus.COMPLETED),
                    createDownload(id = 4L, title = "Failed 2", status = DownloadStatus.FAILED),
                ),
            ),
            updateFlow = MutableStateFlow(null),
        )

        val collector = backgroundScope.launch {
            viewModel.uiState.collect()
        }
        waitForUiState(viewModel) { it is DownloadsUiState.Data }

        viewModel.retryFailedDownloads()
        advanceUntilIdle()

        val state = viewModel.uiState.value as DownloadsUiState.Data
        assertEquals(setOf(1L, 4L), state.data.retryFailedDownloadIds)
        verify(analyticsLogger)
            .logEvent(AnalyticsEvents.DOWNLOADS_RETRY)
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
    fun retryFailedDownloads_skipsPendingDelete() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(
                value = listOf(
                    createDownload(
                        id = 1L,
                        title = "Failed",
                        status = DownloadStatus.FAILED,
                    ),
                    createDownload(
                        id = 2L,
                        title = "Pending Delete",
                        status = DownloadStatus.FAILED,
                        pendingDelete = true,
                    ),
                ),
            ),
            updateFlow = MutableStateFlow(null),
        )

        val collector = backgroundScope.launch {
            viewModel.uiState.collect()
        }
        waitForUiState(viewModel) { state ->
            val data = state as? DownloadsUiState.Data ?: return@waitForUiState false
            data.data.pendingDeleteIds == setOf(2L)
                    && data.data.retryFailedDownloadIds == setOf(1L)
        }

        viewModel.retryFailedDownloads()
        advanceUntilIdle()

        verify(analyticsLogger)
            .logEvent(AnalyticsEvents.DOWNLOADS_RETRY)
        verify(startDownloadUseCase)
            .invoke(1L)
        verify(startDownloadUseCase, never())
            .invoke(2L)

        collector.cancel()
    }

    @Test
    fun retryFailedDownloads_respectsSearch() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(
                value = listOf(
                    createDownload(
                        id = 1L,
                        title = "Alpha Failed",
                        status = DownloadStatus.FAILED,
                    ),
                    createDownload(
                        id = 2L,
                        title = "Beta Failed",
                        status = DownloadStatus.FAILED,
                    ),
                    createDownload(
                        id = 3L,
                        title = "Beta Stopped",
                        status = DownloadStatus.STOPPED,
                    ),
                ),
            ),
            updateFlow = MutableStateFlow(null),
        )

        val collector = backgroundScope.launch {
            viewModel.uiState.collect()
        }
        waitForUiState(viewModel) { it is DownloadsUiState.Data }

        viewModel.updateSearchQuery("Beta")
        waitForUiState(viewModel) { state ->
            val data = state as? DownloadsUiState.Data ?: return@waitForUiState false
            data.data.downloads.map { it.id } == listOf(2L, 3L)
                    && data.data.retryFailedDownloadIds == setOf(2L)
        }

        viewModel.retryFailedDownloads()
        advanceUntilIdle()

        verify(analyticsLogger)
            .logEvent(AnalyticsEvents.DOWNLOADS_RETRY)
        verify(startDownloadUseCase)
            .invoke(2L)
        verify(startDownloadUseCase, never())
            .invoke(1L)
        verify(startDownloadUseCase, never())
            .invoke(3L)
        collector.cancel()
    }

    @Test
    fun toggleDownloadsSeen_marksAllSeen_whenAnySelectedIsUnseen() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(
                value = listOf(
                    createDownload(id = 1L, title = "Seen", seen = true),
                    createDownload(id = 2L, title = "Unseen", seen = false),
                    createDownload(id = 3L, title = "Seen 2", seen = true),
                ),
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
    fun toggleDownloadsSeen_ignoresIdsOutsideTheCurrentDownloads() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(
                value = listOf(
                    createDownload(id = 1L, title = "Seen", seen = true),
                    createDownload(id = 2L, title = "Unseen", seen = false),
                ),
            ),
            updateFlow = MutableStateFlow(null),
        )

        val collector = backgroundScope.launch {
            viewModel.uiState.collect()
        }
        waitForUiState(viewModel) { it is DownloadsUiState.Data }

        viewModel.toggleDownloadsSeen(setOf(1L, 2L, 99L))
        advanceUntilIdle()

        verify(updateDownloadsSeenUseCase)
            .invoke(setOf(1L, 2L), true)
        verify(clearNotificationsByDownloadIdUseCase, never())
            .invoke(99L)
        collector.cancel()
    }

    @Test
    fun toggleDownloadsSeen_marksAllUnseen_whenAllSelectedAreSeen() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(
                value = listOf(
                    createDownload(id = 1L, title = "Seen", seen = true),
                    createDownload(id = 2L, title = "Seen 2", seen = true),
                ),
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
                value = AvailableUpdate(
                    tag = "v1.2.3",
                    localFilePath = "/tmp/update.apk",
                ),
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
                value = AvailableUpdate(
                    tag = "v1.2.3",
                    localFilePath = null,
                ),
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

    @Test
    fun uiState_startsInArchivedModeWithoutActiveDownloads() = runTest(testDispatcher) {
        val archivedQueryReady = CompletableDeferred<Unit>()
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(listOf(createDownload(id = 1L, title = "Active"))),
            archivedDownloadsFlow = flow {
                archivedQueryReady.await()
                emit(listOf(createDownload(id = 2L, title = "Archived").copy(archived = true)))
            },
            updateFlow = MutableStateFlow(null),
            gridLayoutEnabledFlow = MutableStateFlow(false),
            archivedGridLayoutEnabledFlow = MutableStateFlow(true),
            archived = true,
        )
        val states = mutableListOf<DownloadsUiState>()
        val collector = backgroundScope.launch { viewModel.uiState.collect { states += it } }
        runCurrent()

        assertTrue(viewModel.isArchived)
        assertEquals(DownloadsUiState.Loading, viewModel.uiState.value)
        verify(getDownloadsWithMetadataAsFlowUseCase, never()).invoke(false)
        verify(getDownloadsGridLayoutEnabledUseCase, never()).invoke()
        verify(getAvailableUpdateAsFlowUseCase, never()).invoke()

        archivedQueryReady.complete(Unit)
        runCurrent()
        val dataStates = states.filterIsInstance<DownloadsUiState.Data>()
        assertTrue(dataStates.isNotEmpty())
        dataStates.forEach { state ->
            assertEquals(listOf(2L), state.data.downloads.map { it.id })
            assertTrue(state.data.gridLayoutEnabled)
            assertNull(state.data.availableUpdate)
        }
        collector.cancel()
    }

    @Test
    fun uiState_displaysDownloadsBeforeOptionalFlowsEmit() = runTest(testDispatcher) {
        val preferencesReady = CompletableDeferred<Unit>()
        val updateReady = CompletableDeferred<Unit>()
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(listOf(createDownload(id = 1L, title = "Download"))),
            updateFlow = flow {
                updateReady.await()
                emit(AvailableUpdate(tag = "v2", localFilePath = "/tmp/update.apk"))
            },
            gridLayoutEnabledFlow = flow {
                preferencesReady.await()
                emit(true)
            },
            leftSwipeActionFlow = flow {
                preferencesReady.await()
                emit(DownloadSwipeAction.ARCHIVE)
            },
            rightSwipeActionFlow = flow {
                preferencesReady.await()
                emit(DownloadSwipeAction.DELETE)
            },
        )
        val collector = backgroundScope.launch { viewModel.uiState.collect() }
        runCurrent()

        val initial = (viewModel.uiState.value as DownloadsUiState.Data).data
        assertEquals(listOf(1L), initial.downloads.map { it.id })
        assertNull(initial.availableUpdate)
        assertFalse(initial.gridLayoutEnabled)
        assertEquals(DownloadSwipeActionUiData.NONE, initial.leftSwipeAction)
        assertEquals(DownloadSwipeActionUiData.NONE, initial.rightSwipeAction)

        preferencesReady.complete(Unit)
        updateReady.complete(Unit)
        runCurrent()
        val loaded = (viewModel.uiState.value as DownloadsUiState.Data).data
        assertEquals("v2", loaded.availableUpdate?.tag)
        assertTrue(loaded.gridLayoutEnabled)
        assertEquals(DownloadSwipeActionUiData.ARCHIVE, loaded.leftSwipeAction)
        assertEquals(DownloadSwipeActionUiData.DELETE, loaded.rightSwipeAction)
        collector.cancel()
    }

    @Test
    fun uiState_retainsDataWhileRestartingStoppedQuery() = runTest(testDispatcher) {
        val downloads = MutableStateFlow(listOf(createDownload(id = 1L, title = "Old")))
        val restartedQueryReady = CompletableDeferred<Unit>()
        var subscriptions = 0
        val viewModel = createViewModel(
            downloadsFlow = flow {
                subscriptions++
                if (subscriptions > 1) restartedQueryReady.await()
                emitAll(downloads)
            },
            updateFlow = MutableStateFlow(null),
        )
        val firstCollector = backgroundScope.launch { viewModel.uiState.collect() }
        runCurrent()
        assertEquals(
            "Old",
            (viewModel.uiState.value as DownloadsUiState.Data).data.downloads.single().title
        )

        firstCollector.cancelAndJoin()
        advanceTimeBy(5_001)
        runCurrent()
        val cachedState = viewModel.uiState.value
        assertTrue(cachedState is DownloadsUiState.Data)
        downloads.value = listOf(createDownload(id = 1L, title = "Fresh"))
        val resumedStates = mutableListOf<DownloadsUiState>()
        val secondCollector =
            backgroundScope.launch { viewModel.uiState.collect { resumedStates += it } }
        runCurrent()
        assertEquals(2, subscriptions)
        assertEquals(cachedState, viewModel.uiState.value)

        restartedQueryReady.complete(Unit)
        runCurrent()
        assertEquals(
            "Fresh",
            (viewModel.uiState.value as DownloadsUiState.Data).data.downloads.single().title
        )
        assertTrue(resumedStates.all { it is DownloadsUiState.Data })
        secondCollector.cancel()
    }

    @Test
    fun uiState_keepsQueryAndResultsInTheSameSnapshot() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadsFlow = MutableStateFlow(
                listOf(
                    createDownload(id = 1L, title = "Alpha"),
                    createDownload(id = 2L, title = "Beta")
                ),
            ),
            updateFlow = MutableStateFlow(null),
        )
        val collector = backgroundScope.launch { viewModel.uiState.collect() }
        runCurrent()
        viewModel.updateSearchQuery("Beta")

        // Input updates immediately, while the displayed results retain their original query.
        assertEquals("Beta", viewModel.searchQuery.value)
        val previous = (viewModel.uiState.value as DownloadsUiState.Data).data
        assertEquals("", previous.searchQuery)
        assertEquals(listOf(1L, 2L), previous.downloads.map { it.id })

        runCurrent()
        val filtered = (viewModel.uiState.value as DownloadsUiState.Data).data
        assertEquals("Beta", filtered.searchQuery)
        assertEquals(listOf(2L), filtered.downloads.map { it.id })
        viewModel.updateSearchQuery("Missing")
        runCurrent()
        val empty = (viewModel.uiState.value as DownloadsUiState.Data).data
        assertEquals("Missing", empty.searchQuery)
        assertTrue(empty.downloads.isEmpty())
        collector.cancel()
    }

    @After
    fun tearDown() = runTest(testDispatcher) {
        viewModels.forEach { it.viewModelScope.coroutineContext.job.cancelAndJoin() }
        autoCloseable.close()
    }

    private fun createViewModel(
        downloadsFlow: Flow<List<DownloadWithMetadata>>,
        updateFlow: Flow<AvailableUpdate?>,
        gridLayoutEnabledFlow: Flow<Boolean> = MutableStateFlow(false),
        archivedGridLayoutEnabledFlow: Flow<Boolean> = MutableStateFlow(false),
        archivedDownloadsFlow: Flow<List<DownloadWithMetadata>> = downloadsFlow,
        leftSwipeActionFlow: Flow<DownloadSwipeAction> = MutableStateFlow(DownloadSwipeAction.ARCHIVE),
        rightSwipeActionFlow: Flow<DownloadSwipeAction> = MutableStateFlow(DownloadSwipeAction.DELETE),
        archived: Boolean = false,
    ): DownloadsViewModel {
        `when`(getDownloadsWithMetadataAsFlowUseCase.invoke(false))
            .thenReturn(downloadsFlow)
        `when`(getDownloadsWithMetadataAsFlowUseCase.invoke(true))
            .thenReturn(archivedDownloadsFlow)
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
            .thenReturn(leftSwipeActionFlow)
        `when`(getRightSwipeActionSettingAsFlowUseCase.invoke())
            .thenReturn(rightSwipeActionFlow)
        `when`(settingsUseCase.getLeftSwipeActionSettingAsFlowUseCase)
            .thenReturn(getLeftSwipeActionSettingAsFlowUseCase)
        `when`(settingsUseCase.getRightSwipeActionSettingAsFlowUseCase)
            .thenReturn(getRightSwipeActionSettingAsFlowUseCase)
        `when`(getDownloadsGridLayoutEnabledUseCase.invoke())
            .thenReturn(gridLayoutEnabledFlow)
        `when`(stateUseCase.getDownloadsGridLayoutEnabledUseCase)
            .thenReturn(getDownloadsGridLayoutEnabledUseCase)
        `when`(stateUseCase.toggleDownloadsGridLayoutEnabledUseCase)
            .thenReturn(toggleDownloadsGridLayoutEnabledUseCase)
        `when`(getArchivedDownloadsGridLayoutEnabledUseCase.invoke())
            .thenReturn(archivedGridLayoutEnabledFlow)
        `when`(stateUseCase.getArchivedDownloadsGridLayoutEnabledUseCase)
            .thenReturn(getArchivedDownloadsGridLayoutEnabledUseCase)
        `when`(stateUseCase.toggleArchivedDownloadsGridLayoutEnabledUseCase)
            .thenReturn(toggleArchivedDownloadsGridLayoutEnabledUseCase)

        return DownloadsViewModel(
            defaultDispatcher = testDispatcher,
            savedStateHandle = SavedStateHandle(mapOf(Screen.ARG_ARCHIVED to archived)),
            analyticsLogger = analyticsLogger,
            downloadUseCase = downloadUseCase,
            stopDownloadsUseCase = stopDownloadUseCase,
            startDownloadUseCase = startDownloadUseCase,
            availableUpdateUseCase = availableUpdateUseCase,
            downloadProgressUseCase = downloadProgressUseCase,
            downloadWithMetadataUseCase = downloadWithMetadataUseCase,
            clearNotificationsByDownloadIdUseCase = clearNotificationsByDownloadIdUseCase,
            settingsUseCase = settingsUseCase,
            stateUseCase = stateUseCase,
        ).also { viewModels += it }
    }

    private suspend fun waitForUiState(
        viewModel: DownloadsViewModel,
        predicate: (DownloadsUiState) -> Boolean,
    ) {
        viewModel.uiState.first(predicate)
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
