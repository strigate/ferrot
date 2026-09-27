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
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.combined.DeleteDownloadAndRelatedCombinedUseCase
import org.strigate.ferrot.domain.usecase.download.GetAllDownloadsUseCase
import org.strigate.ferrot.domain.usecase.download.StopDownloadUseCase
import java.util.UUID

class DeletePendingDownloadsDelayedWorkerTest {
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var downloadUseCase: DownloadUseCase

    @Mock
    private lateinit var getAllDownloads: GetAllDownloadsUseCase

    @Mock
    private lateinit var deleteDownload: DeleteDownloadAndRelatedCombinedUseCase

    @Mock
    private lateinit var stopDownload: StopDownloadUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        `when`(downloadUseCase.getAllDownloadsUseCase)
            .thenReturn(getAllDownloads)
    }

    @Test
    fun doWork_succeedsWithoutDeletingWhenNothingIsPending() = runTest {
        `when`(getAllDownloads())
            .thenReturn(emptyList())
        val parameters = mock(WorkerParameters::class.java)

        `when`(parameters.id)
            .thenReturn(UUID.randomUUID())
        `when`(parameters.inputData)
            .thenReturn(Data.Builder().putLong("key_delay_millis", 0L).build())

        val worker = DeletePendingDownloadsDelayedWorker(
            appContext = context,
            workerParameters = parameters,
            downloadUseCase = downloadUseCase,
            deleteDownloadAndRelatedCombinedUseCase = deleteDownload,
            stopDownloadUseCase = stopDownload,
        )

        val result = withContext(Dispatchers.IO) {
            mockStatic(Log::class.java).use { worker.doWork() }
        }

        assertTrue(result is ListenableWorker.Result.Success)
        verify(deleteDownload, never()).invoke(anyLong())
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }
}
