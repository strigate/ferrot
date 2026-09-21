package org.strigate.ferrot.work

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.app.Constants.Work.Name.KEY_ID
import org.strigate.ferrot.domain.usecase.combined.RefreshDownloadMetadataCombinedUseCase
import java.util.UUID

class RefreshDownloadMetadataWorkerTest {
    private lateinit var autoCloseable: AutoCloseable

    private lateinit var logMock: MockedStatic<Log>

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var refreshMetadata: RefreshDownloadMetadataCombinedUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        logMock = mockStatic(Log::class.java)
    }

    @Test
    fun doWork_failsForInvalidIdWithoutRefreshing() = runTest {
        val result = createWorker(-1L).doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        verify(refreshMetadata, never()).invoke(-1L)
    }

    @Test
    fun doWork_succeedsWhenMetadataIsRefreshed() = runTest {
        `when`(refreshMetadata(4L))
            .thenReturn(true)

        assertTrue(createWorker(4L).doWork() is ListenableWorker.Result.Success)
        verify(refreshMetadata).invoke(4L)
    }

    @Test
    fun doWork_failsWhenRefreshReturnsFalseOrThrows() = runTest {
        `when`(refreshMetadata(4L))
            .thenReturn(false)
        `when`(refreshMetadata(5L))
            .thenThrow(IllegalStateException("failed"))

        assertTrue(createWorker(4L).doWork() is ListenableWorker.Result.Failure)
        assertTrue(createWorker(5L).doWork() is ListenableWorker.Result.Failure)
    }

    @Test
    fun uniqueWorkName_containsDownloadId() {
        assertTrue(RefreshDownloadMetadataWorker.uniqueWorkName(42L).endsWith("-42"))
    }

    @After
    fun tearDown() {
        logMock.close()
        autoCloseable.close()
    }

    private fun createWorker(downloadId: Long): RefreshDownloadMetadataWorker {
        val parameters = mock(WorkerParameters::class.java)
        `when`(parameters.id)
            .thenReturn(UUID.randomUUID())
        `when`(parameters.inputData)
            .thenReturn(Data.Builder().putLong(KEY_ID, downloadId).build())

        return RefreshDownloadMetadataWorker(context, parameters, refreshMetadata)
    }
}
