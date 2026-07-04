package org.strigate.ferrot.work

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.app.Constants.Work.Name.KEY_ID
import org.strigate.ferrot.app.NotificationService
import org.strigate.ferrot.app.integration.CookieFileStore
import org.strigate.ferrot.app.provider.DownloadPathProvider
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.CookieSetUseCase
import org.strigate.ferrot.domain.usecase.DownloadAudioUseCase
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadProgressUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.DownloadVideoUseCase
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.YoutubeDlAndroidUseCase
import org.strigate.ferrot.domain.usecase.combined.DeleteDownloadAndRelatedCombinedUseCase
import org.strigate.ferrot.domain.usecase.download.DeleteDownloadFilesUseCase
import org.strigate.ferrot.domain.usecase.download.GetDownloadByIdUseCase
import org.strigate.ferrot.domain.usecase.download.UpdateDownloadStatusUseCase
import org.strigate.ferrot.domain.usecase.downloadprogress.UpdateDownloadProgressUseCase
import org.strigate.ferrot.test.MainDispatcherRule
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadWorkerTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val testDispatcher: TestDispatcher = mainDispatcherRule.testDispatcher
    private lateinit var autoCloseable: AutoCloseable

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

    @After
    fun tearDown() {
        logMock?.close()
        autoCloseable.close()
    }

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
