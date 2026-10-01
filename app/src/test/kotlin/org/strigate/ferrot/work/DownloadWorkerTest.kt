package org.strigate.ferrot.work

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.impl.WorkManagerImpl
import androidx.work.WorkerParameters
import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.CALLS_REAL_METHODS
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.withSettings
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.R
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.app.Constants.Work.Name.KEY_ID
import org.strigate.ferrot.app.NotificationService
import org.strigate.ferrot.app.actions.DownloadNotificationActionType
import org.strigate.ferrot.app.actions.buildDownloadNotificationAction
import org.strigate.ferrot.app.actions.downloadNotificationExtras
import org.strigate.ferrot.app.integration.CookieFileStore
import org.strigate.ferrot.app.provider.DownloadPathProvider
import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadAudio
import org.strigate.ferrot.domain.model.DownloadMediaType
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.model.DownloadVideo
import org.strigate.ferrot.domain.usecase.CookieSetUseCase
import org.strigate.ferrot.domain.usecase.DownloadAudioUseCase
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.DownloadVideoUseCase
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.YoutubeDlAndroidUseCase
import org.strigate.ferrot.domain.usecase.combined.DeleteDownloadAndRelatedCombinedUseCase
import org.strigate.ferrot.domain.usecase.cookieset.ResolveCookieSetForUrlUseCase
import org.strigate.ferrot.domain.usecase.download.DeleteDownloadFilesUseCase
import org.strigate.ferrot.domain.usecase.download.GetDownloadByIdUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadCompletedAtUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadErrorMessageUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadStartedAtUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadStatusUseCase
import org.strigate.ferrot.domain.usecase.downloadaudio.SaveDownloadAudioUseCase
import org.strigate.ferrot.domain.usecase.downloadprogress.UpdateDownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.downloadvideo.SaveDownloadVideoUseCase
import org.strigate.ferrot.domain.usecase.settings.GetAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.youtubedl_android.DownloadWithProgressUseCase
import org.strigate.ferrot.domain.usecase.youtubedl_android.GetVideoInfoUseCase
import org.strigate.ferrot.test.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadWorkerTest {
    private lateinit var autoCloseable: AutoCloseable

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val testDispatcher: TestDispatcher = mainDispatcherRule.testDispatcher

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private var logMock: MockedStatic<Log>? = null

    @Mock
    private lateinit var appContext: Context

    @Mock
    private lateinit var analyticsLogger: AnalyticsLogger

    @Mock
    private lateinit var notificationService: NotificationService

    @Mock
    private lateinit var settingsUseCase: SettingsUseCase

    @Mock
    private lateinit var cookieSetUseCase: CookieSetUseCase

    @Mock
    private lateinit var cookieFileStore: CookieFileStore

    @Mock
    private lateinit var downloadPathProvider: DownloadPathProvider

    @Mock
    private lateinit var youtubeDlAndroidUseCase: YoutubeDlAndroidUseCase

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
    private lateinit var deleteDownloadAndRelatedCombinedUseCase: DeleteDownloadAndRelatedCombinedUseCase

    @Mock
    private lateinit var getDownloadByIdUseCase: GetDownloadByIdUseCase

    @Mock
    private lateinit var updateDownloadStatusUseCase: UpdateDownloadStatusUseCase

    @Mock
    private lateinit var deleteDownloadFilesUseCase: DeleteDownloadFilesUseCase

    @Mock
    private lateinit var updateDownloadProgressUseCase: UpdateDownloadProgressUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        logMock = mockStatic(Log::class.java)
        `when`(downloadUseCase.getDownloadByIdUseCase)
            .thenReturn(getDownloadByIdUseCase)
        `when`(downloadUseCase.updateDownloadStatusUseCase)
            .thenReturn(updateDownloadStatusUseCase)
        `when`(downloadUseCase.deleteDownloadFilesUseCase)
            .thenReturn(deleteDownloadFilesUseCase)
        `when`(downloadProgressUseCase.updateDownloadProgressUseCase)
            .thenReturn(updateDownloadProgressUseCase)
    }

    @Test
    fun doWork_failsWithoutTouchingState_whenDownloadIdIsInvalid() = runTest(testDispatcher) {
        val result = createWorker(downloadId = -1L).doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        verify(getDownloadByIdUseCase, never()).invoke(-1L)
        verify(updateDownloadStatusUseCase, never()).invoke(-1L, DownloadStatus.FAILED)
    }

    @Test
    fun doWork_marksDownloadFailed_whenDownloadRecordIsMissing() = runTest(testDispatcher) {
        `when`(getDownloadByIdUseCase.invoke(42L))
            .thenReturn(null)

        val result = createWorker(downloadId = 42L).doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        verify(deleteDownloadFilesUseCase).invoke(42L)
        verify(updateDownloadProgressUseCase).invoke(
            id = 42L,
            progressPercent = 0f,
            bytesDownloaded = 0L,
            etaSeconds = null,
        )
        verify(updateDownloadStatusUseCase).invoke(42L, DownloadStatus.FAILED)
    }

    @Test
    fun doWork_failsWithoutLookup_whenAttemptsAreExhausted() = runTest(testDispatcher) {
        val result = createWorker(downloadId = 42L, runAttemptCount = 21).doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        verify(getDownloadByIdUseCase, never()).invoke(42L)
    }

    @Test
    fun doWork_doesNotPostForeground_whenDownloadIsCompleted() = runTest(testDispatcher) {
        `when`(getDownloadByIdUseCase.invoke(42L))
            .thenReturn(download(DownloadStatus.COMPLETED))

        val result = createWorker(downloadId = 42L).doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        verifyNoInteractions(cookieFileStore, notificationService, updateDownloadStatusUseCase)
        verify(appContext, never()).getString(R.string.notification_text_downloading)
    }

    @Test
    fun doWork_preservesStateAndFiles_whenStartupIsCancelled() = runTest(testDispatcher) {
        val cancellation = CancellationException("System interrupted download")
        `when`(getDownloadByIdUseCase.invoke(42L))
            .thenReturn(download(DownloadStatus.DOWNLOADING))
        `when`(updateDownloadStatusUseCase.invoke(42L, DownloadStatus.METADATA))
            .thenThrow(cancellation)

        try {
            createWorker(downloadId = 42L).doWork()
            fail("Expected cancellation")
        } catch (exception: CancellationException) {
            assertEquals(cancellation.message, exception.message)
        }

        verify(updateDownloadStatusUseCase, never()).invoke(42L, DownloadStatus.FAILED)
        verify(updateDownloadStatusUseCase, never()).invoke(42L, DownloadStatus.STOPPED)
        verifyNoInteractions(
            deleteDownloadFilesUseCase,
            updateDownloadProgressUseCase,
            notificationService
        )
    }

    @Test
    fun doWork_preservesState_whenStoppedWorkerReportsAnError() = runTest(testDispatcher) {
        `when`(getDownloadByIdUseCase.invoke(42L))
            .thenReturn(download(DownloadStatus.DOWNLOADING))
        `when`(updateDownloadStatusUseCase.invoke(42L, DownloadStatus.METADATA))
            .thenThrow(IllegalStateException("Process interrupted"))
        val worker = spy(createWorker(downloadId = 42L))
        doReturn(true).`when`(worker).isStopped

        try {
            worker.doWork()
            fail("Expected cancellation")
        } catch (_: CancellationException) {
            // A stopped attempt must leave shared state to WorkManager's next attempt.
        }

        verify(updateDownloadStatusUseCase, never()).invoke(42L, DownloadStatus.FAILED)
        verifyNoInteractions(
            deleteDownloadFilesUseCase,
            updateDownloadProgressUseCase,
            notificationService
        )
    }

    @Test
    fun doWork_setsActiveStateBeforeForeground_andPreservesFilesOnCancellation() =
        runTest(testDispatcher) {
            val download = download(DownloadStatus.FAILED)
            `when`(getDownloadByIdUseCase.invoke(42L))
                .thenReturn(download)
            `when`(appContext.getString(R.string.notification_text_downloading))
                .thenReturn("downloading")
            val resolveCookies = mock(ResolveCookieSetForUrlUseCase::class.java)
            `when`(cookieSetUseCase.resolveCookieSetForUrlUseCase)
                .thenReturn(resolveCookies)
            `when`(resolveCookies.invoke(download.url))
                .thenThrow(CancellationException("Work replaced during cookie preparation"))
            val worker = spy(createWorker(downloadId = 42L))
            val action = mock(NotificationCompat.Action::class.java)
            val notificationExtras = downloadNotificationExtras(42L)
            val actionsClass =
                Class.forName("org.strigate.ferrot.app.actions.DownloadNotificationActionsKt")
            mockStatic(actionsClass, CALLS_REAL_METHODS).use { actions ->
                actions.`when`<NotificationCompat.Action> {
                    buildDownloadNotificationAction(
                        appContext,
                        42L,
                        DownloadNotificationActionType.STOP
                    )
                }.thenReturn(action)
                doReturn(Unit).`when`(worker).enableForeground(
                    notificationText = "downloading",
                    indeterminate = true,
                    contentText = download.url,
                    extras = notificationExtras,
                    actions = listOf(action),
                )

                try {
                    worker.doWork()
                    fail("Expected cancellation")
                } catch (_: CancellationException) {
                    // The cancelled attempt must not mark the resumed download failed again.
                }
                val order = inOrder(updateDownloadStatusUseCase, worker)
                order.verify(updateDownloadStatusUseCase).invoke(42L, DownloadStatus.METADATA)
                order.verify(worker).enableForeground(
                    notificationText = "downloading",
                    indeterminate = true,
                    contentText = download.url,
                    extras = notificationExtras,
                    actions = listOf(action),
                )
            }
            verify(updateDownloadStatusUseCase, never()).invoke(42L, DownloadStatus.FAILED)
            verifyNoInteractions(
                deleteDownloadFilesUseCase,
                updateDownloadProgressUseCase,
                notificationService
            )
            verify(cookieFileStore).delete(null)
        }

    @Test
    fun doWork_recordsFailure_whenFileCleanupThrows() = runTest(testDispatcher) {
        `when`(getDownloadByIdUseCase.invoke(42L))
            .thenReturn(null)
        `when`(deleteDownloadFilesUseCase.invoke(42L))
            .thenThrow(IllegalStateException("Storage unavailable"))

        val result = createWorker(downloadId = 42L).doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        verify(updateDownloadStatusUseCase).invoke(42L, DownloadStatus.FAILED)
    }

    @Test
    fun doWork_recordsFailure_whenProgressCleanupThrows() = runTest(testDispatcher) {
        `when`(getDownloadByIdUseCase.invoke(42L))
            .thenReturn(null)
        `when`(
            updateDownloadProgressUseCase.invoke(
                id = 42L,
                progressPercent = 0f,
                bytesDownloaded = 0L,
                etaSeconds = null,
            ),
        )
            .thenThrow(IllegalStateException("Progress unavailable"))

        val result = createWorker(downloadId = 42L).doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        verify(updateDownloadStatusUseCase).invoke(42L, DownloadStatus.FAILED)
    }

    @Test
    fun doWork_doesNotRecordFailure_whenCleanupIsCancelled() = runTest(testDispatcher) {
        `when`(getDownloadByIdUseCase.invoke(42L))
            .thenReturn(null)
        `when`(deleteDownloadFilesUseCase.invoke(42L))
            .thenThrow(CancellationException("Work replaced"))

        try {
            createWorker(downloadId = 42L).doWork()
            fail("Expected cancellation")
        } catch (_: CancellationException) {
            // Cleanup must remain cancellable even though ordinary cleanup errors are tolerated.
        }

        verify(updateDownloadStatusUseCase, never()).invoke(42L, DownloadStatus.FAILED)
        verifyNoInteractions(updateDownloadProgressUseCase)
    }

    @Test
    fun doWork_completesWithVideoAndAudio() = runTest(testDispatcher) {
        verifyMediaDownload(audioFailure = null)
    }

    @Test
    fun doWork_keepsVideo_whenAudioProcessFails() = runTest(testDispatcher) {
        verifyMediaDownload(audioFailure = YoutubeDL.CanceledException())
    }

    @Test
    fun doWork_doesNotComplete_whenAudioCoroutineIsCancelled() = runTest(testDispatcher) {
        verifyMediaDownload(audioFailure = CancellationException("System interrupted audio"))
    }

    @Test
    fun doWork_updatesFailedForegroundBeforeRecordingFailure() = runTest(testDispatcher) {
        verifyFailedForeground(terminalUpdateFails = false)
    }

    @Test
    fun doWork_recordsFailure_whenTerminalForegroundUpdateFails() = runTest(testDispatcher) {
        verifyFailedForeground(terminalUpdateFails = true)
    }

    @After
    fun tearDown() {
        logMock?.close()
        autoCloseable.close()
    }

    private suspend fun verifyFailedForeground(terminalUpdateFails: Boolean) {
        val download = download(DownloadStatus.QUEUED)
        val events = mutableListOf<String>()
        `when`(getDownloadByIdUseCase.invoke(42L)).thenReturn(download)
        `when`(appContext.getString(R.string.notification_text_downloading)).thenReturn("downloading")
        `when`(appContext.getString(R.string.download_failed)).thenReturn("failed")
        val resolveCookies = mock(ResolveCookieSetForUrlUseCase::class.java)
        `when`(cookieSetUseCase.resolveCookieSetForUrlUseCase).thenReturn(resolveCookies)
        `when`(resolveCookies.invoke(download.url)).thenThrow(IllegalStateException("Cookie preparation failed"))
        val updateError = mock(UpdateDownloadErrorMessageUseCase::class.java)
        `when`(downloadUseCase.updateDownloadErrorMessageUseCase).thenReturn(updateError)
        doAnswer {
            events += "recorded failed"
            true
        }.`when`(updateDownloadStatusUseCase).invoke(42L, DownloadStatus.FAILED)
        val worker = mock(
            DownloadWorker::class.java,
            withSettings().spiedInstance(createWorker(42L)).defaultAnswer { invocation ->
                when (invocation.method.name) {
                    "enableForeground" -> {
                        events += "foreground downloading"
                        Unit
                    }

                    "updateForeground" -> {
                        events += "foreground ${invocation.getArgument<String>(0)}"
                        if (terminalUpdateFails) throw IllegalStateException("Service unavailable")
                        Unit
                    }

                    else -> invocation.callRealMethod()
                }
            },
        )
        val action = mock(NotificationCompat.Action::class.java)
        val actionsClass =
            Class.forName("org.strigate.ferrot.app.actions.DownloadNotificationActionsKt")
        mockStatic(actionsClass, CALLS_REAL_METHODS).use { actions ->
            for (actionType in listOf(
                DownloadNotificationActionType.STOP,
                DownloadNotificationActionType.RETRY,
                DownloadNotificationActionType.DELETE,
            )) {
                actions.`when`<NotificationCompat.Action> {
                    buildDownloadNotificationAction(appContext, 42L, actionType)
                }.thenReturn(action)
            }
            mockStatic(Class.forName("org.strigate.ferrot.extensions.ContextKt")).use {
                assertTrue(worker.doWork() is ListenableWorker.Result.Failure)
            }
        }
        assertEquals(
            listOf("foreground downloading", "foreground failed", "recorded failed"),
            events,
        )
    }

    private suspend fun verifyMediaDownload(audioFailure: Throwable?) {
        val download = download(DownloadStatus.QUEUED).copy(archived = true)
        val directory = temporaryFolder.newFolder()
        val videoFile = File(directory, "video.mp4").apply { writeText("video content") }
        val audioFile = File(directory, "audio.m4a").apply { writeText("audio content") }
        `when`(getDownloadByIdUseCase.invoke(42L)).thenReturn(download)
        `when`(appContext.getString(R.string.notification_text_downloading)).thenReturn("downloading")
        `when`(appContext.getString(R.string.download_complete)).thenReturn("complete")
        `when`(downloadPathProvider.uidDir(download.uid)).thenReturn(directory)
        val resolveCookies = mock(ResolveCookieSetForUrlUseCase::class.java)
        `when`(cookieSetUseCase.resolveCookieSetForUrlUseCase).thenReturn(resolveCookies)
        val updateError = mock(UpdateDownloadErrorMessageUseCase::class.java)
        `when`(downloadUseCase.updateDownloadErrorMessageUseCase).thenReturn(updateError)
        val updateStarted = mock(UpdateDownloadStartedAtUseCase::class.java)
        `when`(downloadUseCase.updateDownloadStartedAtUseCase).thenReturn(updateStarted)
        val updateCompleted = mock(UpdateDownloadCompletedAtUseCase::class.java)
        `when`(downloadUseCase.updateDownloadCompletedAtUseCase).thenReturn(updateCompleted)
        val getVideoInfo = mock(GetVideoInfoUseCase::class.java)
        `when`(youtubeDlAndroidUseCase.getVideoInfoUseCase).thenReturn(getVideoInfo)
        `when`(
            getVideoInfo.invoke(
                download.url,
                null
            )
        ).thenThrow(IllegalStateException("Metadata unavailable"))
        val savedVideos = mutableListOf<DownloadVideo>()
        val saveVideo = mock(SaveDownloadVideoUseCase::class.java) { invocation ->
            if (invocation.method.name == "invoke") {
                savedVideos += invocation.getArgument<DownloadVideo>(0)
                true
            } else {
                RETURNS_DEFAULTS.answer(invocation)
            }
        }
        `when`(downloadVideoUseCase.saveDownloadVideoUseCase).thenReturn(saveVideo)
        val savedAudio = mutableListOf<DownloadAudio>()
        val saveAudio = mock(SaveDownloadAudioUseCase::class.java) { invocation ->
            if (invocation.method.name == "invoke") {
                savedAudio += invocation.getArgument<DownloadAudio>(0)
                true
            } else {
                RETURNS_DEFAULTS.answer(invocation)
            }
        }
        `when`(downloadAudioUseCase.saveDownloadAudioUseCase).thenReturn(saveAudio)
        val getDeleteDuplicates =
            mock(GetAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase::class.java)
        `when`(settingsUseCase.getAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase)
            .thenReturn(getDeleteDuplicates)
        `when`(getDeleteDuplicates.invoke()).thenReturn(flowOf(false))
        val downloadMedia = mock(DownloadWithProgressUseCase::class.java) { invocation ->
            if (invocation.method.name != "invoke") {
                RETURNS_DEFAULTS.answer(invocation)
            } else {
                val mediaType = invocation.getArgument<DownloadMediaType>(5)
                val reportOutput = invocation.getArgument<((String) -> Unit)?>(7)
                flow<DownloadWithProgressUseCase.DownloadTick> {
                    if (mediaType == DownloadMediaType.AUDIO && audioFailure != null) throw audioFailure
                    reportOutput?.invoke(
                        if (mediaType == DownloadMediaType.VIDEO) videoFile.absolutePath else audioFile.absolutePath,
                    )
                }
            }
        }
        `when`(youtubeDlAndroidUseCase.downloadWithProgressUseCase).thenReturn(downloadMedia)
        // Intercept framework notification calls while exercising the real download orchestration.
        val terminalTitles = mutableListOf<String>()
        val worker = mock(
            DownloadWorker::class.java,
            withSettings().spiedInstance(createWorker(42L)).defaultAnswer { invocation ->
                when (invocation.method.name) {
                    "enableForeground" -> Unit
                    "updateForeground" -> {
                        terminalTitles += invocation.getArgument<String>(0)
                        Unit
                    }

                    else -> invocation.callRealMethod()
                }
            },
        )
        val action = mock(NotificationCompat.Action::class.java)
        val actionsClass =
            Class.forName("org.strigate.ferrot.app.actions.DownloadNotificationActionsKt")
        val workManager = mock(WorkManagerImpl::class.java)
        mockStatic(actionsClass, CALLS_REAL_METHODS).use { actions ->
            actions.`when`<NotificationCompat.Action> {
                buildDownloadNotificationAction(
                    appContext,
                    42L,
                    DownloadNotificationActionType.STOP
                )
            }.thenReturn(action)
            mockStatic(WorkManagerImpl::class.java).use { workManagerStatic ->
                workManagerStatic.`when`<WorkManagerImpl> {
                    WorkManagerImpl.getInstance(appContext)
                }.thenReturn(workManager)
                if (audioFailure is CancellationException) {
                    try {
                        worker.doWork()
                        fail("Expected cancellation")
                    } catch (_: CancellationException) {
                        // Interrupted audio must not complete or destroy the already saved video.
                    }
                } else {
                    assertTrue(worker.doWork() is ListenableWorker.Result.Success)
                }
            }
        }
        val completionUpdates = mockingDetails(updateDownloadStatusUseCase)
            .invocations
            .count { it.method.name == "invoke" && it.arguments[1] == DownloadStatus.COMPLETED }
        assertEquals(if (audioFailure is CancellationException) 0 else 1, completionUpdates)
        assertEquals(audioFailure !is CancellationException, "complete" in terminalTitles)
        assertEquals(videoFile.absolutePath, savedVideos.single().filePath)
        assertTrue(videoFile.exists())
        if (audioFailure != null) {
            assertTrue(savedAudio.isEmpty())
        } else {
            assertEquals(audioFile.absolutePath, savedAudio.single().filePath)
        }
        verify(deleteDownloadFilesUseCase).invoke(42L)
        verify(updateDownloadStatusUseCase, never()).invoke(42L, DownloadStatus.FAILED)
        verify(updateDownloadStatusUseCase, never()).invoke(42L, DownloadStatus.STOPPED)
    }

    private fun download(status: DownloadStatus) = Download(
        id = 42L,
        uid = "download-42",
        url = "https://example.com/video",
        status = status,
        seen = false,
    )

    private fun createWorker(
        downloadId: Long,
        runAttemptCount: Int = 0,
    ) = DownloadWorker(
        appContext = appContext,
        workerParameters = mockWorkerParameters(
            inputData = Data.Builder().putLong(KEY_ID, downloadId).build(),
            runAttemptCount = runAttemptCount,
        ),
        analyticsLogger = analyticsLogger,
        notificationService = notificationService,
        settingsUseCase = settingsUseCase,
        cookieSetUseCase = cookieSetUseCase,
        cookieFileStore = cookieFileStore,
        downloadPathProvider = downloadPathProvider,
        youtubeDlAndroidUseCase = youtubeDlAndroidUseCase,
        downloadUseCase = downloadUseCase,
        downloadVideoUseCase = downloadVideoUseCase,
        downloadAudioUseCase = downloadAudioUseCase,
        downloadProgressUseCase = downloadProgressUseCase,
        downloadMetadataUseCase = downloadMetadataUseCase,
        deleteDownloadAndRelatedCombinedUseCase = deleteDownloadAndRelatedCombinedUseCase,
    )

    private fun mockWorkerParameters(
        inputData: Data = Data.EMPTY,
        runAttemptCount: Int = 0,
    ): WorkerParameters {
        val workerParameters = mock(WorkerParameters::class.java)
        `when`(workerParameters.id)
            .thenReturn(UUID.randomUUID())
        `when`(workerParameters.inputData)
            .thenReturn(inputData)
        `when`(workerParameters.runAttemptCount)
            .thenReturn(runAttemptCount)

        return workerParameters
    }
}
