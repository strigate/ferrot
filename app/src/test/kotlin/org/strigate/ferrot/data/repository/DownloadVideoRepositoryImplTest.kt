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
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.data.local.dao.DownloadVideoDao
import org.strigate.ferrot.data.local.entity.DownloadVideoEntity
import org.strigate.ferrot.domain.model.DownloadVideo

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadVideoRepositoryImplTest {
    private lateinit var autoCloseable: AutoCloseable
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var downloadVideoDao: DownloadVideoDao

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        autoCloseable.close()
    }

    @Test
    fun save_insertsMappedEntity() = runTest(testDispatcher) {
        val repository = DownloadVideoRepositoryImpl(downloadVideoDao)
        `when`(downloadVideoDao.insertReplace(sampleEntity()))
            .thenReturn(5L)

        val result = repository.save(sampleVideo())
        assertEquals(5L, result)

        verify(downloadVideoDao).insertReplace(sampleEntity())
    }

    @Test
    fun getByDownloadIdAsFlow_mapsEntityToDomain() = runTest(testDispatcher) {
        `when`(downloadVideoDao.getByDownloadIdAsFlow(3L))
            .thenReturn(flowOf(sampleEntity(downloadId = 3L)))

        val repository = DownloadVideoRepositoryImpl(downloadVideoDao)
        val result = repository.getByDownloadIdAsFlow(3L).first()

        assertEquals(sampleVideo(downloadId = 3L), result)
    }

    @Test
    fun getByDownloadIdAsFlow_returnsNull_whenDaoEmitsNull() = runTest(testDispatcher) {
        `when`(downloadVideoDao.getByDownloadIdAsFlow(3L))
            .thenReturn(flowOf(null))

        val repository = DownloadVideoRepositoryImpl(downloadVideoDao)
        assertNull(repository.getByDownloadIdAsFlow(3L).first())
    }

    @Test
    fun queryMethods_delegateToDao() = runTest(testDispatcher) {
        `when`(downloadVideoDao.getDownloadIdsBySha256("sha")).thenReturn(listOf(2L, 8L))
        `when`(downloadVideoDao.getAllFilePaths()).thenReturn(listOf("/tmp/video.mp4"))
        `when`(downloadVideoDao.deleteByDownloadId(3L)).thenReturn(1)
        val repository = DownloadVideoRepositoryImpl(downloadVideoDao)

        assertEquals(listOf(2L, 8L), repository.getDownloadIdsBySha256("sha"))
        assertEquals(listOf("/tmp/video.mp4"), repository.getAllFilePaths())
        assertEquals(1, repository.deleteByDownloadId(3L))

        verify(downloadVideoDao).getDownloadIdsBySha256("sha")
        verify(downloadVideoDao).getAllFilePaths()
        verify(downloadVideoDao).deleteByDownloadId(3L)
    }

    private fun sampleVideo(downloadId: Long = 1L) = DownloadVideo(
        downloadId = downloadId,
        filePath = "/tmp/video.mp4",
        fileExtension = "mp4",
        sha256 = "sha",
    )

    private fun sampleEntity(downloadId: Long = 1L) = DownloadVideoEntity(
        id = 0L,
        downloadId = downloadId,
        filePath = "/tmp/video.mp4",
        fileExtension = "mp4",
        sha256 = "sha",
    )
}
