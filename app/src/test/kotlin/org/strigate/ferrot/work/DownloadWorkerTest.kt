package org.strigate.ferrot.work

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.impl.WorkManagerImpl
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.CALLS_REAL_METHODS
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.Mockito.withSettings
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.R
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.app.Constants.Work.Name.KEY_ID
import org.strigate.ferrot.app.NotificationService
import org.strigate.ferrot.app.actions.DownloadNotificationActionType
import org.strigate.ferrot.app.actions.buildDownloadNotificationAction
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
import org.strigate.ferrot.domain.usecase.download.MetadataRemovalException
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
import java.io.File
import java.util.UUID

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

    @Mock
    private lateinit var resolveCookieSetForUrlUseCase: ResolveCookieSetForUrlUseCase

    @Mock
    private lateinit var updateDownloadErrorMessageUseCase: UpdateDownloadErrorMessageUseCase

    @Mock
    private lateinit var updateDownloadStartedAtUseCase: UpdateDownloadStartedAtUseCase

    @Mock
    private lateinit var updateDownloadCompletedAtUseCase: UpdateDownloadCompletedAtUseCase

    @Mock
    private lateinit var getVideoInfoUseCase: GetVideoInfoUseCase

    @Mock
    private lateinit var getAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase: GetAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase

    @Mock
    private lateinit var notificationAction: NotificationCompat.Action

    @Mock
    private lateinit var workManager: WorkManagerImpl

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        logMock = mockStatic(Log::class.java)

        `when`(appContext.getString(R.string.download_failed))
            .thenReturn("failed")
        `when`(appContext.getString(R.string.notification_text_downloading))
            .thenReturn("downloading")
        `when`(downloadUseCase.getDownloadByIdUseCase)
            .thenReturn(getDownloadByIdUseCase)
        `when`(downloadUseCase.updateDownloadStatusUseCase)
            .thenReturn(updateDownloadStatusUseCase)
        `when`(downloadUseCase.deleteDownloadFilesUseCase)
            .thenReturn(deleteDownloadFilesUseCase)
        `when`(downloadProgressUseCase.updateDownloadProgressUseCase)
            .thenReturn(updateDownloadProgressUseCase)
        `when`(cookieSetUseCase.resolveCookieSetForUrlUseCase)
            .thenReturn(resolveCookieSetForUrlUseCase)
        `when`(downloadUseCase.updateDownloadErrorMessageUseCase)
            .thenReturn(updateDownloadErrorMessageUseCase)
        `when`(downloadUseCase.updateDownloadStartedAtUseCase)
            .thenReturn(updateDownloadStartedAtUseCase)
        `when`(downloadUseCase.updateDownloadCompletedAtUseCase)
            .thenReturn(updateDownloadCompletedAtUseCase)
        `when`(youtubeDlAndroidUseCase.getVideoInfoUseCase)
            .thenReturn(getVideoInfoUseCase)
        `when`(settingsUseCase.getAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase)
            .thenReturn(getAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase)
    }

    @Test
    fun doWork_preservesState_whenIdIsInvalid() = runTest(testDispatcher) {
        val result = createWorker(downloadId = -1L).doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        verify(getDownloadByIdUseCase, never())
            .invoke(-1L)
        verify(updateDownloadStatusUseCase, never())
            .invoke(-1L, DownloadStatus.FAILED)
    }

    @Test
    fun doWork_marksFailed_whenDownloadIsMissing() = runTest(testDispatcher) {
        `when`(getDownloadByIdUseCase.invoke(42L))
            .thenReturn(null)

        val result = createWorker(downloadId = 42L).doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        verify(deleteDownloadFilesUseCase)
            .invoke(42L)
        verify(updateDownloadProgressUseCase)
            .invoke(
                id = 42L,
                progressPercent = 0f,
                bytesDownloaded = 0L,
                etaSeconds = null,
            )
        verify(updateDownloadStatusUseCase)
            .invoke(42L, DownloadStatus.FAILED)
    }

    @Test
    fun doWork_failsWithoutLookup_whenAttemptsAreExhausted() = runTest(testDispatcher) {
        val result = createWorker(downloadId = 42L, runAttemptCount = 21).doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        verify(getDownloadByIdUseCase, never())
            .invoke(42L)
    }

    @Test
    fun doWork_recoversFailedNotification_whenForegroundIsDenied() = runTest(testDispatcher) {
        `when`(getDownloadByIdUseCase.invoke(42L))
            .thenReturn(download(DownloadStatus.DOWNLOADING))

        val exception = mock(ForegroundServiceStartNotAllowedException::class.java)
        `when`(exception.message)
            .thenReturn("startForegroundService() not allowed due to mAllowStartForeground false")
        val events = mutableListOf<String>()
        doAnswer {
            events += "recorded failed"
            true
        }.`when`(updateDownloadStatusUseCase)
            .invoke(42L, DownloadStatus.FAILED)
        val worker = createForegroundWorker(
            onForegroundEnabled = { throw exception },
            onExistingForegroundUpdated = { title -> events += title },
            onForegroundCleared = { events += "cleared" },
        )

        val result = runForegroundWorker(worker)

        assertTrue(result is ListenableWorker.Result.Failure)
        assertEquals(listOf("failed", "recorded failed", "cleared"), events)
        verify(updateDownloadStatusUseCase)
            .invoke(42L, DownloadStatus.FAILED)
        verify(updateDownloadErrorMessageUseCase)
            .invoke(42L, exception.message)
        verify(deleteDownloadFilesUseCase)
            .invoke(42L)
        verify(cookieFileStore)
            .delete(null)
    }

    @Test
    fun doWork_doesNotPostForeground_whenDownloadIsCompleted() = runTest(testDispatcher) {
        `when`(getDownloadByIdUseCase.invoke(42L))
            .thenReturn(download(DownloadStatus.COMPLETED))

        val result = createWorker(downloadId = 42L).doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        verifyNoInteractions(cookieFileStore, notificationService, updateDownloadStatusUseCase)
        verify(appContext, never())
            .getString(R.string.notification_text_downloading)
    }

    @Test
    fun doWork_preservesStateAndFiles_whenStartupIsCancelled() = runTest(testDispatcher) {
        val cancellation = CancellationException("System interrupted download")

        `when`(getDownloadByIdUseCase.invoke(42L))
            .thenReturn(download(DownloadStatus.DOWNLOADING))
        `when`(updateDownloadStatusUseCase.invoke(42L, DownloadStatus.METADATA))
            .thenThrow(cancellation)

        val exception = assertCancelled { createWorker(downloadId = 42L).doWork() }

        assertEquals(cancellation.message, exception.message)

        verify(updateDownloadStatusUseCase, never())
            .invoke(42L, DownloadStatus.FAILED)
        verify(updateDownloadStatusUseCase, never())
            .invoke(42L, DownloadStatus.STOPPED)
        verifyNoInteractions(
            deleteDownloadFilesUseCase,
            updateDownloadProgressUseCase,
            notificationService,
        )
    }

    @Test
    fun doWork_preservesState_whenStoppedWorkerReportsAnError() = runTest(testDispatcher) {
        `when`(getDownloadByIdUseCase.invoke(42L))
            .thenReturn(download(DownloadStatus.DOWNLOADING))
        `when`(updateDownloadStatusUseCase.invoke(42L, DownloadStatus.METADATA))
            .thenThrow(IllegalStateException("Process interrupted"))

        var foregroundCleared = false
        val worker = createForegroundWorker(onForegroundCleared = { foregroundCleared = true })
        doReturn(true).`when`(worker)
            .isStopped

        assertCancelled { worker.doWork() }
        assertFalse(foregroundCleared)

        verify(updateDownloadStatusUseCase, never())
            .invoke(42L, DownloadStatus.FAILED)
        verifyNoInteractions(
            deleteDownloadFilesUseCase,
            updateDownloadProgressUseCase,
            notificationService,
        )
    }

    @Test
    fun doWork_activatesDownloadBeforeForeground() = runTest(testDispatcher) {
        val download = download(DownloadStatus.FAILED)
        val events = mutableListOf<String>()

        `when`(getDownloadByIdUseCase.invoke(download.id))
            .thenReturn(download)
        `when`(appContext.getString(R.string.notification_text_downloading))
            .thenReturn("downloading")
        `when`(resolveCookieSetForUrlUseCase.invoke(download.url))
            .thenThrow(CancellationException("Work replaced during cookie preparation"))
        doAnswer {
            events += "active state"
            true
        }.`when`(updateDownloadStatusUseCase)
            .invoke(download.id, DownloadStatus.METADATA)

        val worker = createForegroundWorker(
            onForegroundEnabled = { events += "foreground" },
            onForegroundCleared = { events += "cleared" },
        )

        assertCancelled { runForegroundWorker(worker) }

        assertEquals(listOf("active state", "foreground", "cleared"), events)
        verify(updateDownloadStatusUseCase, never())
            .invoke(download.id, DownloadStatus.FAILED)
        verifyNoInteractions(
            deleteDownloadFilesUseCase,
            updateDownloadProgressUseCase,
            notificationService,
        )
        verify(cookieFileStore)
            .delete(null)
    }

    @Test
    fun doWork_recordsFailure_whenFileCleanupThrows() = runTest(testDispatcher) {
        `when`(getDownloadByIdUseCase.invoke(42L))
            .thenReturn(null)
        `when`(deleteDownloadFilesUseCase.invoke(42L))
            .thenThrow(IllegalStateException("Storage unavailable"))

        val result = createWorker(downloadId = 42L).doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        verify(updateDownloadStatusUseCase)
            .invoke(42L, DownloadStatus.FAILED)
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
        ).thenThrow(IllegalStateException("Progress unavailable"))

        val result = createWorker(downloadId = 42L).doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        verify(updateDownloadStatusUseCase)
            .invoke(42L, DownloadStatus.FAILED)
    }

    @Test
    fun doWork_doesNotRecordFailure_whenCleanupIsCancelled() = runTest(testDispatcher) {
        `when`(getDownloadByIdUseCase.invoke(42L))
            .thenReturn(null)
        `when`(deleteDownloadFilesUseCase.invoke(42L))
            .thenThrow(CancellationException("Work replaced"))

        assertCancelled { createWorker(downloadId = 42L).doWork() }

        verify(updateDownloadStatusUseCase, never())
            .invoke(42L, DownloadStatus.FAILED)
        verifyNoInteractions(updateDownloadProgressUseCase)
    }

    @Test
    fun doWork_completesWithVideoAndAudio() = runTest(testDispatcher) {
        val media = prepareMediaDownload()

        val result = runForegroundWorker(media.worker)

        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(listOf("foreground complete", "recorded complete"), media.events.takeLast(2))
        assertVideoSaved(media)
        assertEquals(media.audioFile.absolutePath, media.savedAudio.single().filePath)
        assertTrue(media.audioFile.exists())

        verify(updateDownloadStatusUseCase)
            .invoke(42L, DownloadStatus.COMPLETED)
        verify(deleteDownloadFilesUseCase)
            .invoke(42L)
        verify(updateDownloadStatusUseCase, never())
            .invoke(42L, DownloadStatus.FAILED)
    }

    @Test
    fun doWork_keepsVideo_whenAudioProcessFails() = runTest(testDispatcher) {
        val media = prepareMediaDownload(audioFailure = YoutubeDL.CanceledException())

        val result = runForegroundWorker(media.worker)

        assertTrue(result is ListenableWorker.Result.Success)
        assertVideoSaved(media)
        assertTrue(media.savedAudio.isEmpty())
        assertFalse(media.audioFile.exists())

        verify(updateDownloadStatusUseCase)
            .invoke(42L, DownloadStatus.COMPLETED)
        verify(deleteDownloadFilesUseCase)
            .invoke(42L)
        verify(updateDownloadStatusUseCase, never())
            .invoke(42L, DownloadStatus.FAILED)
        verify(updateDownloadStatusUseCase, never())
            .invoke(42L, DownloadStatus.STOPPED)
    }

    @Test
    fun doWork_failsWhenAudioMetadataCleanupFails() = runTest(testDispatcher) {
        val media = prepareMediaDownload(audioFailure = MetadataRemovalException())
        val result = runForegroundWorker(media.worker)

        assertTrue(result is ListenableWorker.Result.Failure)
        assertTrue(media.savedAudio.isEmpty())
        verify(updateDownloadStatusUseCase)
            .invoke(42L, DownloadStatus.FAILED)
        verify(updateDownloadStatusUseCase, never())
            .invoke(42L, DownloadStatus.COMPLETED)
    }

    @Test
    fun doWork_doesNotComplete_whenAudioCoroutineIsCancelled() = runTest(testDispatcher) {
        val media = prepareMediaDownload(
            audioFailure = CancellationException("System interrupted audio"),
        )

        assertCancelled { runForegroundWorker(media.worker) }

        assertVideoSaved(media)
        assertTrue(media.savedAudio.isEmpty())
        assertTrue("foreground complete" !in media.events)

        verify(updateDownloadStatusUseCase, never())
            .invoke(42L, DownloadStatus.COMPLETED)
        verify(updateDownloadStatusUseCase, never())
            .invoke(42L, DownloadStatus.FAILED)
        verify(updateDownloadStatusUseCase, never())
            .invoke(42L, DownloadStatus.STOPPED)
        verify(deleteDownloadFilesUseCase)
            .invoke(42L)
    }

    @Test
    fun doWork_updatesFailedForegroundBeforeRecordingFailure() = runTest(testDispatcher) {
        val events = mutableListOf<String>()
        val worker = prepareFailedDownload(events)
        val result = runForegroundWorker(worker)

        assertTrue(result is ListenableWorker.Result.Failure)
        assertEquals(
            listOf("foreground downloading", "foreground failed", "recorded failed", "cleared"),
            events,
        )
        verify(updateDownloadStatusUseCase)
            .invoke(42L, DownloadStatus.FAILED)
    }

    @Test
    fun doWork_recordsFailure_whenForegroundUpdateFails() = runTest(testDispatcher) {
        val events = mutableListOf<String>()
        val worker = prepareFailedDownload(events, terminalUpdateFails = true)
        val result = runForegroundWorker(worker)

        assertTrue(result is ListenableWorker.Result.Failure)
        assertEquals(
            listOf(
                "foreground downloading",
                "foreground failed",
                "recovered failed",
                "recorded failed",
                "cleared",
            ),
            events,
        )
        verify(updateDownloadStatusUseCase)
            .invoke(42L, DownloadStatus.FAILED)
    }

    @After
    fun tearDown() {
        logMock?.close()
        autoCloseable.close()
    }

    private suspend fun prepareFailedDownload(
        events: MutableList<String>,
        terminalUpdateFails: Boolean = false,
    ): DownloadWorker {
        val download = download(DownloadStatus.QUEUED)

        `when`(getDownloadByIdUseCase.invoke(download.id))
            .thenReturn(download)
        `when`(appContext.getString(R.string.notification_text_downloading))
            .thenReturn("downloading")
        `when`(appContext.getString(R.string.download_failed))
            .thenReturn("failed")
        `when`(resolveCookieSetForUrlUseCase.invoke(download.url))
            .thenThrow(IllegalStateException("Cookie preparation failed"))
        doAnswer {
            events += "recorded failed"
            true
        }.`when`(updateDownloadStatusUseCase)
            .invoke(download.id, DownloadStatus.FAILED)

        return createForegroundWorker(
            onForegroundEnabled = { events += "foreground downloading" },
            onForegroundUpdated = { title ->
                events += "foreground $title"
                if (terminalUpdateFails) throw IllegalStateException("Service unavailable")
            },
            onExistingForegroundUpdated = { title -> events += "recovered $title" },
            onForegroundCleared = { events += "cleared" },
        )
    }

    private suspend fun prepareMediaDownload(audioFailure: Throwable? = null): MediaDownload {
        val download = download(DownloadStatus.QUEUED).copy(archived = true)
        val directory = temporaryFolder.newFolder()
        val videoFile = File(directory, "video.mp4")
        val audioFile = File(directory, "audio.m4a")
        val savedVideos = mutableListOf<DownloadVideo>()
        val savedAudio = mutableListOf<DownloadAudio>()
        val events = mutableListOf<String>()

        `when`(getDownloadByIdUseCase.invoke(download.id))
            .thenReturn(download)
        `when`(appContext.getString(R.string.notification_text_downloading))
            .thenReturn("downloading")
        `when`(appContext.getString(R.string.download_complete))
            .thenReturn("complete")
        `when`(downloadPathProvider.uidDir(download.uid))
            .thenReturn(directory)
        `when`(getVideoInfoUseCase.invoke(download.url, null))
            .thenThrow(IllegalStateException("Metadata unavailable"))
        `when`(getAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase.invoke())
            .thenReturn(flowOf(false))
        doAnswer {
            events += "recorded complete"
            true
        }.`when`(updateDownloadStatusUseCase)
            .invoke(download.id, DownloadStatus.COMPLETED)

        val saveVideo = mock(SaveDownloadVideoUseCase::class.java) { invocation ->
            if (invocation.method.name == "invoke") {
                savedVideos += invocation.getArgument<DownloadVideo>(0)
                true
            } else {
                RETURNS_DEFAULTS.answer(invocation)
            }
        }
        `when`(downloadVideoUseCase.saveDownloadVideoUseCase)
            .thenReturn(saveVideo)
        val saveAudio = mock(SaveDownloadAudioUseCase::class.java) { invocation ->
            if (invocation.method.name == "invoke") {
                savedAudio += invocation.getArgument<DownloadAudio>(0)
                true
            } else {
                RETURNS_DEFAULTS.answer(invocation)
            }
        }
        `when`(downloadAudioUseCase.saveDownloadAudioUseCase)
            .thenReturn(saveAudio)
        `when`(youtubeDlAndroidUseCase.downloadWithProgressUseCase)
            .thenReturn(createMediaDownloadFlow(videoFile, audioFile, audioFailure))

        return MediaDownload(
            worker = createForegroundWorker(onForegroundUpdated = { title -> events += "foreground $title" }),
            videoFile = videoFile,
            audioFile = audioFile,
            savedVideos = savedVideos,
            savedAudio = savedAudio,
            events = events,
        )
    }

    private fun createMediaDownloadFlow(
        videoFile: File,
        audioFile: File,
        audioFailure: Throwable?,
    ): DownloadWithProgressUseCase = mock(DownloadWithProgressUseCase::class.java) { invocation ->
        if (invocation.method.name != "invoke") {
            RETURNS_DEFAULTS.answer(invocation)
        } else {
            val mediaType = invocation.getArgument<DownloadMediaType>(5)
            val reportOutput = invocation.getArgument<((String) -> Unit)?>(7)
            flow<DownloadWithProgressUseCase.DownloadTick> {
                if (mediaType == DownloadMediaType.AUDIO && audioFailure != null) throw audioFailure
                val outputFile = if (mediaType == DownloadMediaType.VIDEO) videoFile else audioFile
                outputFile.writeText("downloaded media content")
                reportOutput?.invoke(outputFile.absolutePath)
            }
        }
    }

    private fun createForegroundWorker(
        onForegroundEnabled: () -> Unit = {},
        onForegroundUpdated: (String) -> Unit = {},
        onExistingForegroundUpdated: (String) -> Unit = {},
        onForegroundCleared: () -> Unit = {},
    ): DownloadWorker = mock(
        DownloadWorker::class.java,
        withSettings().spiedInstance(createWorker(42L)).defaultAnswer { invocation ->
            when (invocation.method.name) {
                "enableForeground" -> onForegroundEnabled()
                "updateForeground" -> onForegroundUpdated(invocation.getArgument(0))
                "updateExistingForegroundNotification" -> onExistingForegroundUpdated(
                    invocation.getArgument(
                        0,
                    ),
                )

                "clearForegroundNotification" -> onForegroundCleared()
                else -> invocation.callRealMethod()
            }
        },
    )

    private suspend fun runForegroundWorker(worker: DownloadWorker): ListenableWorker.Result {
        val actionsClass = Class.forName(
            "org.strigate.ferrot.app.actions.DownloadNotificationActionsKt",
        )
        return mockStatic(actionsClass, CALLS_REAL_METHODS).use { actions ->
            for (actionType in listOf(
                DownloadNotificationActionType.STOP,
                DownloadNotificationActionType.RETRY,
                DownloadNotificationActionType.DELETE,
            )) {
                actions.`when`<NotificationCompat.Action> {
                    buildDownloadNotificationAction(appContext, 42L, actionType)
                }.thenReturn(notificationAction)
            }
            mockStatic(WorkManagerImpl::class.java).use { workManagerStatic ->
                workManagerStatic.`when`<WorkManagerImpl> {
                    WorkManagerImpl.getInstance(appContext)
                }.thenReturn(workManager)
                mockStatic(Class.forName("org.strigate.ferrot.extensions.ContextKt")).use {
                    worker.doWork()
                }
            }
        }
    }

    private suspend fun assertCancelled(block: suspend () -> Any?): CancellationException {
        try {
            block()
        } catch (exception: CancellationException) {
            return exception
        }
        throw AssertionError("Expected cancellation")
    }

    private fun assertVideoSaved(media: MediaDownload) {
        assertEquals(media.videoFile.absolutePath, media.savedVideos.single().filePath)
        assertTrue(media.videoFile.exists())
    }

    private data class MediaDownload(
        val worker: DownloadWorker,
        val videoFile: File,
        val audioFile: File,
        val savedVideos: List<DownloadVideo>,
        val savedAudio: List<DownloadAudio>,
        val events: List<String>,
    )

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
