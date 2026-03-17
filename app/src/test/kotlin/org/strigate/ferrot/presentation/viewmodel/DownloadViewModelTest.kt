package org.strigate.ferrot.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadAudio
import org.strigate.ferrot.domain.model.DownloadMediaType
import org.strigate.ferrot.domain.model.DownloadMetadata
import org.strigate.ferrot.domain.model.DownloadProgress
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.model.DownloadVideo
import org.strigate.ferrot.domain.usecase.DownloadAudioUseCase
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.DownloadVideoUseCase
import org.strigate.ferrot.domain.usecase.DownloadWithMetadataUseCase
import org.strigate.ferrot.domain.usecase.download.GetDownloadByIdAsFlowUseCase
import org.strigate.ferrot.domain.usecase.download.GetDownloadByIdUseCase
import org.strigate.ferrot.domain.usecase.download.RequestDeleteDownloadsUseCase
import org.strigate.ferrot.domain.usecase.download.StartDownloadUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadSeenByIdUseCase
import org.strigate.ferrot.domain.usecase.downloadaudio.GetDownloadAudioByDownloadIdAsFlowUseCase
import org.strigate.ferrot.domain.usecase.downloadmetadata.GetDownloadMetadataByIdAsFlowUseCase
import org.strigate.ferrot.domain.usecase.downloadprogress.GetDownloadProgressByDownloadIdAsFlowUseCase
import org.strigate.ferrot.domain.usecase.downloadvideo.GetDownloadVideoByDownloadIdAsFlowUseCase
import org.strigate.ferrot.domain.usecase.downloadwithmetadata.GetDownloadIdsWithMetadataAsFlowUseCase
import org.strigate.ferrot.domain.usecase.notifications.ClearNotificationsByDownloadIdUseCase
import org.strigate.ferrot.presentation.Screen
import org.strigate.ferrot.presentation.event.DownloadEvent
import org.strigate.ferrot.presentation.model.DownloadPageUiData
import org.strigate.ferrot.presentation.state.DownloadUiState
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadViewModelTest {
    private lateinit var autoCloseable: AutoCloseable
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var analyticsLogger: AnalyticsLogger

    @Mock
    private lateinit var downloadUseCase: DownloadUseCase

    @Mock
    private lateinit var downloadVideoUseCase: DownloadVideoUseCase

    @Mock
    private lateinit var downloadAudioUseCase: DownloadAudioUseCase

    @Mock
    private lateinit var downloadProgressUseCase: DownloadProgressUseCase

    @Mock
    private lateinit var downloadMetadataUseCase: DownloadMetadataUseCase

    @Mock
    private lateinit var clearNotificationsByDownloadIdUseCase: ClearNotificationsByDownloadIdUseCase

    @Mock
    private lateinit var startDownloadUseCase: StartDownloadUseCase

    @Mock
    private lateinit var downloadWithMetadataUseCase: DownloadWithMetadataUseCase

    @Mock
    private lateinit var getDownloadIdsWithMetadataAsFlowUseCase: GetDownloadIdsWithMetadataAsFlowUseCase

    @Mock
    private lateinit var getDownloadByIdAsFlowUseCase: GetDownloadByIdAsFlowUseCase

    @Mock
    private lateinit var getDownloadByIdUseCase: GetDownloadByIdUseCase

    @Mock
    private lateinit var updateDownloadSeenByIdUseCase: UpdateDownloadSeenByIdUseCase

    @Mock
    private lateinit var requestDeleteDownloadsUseCase: RequestDeleteDownloadsUseCase

    @Mock
    private lateinit var getDownloadVideoByDownloadIdAsFlowUseCase: GetDownloadVideoByDownloadIdAsFlowUseCase

    @Mock
    private lateinit var getDownloadAudioByDownloadIdAsFlowUseCase: GetDownloadAudioByDownloadIdAsFlowUseCase

    @Mock
    private lateinit var getDownloadMetadataByIdAsFlowUseCase: GetDownloadMetadataByIdAsFlowUseCase

    @Mock
    private lateinit var getDownloadProgressByDownloadIdAsFlowUseCase: GetDownloadProgressByDownloadIdAsFlowUseCase

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
    fun init_clearsNotificationsForInitialDownload() = runTest(testDispatcher) {
        createViewModel(initialId = 20L)
        advanceUntilIdle()

        verify(clearNotificationsByDownloadIdUseCase).invoke(20L)
    }

    @Test
    fun logShown_logsDownloadScreen() {
        val viewModel = createViewModel()

        viewModel.logShown()

        verify(analyticsLogger).logScreen(AnalyticsEvents.Screens.DOWNLOAD)
    }

    @Test
    fun constructor_requiresDownloadIdArgument() {
        try {
            DownloadViewModel(
                savedStateHandle = SavedStateHandle(),
                analyticsLogger = analyticsLogger,
                downloadUseCase = downloadUseCase,
                downloadVideoUseCase = downloadVideoUseCase,
                downloadAudioUseCase = downloadAudioUseCase,
                downloadProgressUseCase = downloadProgressUseCase,
                downloadMetadataUseCase = downloadMetadataUseCase,
                clearNotificationsByDownloadIdUseCase = clearNotificationsByDownloadIdUseCase,
                startDownloadUseCase = startDownloadUseCase,
                downloadWithMetadataUseCase = downloadWithMetadataUseCase,
            )
            fail("Expected IllegalStateException")
        } catch (_: IllegalStateException) {
        }
    }

    @Test
    fun uiState_returnsNullId_whenDownloadIdsAreEmpty() = runTest(testDispatcher) {
        val downloadIdsFlow = MutableStateFlow(emptyList<Long>())
        val viewModel = createViewModel(
            initialId = 20L,
            downloadIdsFlow = downloadIdsFlow,
        )
        val collector = collectUiState(backgroundScope, viewModel)

        waitForUiState(viewModel)

        val state = viewModel.uiState.value as DownloadUiState.Data
        assertEquals(emptyList<Long>(), state.data.downloadIds)
        assertNull(state.data.id)

        collector.cancel()
    }

    @Test
    fun uiState_fallsBackToInitialId_whenSelectedIdIsOutsideTheList() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            initialId = 20L,
            downloadIdsFlow = MutableStateFlow(listOf(10L, 20L, 30L)),
        )
        val collector = collectUiState(backgroundScope, viewModel)

        advanceUntilIdle()
        viewModel.selectDownload(99L)
        advanceUntilIdle()
        waitForUiState(viewModel) { state ->
            val data = state as? DownloadUiState.Data ?: return@waitForUiState false
            data.data.downloadIds == listOf(10L, 20L, 30L) && data.data.id == 20L
        }

        assertEquals(
            20L,
            (viewModel.uiState.value as DownloadUiState.Data).data.id,
        )

        collector.cancel()
    }

    @Test
    fun selectedId_movesToNextAdjacentDownload_whenCurrentDownloadIsDeleted() =
        runTest(testDispatcher) {
            val downloadIdsFlow = MutableStateFlow(listOf(10L, 20L, 30L))
            val viewModel = createViewModel(
                initialId = 20L,
                downloadIdsFlow = downloadIdsFlow,
            )
            val collector = collectUiState(backgroundScope, viewModel)

            advanceUntilIdle()
            downloadIdsFlow.value = listOf(10L, 30L)
            advanceUntilIdle()
            waitForUiState(viewModel)

            assertEquals(30L, viewModel.selectedId.value)
            assertEquals(30L, (viewModel.uiState.value as DownloadUiState.Data).data.id)

            collector.cancel()
        }

    @Test
    fun selectedId_movesToPreviousAdjacentDownload_whenLastDownloadIsDeleted() =
        runTest(testDispatcher) {
            val downloadIdsFlow = MutableStateFlow(listOf(10L, 20L, 30L))
            val viewModel = createViewModel(
                initialId = 30L,
                downloadIdsFlow = downloadIdsFlow,
            )
            val collector = collectUiState(backgroundScope, viewModel)

            advanceUntilIdle()
            downloadIdsFlow.value = listOf(10L, 20L)
            advanceUntilIdle()
            waitForUiState(viewModel)

            assertEquals(20L, viewModel.selectedId.value)
            assertEquals(
                20L,
                (viewModel.uiState.value as DownloadUiState.Data).data.id,
            )

            collector.cancel()
        }

    @Test
    fun selectDownload_updatesSelectedId_andDefaultsMediaToVideo() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadIdsFlow = MutableStateFlow(listOf(10L, 20L, 44L)),
        )
        val collector = collectSelectedMedia(backgroundScope, viewModel)

        viewModel.selectDownload(44L)
        advanceUntilIdle()

        assertEquals(44L, viewModel.selectedId.value)
        assertEquals(DownloadMediaType.VIDEO, viewModel.selectedMedia.value)

        collector.cancel()
    }

    @Test
    fun selectDownload_keepsExistingMediaSelection_forThatDownload() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadIdsFlow = MutableStateFlow(listOf(10L, 20L, 44L)),
        )
        val collector = collectSelectedMedia(backgroundScope, viewModel)

        viewModel.setSelectedMedia(DownloadMediaType.AUDIO, forDownloadId = 44L)
        viewModel.selectDownload(44L)
        advanceUntilIdle()

        assertEquals(44L, viewModel.selectedId.value)
        assertEquals(DownloadMediaType.AUDIO, viewModel.selectedMedia.value)

        collector.cancel()
    }

    @Test
    fun setSelectedMedia_updatesCurrentOrExplicitDownloadSelection() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val collector = collectSelectedMedia(backgroundScope, viewModel)

        viewModel.selectDownload(20L)
        advanceUntilIdle()
        viewModel.setSelectedMedia(DownloadMediaType.AUDIO)
        advanceUntilIdle()
        assertEquals(DownloadMediaType.AUDIO, viewModel.selectedMedia.value)

        viewModel.setSelectedMedia(DownloadMediaType.VIDEO, forDownloadId = 99L)
        viewModel.selectDownload(99L)
        advanceUntilIdle()
        assertEquals(DownloadMediaType.VIDEO, viewModel.selectedMedia.value)

        collector.cancel()
    }

    @Test
    fun setDefaultsForIds_addsMissingDefaults_withoutOverwritingExistingSelection() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            val collector = collectSelectedMedia(backgroundScope, viewModel)

            viewModel.selectDownload(20L)
            advanceUntilIdle()
            viewModel.setSelectedMedia(DownloadMediaType.AUDIO)
            advanceUntilIdle()

            viewModel.setDefaultsForIds(listOf(20L, 30L))
            advanceUntilIdle()
            viewModel.setDefaultsForIds(listOf(20L, 30L))
            advanceUntilIdle()

            assertEquals(DownloadMediaType.AUDIO, viewModel.selectedMedia.value)

            viewModel.selectDownload(30L)
            advanceUntilIdle()
            assertEquals(DownloadMediaType.VIDEO, viewModel.selectedMedia.value)

            collector.cancel()
        }

    @Test
    fun markSeenIfCompleted_updatesSeenForCompletedUnseenDownload() = runTest(testDispatcher) {
        `when`(getDownloadByIdUseCase.invoke(7L))
            .thenReturn(createDownload(id = 7L, status = DownloadStatus.COMPLETED, seen = false))
        val viewModel = createViewModel()

        viewModel.markSeenIfCompleted(7L)
        advanceUntilIdle()

        verify(updateDownloadSeenByIdUseCase).invoke(7L)
    }

    @Test
    fun markSeenIfCompleted_doesNothingForMissingSeenOrIncompleteDownload() =
        runTest(testDispatcher) {
            `when`(getDownloadByIdUseCase.invoke(7L)).thenReturn(null)
            `when`(getDownloadByIdUseCase.invoke(8L))
                .thenReturn(createDownload(id = 8L, status = DownloadStatus.COMPLETED, seen = true))
            `when`(getDownloadByIdUseCase.invoke(9L))
                .thenReturn(createDownload(id = 9L, status = DownloadStatus.FAILED, seen = false))
            val viewModel = createViewModel()

            viewModel.markSeenIfCompleted(7L)
            viewModel.markSeenIfCompleted(8L)
            viewModel.markSeenIfCompleted(9L)
            advanceUntilIdle()

            verify(updateDownloadSeenByIdUseCase, never()).invoke(7L)
            verify(updateDownloadSeenByIdUseCase, never()).invoke(8L)
            verify(updateDownloadSeenByIdUseCase, never()).invoke(9L)
        }

    @Test
    fun deleteDownload_requestsDeletion_withoutNavigation_whenOtherDownloadsRemain() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(
                initialId = 20L,
                downloadIdsFlow = MutableStateFlow(listOf(10L, 20L, 30L)),
            )
            val eventDeferred = backgroundScope.async {
                runCatching {
                    withTimeout(250L) {
                        viewModel.events.first()
                    }
                }
            }

            advanceUntilIdle()
            viewModel.deleteDownload(20L)
            advanceUntilIdle()

            verify(requestDeleteDownloadsUseCase).invoke(listOf(20L))
            assertNull(eventDeferred.await().getOrNull())
        }

    @Test
    fun deleteDownload_emitsNavigateBack_whenDeletingLastDownload() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            initialId = 20L,
            downloadIdsFlow = MutableStateFlow(listOf(20L)),
        )
        val eventDeferred = backgroundScope.async {
            viewModel.events.first()
        }

        viewModel.deleteDownload()
        advanceUntilIdle()

        verify(requestDeleteDownloadsUseCase).invoke(listOf(20L))
        assertEquals(DownloadEvent.NavigateBack, eventDeferred.await())
    }

    @Test
    fun retryDownload_startsExplicitOrSelectedDownload() = runTest(testDispatcher) {
        val viewModel = createViewModel(initialId = 20L)

        viewModel.retryDownload(88L)
        viewModel.retryDownload()
        advanceUntilIdle()

        verify(startDownloadUseCase).invoke(88L)
        verify(startDownloadUseCase).invoke(20L)
    }

    @Test
    fun shareSaveAndPlay_emitEventsUsingSelectedMediaPath() = runTest(testDispatcher) {
        stubPageData(
            downloadId = 20L,
            download = createDownload(id = 20L, status = DownloadStatus.COMPLETED),
            video = DownloadVideo(
                downloadId = 20L,
                filePath = "/tmp/video.mp4",
                fileExtension = "mp4",
                sha256 = null,
            ),
            audio = DownloadAudio(
                downloadId = 20L,
                filePath = "/tmp/audio.mp3",
                fileExtension = "mp3",
            ),
        )
        val viewModel = createViewModel(initialId = 20L)
        val mediaCollector = collectSelectedMedia(backgroundScope, viewModel)

        val shareDeferred = backgroundScope.async { viewModel.events.first() }
        viewModel.shareDownload()
        advanceUntilIdle()
        assertEquals(DownloadEvent.Share("/tmp/video.mp4"), shareDeferred.await())

        viewModel.setSelectedMedia(DownloadMediaType.AUDIO)
        advanceUntilIdle()

        val saveDeferred = backgroundScope.async { viewModel.events.first() }
        viewModel.saveDownload()
        advanceUntilIdle()
        assertEquals(DownloadEvent.Save("/tmp/audio.mp3"), saveDeferred.await())

        val playDeferred = backgroundScope.async { viewModel.events.first() }
        viewModel.playDownload()
        advanceUntilIdle()
        assertEquals(DownloadEvent.Play("/tmp/audio.mp3"), playDeferred.await())

        mediaCollector.cancel()
    }

    @Test
    fun shareSaveAndPlay_emitEventsForExplicitDownloadId() = runTest(testDispatcher) {
        stubPageData(
            downloadId = 88L,
            download = createDownload(id = 88L, status = DownloadStatus.COMPLETED),
            video = DownloadVideo(
                downloadId = 88L,
                filePath = "/tmp/explicit-video.mp4",
                fileExtension = "mp4",
                sha256 = null,
            ),
            audio = DownloadAudio(
                downloadId = 88L,
                filePath = "/tmp/explicit-audio.mp3",
                fileExtension = "mp3",
            ),
        )
        val viewModel = createViewModel(initialId = 20L)
        val mediaCollector = collectSelectedMedia(backgroundScope, viewModel)

        val shareDeferred = backgroundScope.async { viewModel.events.first() }
        viewModel.shareDownload(88L)
        advanceUntilIdle()
        assertEquals(DownloadEvent.Share("/tmp/explicit-video.mp4"), shareDeferred.await())

        viewModel.setSelectedMedia(DownloadMediaType.AUDIO, forDownloadId = 88L)
        advanceUntilIdle()

        val saveDeferred = backgroundScope.async { viewModel.events.first() }
        viewModel.saveDownload(88L)
        advanceUntilIdle()
        assertEquals(DownloadEvent.Save("/tmp/explicit-audio.mp3"), saveDeferred.await())

        val playDeferred = backgroundScope.async { viewModel.events.first() }
        viewModel.playDownload(88L)
        advanceUntilIdle()
        assertEquals(DownloadEvent.Play("/tmp/explicit-audio.mp3"), playDeferred.await())

        mediaCollector.cancel()
    }

    @Test
    fun shareDownload_usesExplicitStoredVideoSelection_forExplicitDownloadId() =
        runTest(testDispatcher) {
            stubPageData(
                downloadId = 88L,
                download = createDownload(id = 88L, status = DownloadStatus.COMPLETED),
                video = DownloadVideo(
                    downloadId = 88L,
                    filePath = "/tmp/stored-video.mp4",
                    fileExtension = "mp4",
                    sha256 = null,
                ),
            )
            val viewModel = createViewModel(initialId = 20L)

            viewModel.setSelectedMedia(DownloadMediaType.VIDEO, forDownloadId = 88L)
            advanceUntilIdle()

            val shareDeferred = backgroundScope.async { viewModel.events.first() }
            viewModel.shareDownload(88L)
            advanceUntilIdle()

            assertEquals(DownloadEvent.Share("/tmp/stored-video.mp4"), shareDeferred.await())
        }

    @Test
    fun shareDownload_handlesSuspendingPageDataFlow() = runTest(testDispatcher) {
        `when`(getDownloadByIdAsFlowUseCase.invoke(66L)).thenReturn(
            flow {
                kotlinx.coroutines.yield()
                emit(createDownload(id = 66L, status = DownloadStatus.COMPLETED))
            }
        )
        `when`(getDownloadVideoByDownloadIdAsFlowUseCase.invoke(66L)).thenReturn(
            flow {
                kotlinx.coroutines.yield()
                emit(
                    DownloadVideo(
                        downloadId = 66L,
                        filePath = "/tmp/suspending-video.mp4",
                        fileExtension = "mp4",
                        sha256 = null,
                    )
                )
            }
        )
        `when`(getDownloadAudioByDownloadIdAsFlowUseCase.invoke(66L)).thenReturn(flowOf(null))
        `when`(getDownloadMetadataByIdAsFlowUseCase.invoke(66L)).thenReturn(flowOf(null))
        `when`(getDownloadProgressByDownloadIdAsFlowUseCase.invoke(66L)).thenReturn(flowOf(null))

        val viewModel = createViewModel(initialId = 20L)
        val shareDeferred = backgroundScope.async { viewModel.events.first() }

        viewModel.shareDownload(66L)
        advanceUntilIdle()

        assertEquals(DownloadEvent.Share("/tmp/suspending-video.mp4"), shareDeferred.await())
    }

    @Test
    fun shareSaveAndPlay_doNothing_whenSelectedMediaFileIsMissing() = runTest(testDispatcher) {
        stubPageData(
            downloadId = 20L,
            download = createDownload(id = 20L, status = DownloadStatus.COMPLETED),
        )
        val viewModel = createViewModel(initialId = 20L)
        val mediaCollector = collectSelectedMedia(backgroundScope, viewModel)

        val shareDeferred = backgroundScope.async {
            runCatching {
                withTimeout(250L) {
                    viewModel.events.first()
                }
            }
        }
        viewModel.shareDownload()
        advanceUntilIdle()
        assertNull(shareDeferred.await().getOrNull())

        viewModel.setSelectedMedia(DownloadMediaType.AUDIO)
        advanceUntilIdle()
        val saveDeferred = backgroundScope.async {
            runCatching {
                withTimeout(250L) {
                    viewModel.events.first()
                }
            }
        }
        viewModel.saveDownload()
        advanceUntilIdle()
        assertNull(saveDeferred.await().getOrNull())

        val playDeferred = backgroundScope.async {
            runCatching {
                withTimeout(250L) {
                    viewModel.events.first()
                }
            }
        }
        viewModel.playDownload()
        advanceUntilIdle()
        assertNull(playDeferred.await().getOrNull())

        mediaCollector.cancel()
    }

    @Test
    fun getDownloadPageUiData_returnsNull_whenBaseDownloadIsMissing() = runTest(testDispatcher) {
        stubPageData(downloadId = 55L, download = null)
        val viewModel = createViewModel()

        assertNull(viewModel.getDownloadPageUiData(55L).first())
    }

    @Test
    fun getDownloadPageUiData_mapsDomainModelsIntoUiData() = runTest(testDispatcher) {
        stubPageData(
            downloadId = 20L,
            download = createDownload(
                id = 20L,
                status = DownloadStatus.COMPLETED,
                seen = true,
                errorMessage = "download failed",
                completedAtMillis = 1234L,
            ),
            video = DownloadVideo(
                downloadId = 20L,
                filePath = "/tmp/video.mp4",
                fileExtension = "mp4",
                sha256 = null,
            ),
            audio = DownloadAudio(
                downloadId = 20L,
                filePath = "/tmp/audio.mp3",
                fileExtension = "mp3",
            ),
            metadata = DownloadMetadata(
                downloadId = 20L,
                videoId = "video-id",
                source = "yt",
                title = "Example title",
                thumbnailFilePath = "/tmp/thumb.jpg",
                durationSeconds = 42,
            ),
            progress = DownloadProgress(
                downloadId = 20L,
                updatedAtMillis = 10L,
                progressPercent = 50f,
                bytesDownloaded = 500L,
                etaSeconds = 12L,
                expectedBytes = 1000L,
            ),
        )
        val viewModel = createViewModel()

        val pageData = viewModel.getDownloadPageUiData(20L).first()

        assertEquals(
            DownloadPageUiData(
                id = 20L,
                url = "https://example.com/20",
                status = org.strigate.ferrot.presentation.model.DownloadStatusUiData.COMPLETED,
                metadata = org.strigate.ferrot.presentation.model.DownloadMetadataUiData(
                    title = "Example title",
                    thumbnailFilePath = "/tmp/thumb.jpg",
                    durationSeconds = 42,
                ),
                video = org.strigate.ferrot.presentation.model.DownloadVideoUiData(
                    filePath = "/tmp/video.mp4",
                    fileName = "video.mp4",
                    fileExtension = "mp4",
                ),
                audio = org.strigate.ferrot.presentation.model.DownloadAudioUiData(
                    filePath = "/tmp/audio.mp3",
                    fileName = "audio.mp3",
                    fileExtension = "mp3",
                ),
                progress = org.strigate.ferrot.presentation.model.DownloadProgressUiData(
                    progressFraction = 0.5f,
                    bytesDownloaded = 500L,
                    etaSeconds = 12L,
                    expectedBytes = 1000L,
                ),
                seen = true,
                errorMessage = "download failed",
                completedAtMillis = 1234L,
            ),
            pageData,
        )
    }

    private fun createViewModel(
        initialId: Long = 20L,
        downloadIdsFlow: MutableStateFlow<List<Long>> = MutableStateFlow(listOf(10L, 20L, 30L)),
    ): DownloadViewModel {
        `when`(getDownloadIdsWithMetadataAsFlowUseCase.invoke())
            .thenReturn(downloadIdsFlow)
        `when`(downloadWithMetadataUseCase.getDownloadIdsWithMetadataAsFlowUseCase)
            .thenReturn(getDownloadIdsWithMetadataAsFlowUseCase)

        `when`(downloadUseCase.getDownloadByIdAsFlowUseCase)
            .thenReturn(getDownloadByIdAsFlowUseCase)
        `when`(downloadUseCase.getDownloadByIdUseCase)
            .thenReturn(getDownloadByIdUseCase)
        `when`(downloadUseCase.updateDownloadSeenByIdUseCase)
            .thenReturn(updateDownloadSeenByIdUseCase)
        `when`(downloadUseCase.requestDeleteDownloadsUseCase)
            .thenReturn(requestDeleteDownloadsUseCase)

        `when`(downloadVideoUseCase.getDownloadVideoByDownloadIdAsFlowUseCase)
            .thenReturn(getDownloadVideoByDownloadIdAsFlowUseCase)
        `when`(downloadAudioUseCase.getDownloadAudioByDownloadIdAsFlowUseCase)
            .thenReturn(getDownloadAudioByDownloadIdAsFlowUseCase)
        `when`(downloadMetadataUseCase.getDownloadMetadataByIdAsFlowUseCase)
            .thenReturn(getDownloadMetadataByIdAsFlowUseCase)
        `when`(downloadProgressUseCase.getDownloadProgressByDownloadIdAsFlowUseCase)
            .thenReturn(getDownloadProgressByDownloadIdAsFlowUseCase)

        return DownloadViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(Screen.Download.ARG_DOWNLOAD_ID to initialId)
            ),
            analyticsLogger = analyticsLogger,
            downloadUseCase = downloadUseCase,
            downloadVideoUseCase = downloadVideoUseCase,
            downloadAudioUseCase = downloadAudioUseCase,
            downloadProgressUseCase = downloadProgressUseCase,
            downloadMetadataUseCase = downloadMetadataUseCase,
            clearNotificationsByDownloadIdUseCase = clearNotificationsByDownloadIdUseCase,
            startDownloadUseCase = startDownloadUseCase,
            downloadWithMetadataUseCase = downloadWithMetadataUseCase,
        )
    }

    private fun stubPageData(
        downloadId: Long,
        download: Download? = createDownload(id = downloadId),
        video: DownloadVideo? = null,
        audio: DownloadAudio? = null,
        metadata: DownloadMetadata? = null,
        progress: DownloadProgress? = null,
    ) {
        `when`(getDownloadByIdAsFlowUseCase.invoke(downloadId))
            .thenReturn(flowOf(download))
        `when`(getDownloadVideoByDownloadIdAsFlowUseCase.invoke(downloadId))
            .thenReturn(flowOf(video))
        `when`(getDownloadAudioByDownloadIdAsFlowUseCase.invoke(downloadId))
            .thenReturn(flowOf(audio))
        `when`(getDownloadMetadataByIdAsFlowUseCase.invoke(downloadId))
            .thenReturn(flowOf(metadata))
        `when`(getDownloadProgressByDownloadIdAsFlowUseCase.invoke(downloadId))
            .thenReturn(flowOf(progress))
    }

    private fun collectUiState(
        scope: CoroutineScope,
        viewModel: DownloadViewModel,
    ) = scope.launch {
        viewModel.uiState.collect()
    }

    private fun collectSelectedMedia(
        scope: CoroutineScope,
        viewModel: DownloadViewModel,
    ) = scope.launch {
        viewModel.selectedMedia.collect()
    }

    private suspend fun waitForUiState(
        viewModel: DownloadViewModel,
        predicate: (DownloadUiState) -> Boolean = { it is DownloadUiState.Data },
    ) {
        withTimeout(2.seconds) {
            while (!predicate(viewModel.uiState.value)) {
                kotlinx.coroutines.yield()
            }
        }
    }

    private fun createDownload(
        id: Long,
        status: DownloadStatus = DownloadStatus.QUEUED,
        seen: Boolean = false,
        errorMessage: String? = null,
        completedAtMillis: Long? = null,
    ) = Download(
        id = id,
        uid = "uid-$id",
        url = "https://example.com/$id",
        status = status,
        seen = seen,
        errorMessage = errorMessage,
        completedAtMillis = completedAtMillis,
    )
}
