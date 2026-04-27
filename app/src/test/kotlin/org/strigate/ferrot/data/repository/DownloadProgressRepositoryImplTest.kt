package org.strigate.ferrot.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.data.local.dao.DownloadProgressDao
import org.strigate.ferrot.data.local.entity.DownloadProgressEntity
import org.strigate.ferrot.domain.model.DownloadProgress

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadProgressRepositoryImplTest {
    private lateinit var autoCloseable: AutoCloseable
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var downloadProgressDao: DownloadProgressDao

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }


    @Test
    fun save_insertsMappedEntity() = runTest(testDispatcher) {
        val repository = DownloadProgressRepositoryImpl(downloadProgressDao)
        val progress = sampleDownloadProgress()

        `when`(downloadProgressDao.insertReplace(sampleEntity()))
            .thenReturn(5L)

        val result = repository.save(progress)

        assertEquals(5L, result)
        verify(downloadProgressDao)
            .insertReplace(sampleEntity())
    }

    @Test
    fun getByDownloadIdAsFlow_mapsEntityToDomain() = runTest(testDispatcher) {
        `when`(downloadProgressDao.getByDownloadIdAsFlow(7L))
            .thenReturn(flowOf(sampleEntity(downloadId = 7L)))

        val repository = DownloadProgressRepositoryImpl(downloadProgressDao)
        val result = repository.getByDownloadIdAsFlow(7L).first()
        assertEquals(sampleDownloadProgress(downloadId = 7L), result)
    }

    @Test
    fun getByDownloadIdAsFlow_returnsNull_whenDaoEmitsNull() = runTest(testDispatcher) {
        `when`(downloadProgressDao.getByDownloadIdAsFlow(7L))
            .thenReturn(flowOf(null))

        val repository = DownloadProgressRepositoryImpl(downloadProgressDao)
        assertNull(repository.getByDownloadIdAsFlow(7L).first())
    }

    @Test
    fun updateExpectedBytes_delegatesToDao() = runTest(testDispatcher) {
        doReturn(2)
            .`when`(downloadProgressDao)
            .updateExpectedBytes(eq(7L), eq(4096L), anyLong())

        val repository = DownloadProgressRepositoryImpl(downloadProgressDao)
        val result = repository.updateExpectedBytes(7L, 4096L)

        assertEquals(2, result)
        verify(downloadProgressDao)
            .updateExpectedBytes(eq(7L), eq(4096L), anyLong())
    }

    @Test
    fun deleteByDownloadId_returnsDaoDeleteCount() = runTest(testDispatcher) {
        `when`(downloadProgressDao.deleteByDownloadId(7L))
            .thenReturn(1)

        val repository = DownloadProgressRepositoryImpl(downloadProgressDao)
        val result = repository.deleteByDownloadId(7L)

        assertEquals(1, result)
        verify(downloadProgressDao)
            .deleteByDownloadId(7L)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        autoCloseable.close()
    }

    private fun sampleDownloadProgress(downloadId: Long = 4L) = DownloadProgress(
        downloadId = downloadId,
        updatedAtMillis = 123L,
        progressPercent = 42.5F,
        bytesDownloaded = 2048L,
        etaSeconds = 17L,
        expectedBytes = 4096L,
    )

    private fun sampleEntity(downloadId: Long = 4L) = DownloadProgressEntity(
        downloadId = downloadId,
        updatedAtMillis = 123L,
        progressPercent = 42.5F,
        bytesDownloaded = 2048L,
        etaSeconds = 17L,
        expectedBytes = 4096L,
    )
}
