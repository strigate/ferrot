package org.strigate.ferrot.work

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.app.provider.DownloadPathProvider
import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.DownloadAudioUseCase
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.DownloadVideoUseCase
import org.strigate.ferrot.domain.usecase.download.GetAllDownloadsUseCase
import org.strigate.ferrot.domain.usecase.downloadaudio.GetAllDownloadAudioFilePathsUseCase
import org.strigate.ferrot.domain.usecase.downloadmetadata.GetAllDownloadThumbnailFilePathsUseCase
import org.strigate.ferrot.domain.usecase.downloadvideo.GetAllDownloadVideoFilePathsUseCase
import org.strigate.ferrot.test.MainDispatcherRule
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteAllOrphanDownloadFilesWorkerTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testDispatcher: TestDispatcher = mainDispatcherRule.testDispatcher
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var appContext: Context

    @Mock
    private lateinit var downloadPathProvider: DownloadPathProvider

    @Mock
    private lateinit var downloadUseCase: DownloadUseCase

    @Mock
    private lateinit var downloadAudioUseCase: DownloadAudioUseCase

    @Mock
    private lateinit var downloadVideoUseCase: DownloadVideoUseCase

    @Mock
    private lateinit var downloadMetadataUseCase: DownloadMetadataUseCase

    @Mock
    private lateinit var getAllDownloadsUseCase: GetAllDownloadsUseCase

    @Mock
    private lateinit var getAllDownloadAudioFilePathsUseCase: GetAllDownloadAudioFilePathsUseCase

    @Mock
    private lateinit var getAllDownloadVideoFilePathsUseCase: GetAllDownloadVideoFilePathsUseCase

    @Mock
    private lateinit var getAllDownloadThumbnailFilePathsUseCase: GetAllDownloadThumbnailFilePathsUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        `when`(downloadUseCase.getAllDownloadsUseCase)
            .thenReturn(getAllDownloadsUseCase)
        `when`(downloadAudioUseCase.getAllDownloadAudioFilePathsUseCase)
            .thenReturn(getAllDownloadAudioFilePathsUseCase)
        `when`(downloadVideoUseCase.getAllDownloadVideoFilePathsUseCase)
            .thenReturn(getAllDownloadVideoFilePathsUseCase)
        `when`(downloadMetadataUseCase.getAllDownloadThumbnailFilePathsUseCase)
            .thenReturn(getAllDownloadThumbnailFilePathsUseCase)
    }

    @Test
    fun doWork_deletesOnlyUnreferencedFilesOutsideProtectedDownloads() = runTest(testDispatcher) {
        val root = temporaryFolder.newFolder("downloads")
        val referenced = File(root, "done/referenced.mp4").apply {
            parentFile?.mkdirs()
            writeText("kept")
        }
        val orphan = File(root, "done/orphan.mp4").apply { writeText("deleted") }
        val protected = File(root, "active/protected.part").apply {
            parentFile?.mkdirs()
            writeText("active")
        }
        stubPaths(root)
        `when`(downloadPathProvider.uidDir("active"))
            .thenReturn(File(root, "active"))
        `when`(getAllDownloadAudioFilePathsUseCase.invoke())
            .thenReturn(listOf(referenced.absolutePath))
        `when`(getAllDownloadVideoFilePathsUseCase.invoke())
            .thenReturn(emptyList())
        `when`(getAllDownloadThumbnailFilePathsUseCase.invoke())
            .thenReturn(emptyList())
        `when`(getAllDownloadsUseCase.invoke())
            .thenReturn(
                listOf(download(uid = "active", status = DownloadStatus.DOWNLOADING))
            )

        val result = doWorkWithLogMock()

        assertTrue(result is ListenableWorker.Result.Success)
        assertTrue(referenced.exists())
        assertFalse(orphan.exists())
        assertTrue(protected.exists())
    }

    @Test
    fun doWork_succeedsWhenRootDoesNotExist() = runTest(testDispatcher) {
        val missingRoot = File(temporaryFolder.root, "missing")
        stubPaths(missingRoot)
        stubNoReferencedFilesOrDownloads()

        val result = doWorkWithLogMock()

        assertTrue(result is ListenableWorker.Result.Success)
        assertFalse(missingRoot.exists())
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private suspend fun doWorkWithLogMock() = withContext(Dispatchers.IO) {
        mockStatic(Log::class.java).use {
            createWorker().doWork()
        }
    }

    private fun stubPaths(root: File) {
        `when`(downloadPathProvider.outputDir())
            .thenReturn(root)
    }

    private suspend fun stubNoReferencedFilesOrDownloads() {
        `when`(getAllDownloadAudioFilePathsUseCase.invoke())
            .thenReturn(emptyList())
        `when`(getAllDownloadVideoFilePathsUseCase.invoke())
            .thenReturn(emptyList())
        `when`(getAllDownloadThumbnailFilePathsUseCase.invoke())
            .thenReturn(emptyList())
        `when`(getAllDownloadsUseCase.invoke())
            .thenReturn(emptyList())
    }

    private fun createWorker() = DeleteAllOrphanDownloadFilesWorker(
        appContext = appContext,
        workerParameters = mockWorkerParameters(),
        downloadPathProvider = downloadPathProvider,
        downloadUseCase = downloadUseCase,
        downloadAudioUseCase = downloadAudioUseCase,
        downloadVideoUseCase = downloadVideoUseCase,
        downloadMetadataUseCase = downloadMetadataUseCase,
    )

    private fun download(uid: String, status: DownloadStatus) = Download(
        id = 1L,
        uid = uid,
        url = "https://example.com/video",
        status = status,
        seen = false,
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
