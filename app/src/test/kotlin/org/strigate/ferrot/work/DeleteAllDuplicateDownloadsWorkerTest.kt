package org.strigate.ferrot.work

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadMetadata
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.model.DownloadVideo
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.DownloadVideoUseCase
import org.strigate.ferrot.domain.usecase.combined.DeleteDownloadAndRelatedCombinedUseCase
import org.strigate.ferrot.domain.usecase.download.GetAllDownloadsUseCase
import org.strigate.ferrot.domain.usecase.downloadmetadata.GetDownloadIdsBySourceAndVideoIdUseCase
import org.strigate.ferrot.domain.usecase.downloadmetadata.GetDownloadMetadataByIdAsFlowUseCase
import org.strigate.ferrot.domain.usecase.downloadvideo.GetDownloadIdsBySha256UseCase
import org.strigate.ferrot.domain.usecase.downloadvideo.GetDownloadVideoByDownloadIdAsFlowUseCase
import org.strigate.ferrot.test.MainDispatcherRule
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteAllDuplicateDownloadsWorkerTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val testDispatcher: TestDispatcher = mainDispatcherRule.testDispatcher
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var appContext: Context

    @Mock
    private lateinit var downloadUseCase: DownloadUseCase

    @Mock
    private lateinit var downloadVideoUseCase: DownloadVideoUseCase

    @Mock
    private lateinit var downloadMetadataUseCase: DownloadMetadataUseCase

    @Mock
    private lateinit var getAllDownloadsUseCase: GetAllDownloadsUseCase

    @Mock
    private lateinit var getDownloadMetadataByIdAsFlowUseCase: GetDownloadMetadataByIdAsFlowUseCase

    @Mock
    private lateinit var getDownloadIdsBySourceAndVideoIdUseCase: GetDownloadIdsBySourceAndVideoIdUseCase

    @Mock
    private lateinit var getDownloadVideoByDownloadIdAsFlowUseCase: GetDownloadVideoByDownloadIdAsFlowUseCase

    @Mock
    private lateinit var getDownloadIdsBySha256UseCase: GetDownloadIdsBySha256UseCase

    @Mock
    private lateinit var deleteDownloadAndRelatedCombinedUseCase: DeleteDownloadAndRelatedCombinedUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        `when`(downloadUseCase.getAllDownloadsUseCase)
            .thenReturn(getAllDownloadsUseCase)
        `when`(downloadMetadataUseCase.getDownloadMetadataByIdAsFlowUseCase)
            .thenReturn(getDownloadMetadataByIdAsFlowUseCase)
        `when`(downloadMetadataUseCase.getDownloadIdsBySourceAndVideoIdUseCase)
            .thenReturn(getDownloadIdsBySourceAndVideoIdUseCase)
        `when`(downloadVideoUseCase.getDownloadVideoByDownloadIdAsFlowUseCase)
            .thenReturn(getDownloadVideoByDownloadIdAsFlowUseCase)
        `when`(downloadVideoUseCase.getDownloadIdsBySha256UseCase)
            .thenReturn(getDownloadIdsBySha256UseCase)
    }

    @Test
    fun doWork_deletesOlderCompletedMetadataDuplicates() = runTest(testDispatcher) {
        `when`(getAllDownloadsUseCase.invoke())
            .thenReturn(
                listOf(
                    download(1L, DownloadStatus.COMPLETED),
                    download(2L, DownloadStatus.COMPLETED),
                    download(3L, DownloadStatus.FAILED),
                )
            )
        `when`(getDownloadMetadataByIdAsFlowUseCase.invoke(1L))
            .thenReturn(flowOf(metadata(1L)))
        `when`(getDownloadMetadataByIdAsFlowUseCase.invoke(2L))
            .thenReturn(flowOf(metadata(2L)))
        `when`(getDownloadIdsBySourceAndVideoIdUseCase.invoke("youtube", "abc"))
            .thenReturn(listOf(1L, 2L, 3L))
        `when`(deleteDownloadAndRelatedCombinedUseCase.invoke(1L))
            .thenReturn(true)
        `when`(getDownloadVideoByDownloadIdAsFlowUseCase.invoke(2L))
            .thenReturn(flowOf(null))

        doWorkWithLogMock()

        verify(deleteDownloadAndRelatedCombinedUseCase).invoke(1L)
        verify(deleteDownloadAndRelatedCombinedUseCase, never()).invoke(2L)
        verify(deleteDownloadAndRelatedCombinedUseCase, never()).invoke(3L)
    }

    @Test
    fun doWork_deletesOlderCompletedShaDuplicates() = runTest(testDispatcher) {
        `when`(getAllDownloadsUseCase.invoke())
            .thenReturn(
                listOf(
                    download(4L, DownloadStatus.COMPLETED),
                    download(7L, DownloadStatus.COMPLETED)
                )
            )
        `when`(getDownloadMetadataByIdAsFlowUseCase.invoke(4L))
            .thenReturn(flowOf(null))
        `when`(getDownloadMetadataByIdAsFlowUseCase.invoke(7L))
            .thenReturn(flowOf(null))
        `when`(getDownloadVideoByDownloadIdAsFlowUseCase.invoke(4L))
            .thenReturn(flowOf(video(4L)))
        `when`(getDownloadVideoByDownloadIdAsFlowUseCase.invoke(7L))
            .thenReturn(flowOf(video(7L)))
        `when`(getDownloadIdsBySha256UseCase.invoke("sha"))
            .thenReturn(listOf(4L, 7L))
        `when`(deleteDownloadAndRelatedCombinedUseCase.invoke(4L))
            .thenReturn(true)

        doWorkWithLogMock()

        verify(deleteDownloadAndRelatedCombinedUseCase).invoke(4L)
        verify(deleteDownloadAndRelatedCombinedUseCase, never()).invoke(7L)
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

    private fun createWorker() = DeleteAllDuplicateDownloadsWorker(
        appContext = appContext,
        workerParameters = mockWorkerParameters(),
        downloadUseCase = downloadUseCase,
        downloadVideoUseCase = downloadVideoUseCase,
        downloadMetadataUseCase = downloadMetadataUseCase,
        deleteDownloadAndRelatedCombinedUseCase = deleteDownloadAndRelatedCombinedUseCase,
    )

    private fun download(id: Long, status: DownloadStatus) = Download(
        id = id,
        uid = "uid-$id",
        url = "https://example.com/$id",
        status = status,
        seen = false,
    )

    private fun metadata(downloadId: Long) = DownloadMetadata(
        downloadId = downloadId,
        videoId = "abc",
        source = "youtube",
        title = null,
        thumbnailFilePath = null,
        durationSeconds = null,
    )

    private fun video(downloadId: Long) = DownloadVideo(
        downloadId = downloadId,
        filePath = "/tmp/video-$downloadId.mp4",
        fileExtension = "mp4",
        sha256 = "sha",
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
