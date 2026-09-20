package org.strigate.ferrot.work

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.app.Constants.Work.Name.KEY_ID
import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.combined.DeleteDownloadAndRelatedCombinedUseCase
import org.strigate.ferrot.domain.usecase.download.GetDownloadByIdUseCase
import org.strigate.ferrot.domain.usecase.download.StopDownloadUseCase
import java.util.UUID

class DeletePendingDownloadDelayedWorkerTest {
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var downloadUseCase: DownloadUseCase

    @Mock
    private lateinit var getDownload: GetDownloadByIdUseCase

    @Mock
    private lateinit var deleteDownload: DeleteDownloadAndRelatedCombinedUseCase

    @Mock
    private lateinit var stopDownload: StopDownloadUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        `when`(downloadUseCase.getDownloadByIdUseCase)
            .thenReturn(getDownload)
    }

    @Test
    fun doWork_succeedsWithoutLookupForInvalidId() = runTest {
        assertTrue(doWork(createWorker(-1L)) is ListenableWorker.Result.Success)
        verify(getDownload, never()).invoke(-1L)
    }

    @Test
    fun doWork_doesNotDeleteWhenPendingFlagWasCleared() = runTest {
        `when`(getDownload(8L))
            .thenReturn(
                Download(
                    id = 8L,
                    uid = "uid",
                    url = "https://example.com",
                    status = DownloadStatus.COMPLETED,
                    seen = false,
                )
            )

        assertTrue(doWork(createWorker(8L)) is ListenableWorker.Result.Success)
        verify(stopDownload, never()).invoke(8L)
        verify(deleteDownload, never()).invoke(8L)
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private fun createWorker(downloadId: Long): DeletePendingDownloadDelayedWorker {
        val parameters = mock(WorkerParameters::class.java)
        `when`(parameters.id)
            .thenReturn(UUID.randomUUID())
        `when`(parameters.inputData)
            .thenReturn(Data.Builder().putLong(KEY_ID, downloadId).build())

        return DeletePendingDownloadDelayedWorker(
            appContext = context,
            workerParameters = parameters,
            downloadUseCase = downloadUseCase,
            deleteDownloadAndRelatedCombinedUseCase = deleteDownload,
            stopDownloadUseCase = stopDownload,
        )
    }

    private suspend fun doWork(worker: DeletePendingDownloadDelayedWorker) =
        withContext(Dispatchers.IO) {
            mockStatic(Log::class.java).use { worker.doWork() }
        }
}
