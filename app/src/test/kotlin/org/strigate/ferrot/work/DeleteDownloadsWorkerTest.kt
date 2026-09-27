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
import org.strigate.ferrot.domain.usecase.combined.DeleteDownloadAndRelatedCombinedUseCase
import org.strigate.ferrot.domain.usecase.download.StopDownloadUseCase
import java.util.UUID

class DeleteDownloadsWorkerTest {
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var deleteDownload: DeleteDownloadAndRelatedCombinedUseCase

    @Mock
    private lateinit var stopDownload: StopDownloadUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun doWork_succeedsWithoutSideEffectsWhenIdsAreMissing() = runTest {
        val parameters = mock(WorkerParameters::class.java)
        `when`(parameters.id)
            .thenReturn(UUID.randomUUID())
        `when`(parameters.inputData)
            .thenReturn(Data.EMPTY)

        val worker = DeleteDownloadsWorker(context, parameters, deleteDownload, stopDownload)

        assertTrue(doWork(worker) is ListenableWorker.Result.Success)
        verify(stopDownload, never()).invoke(anyLong())
        verify(deleteDownload, never()).invoke(anyLong())
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private suspend fun doWork(worker: DeleteDownloadsWorker) = withContext(Dispatchers.IO) {
        mockStatic(Log::class.java).use { worker.doWork() }
    }
}
