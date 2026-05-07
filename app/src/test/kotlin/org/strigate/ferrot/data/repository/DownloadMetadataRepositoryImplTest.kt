package org.strigate.ferrot.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.test.MainDispatcherRule
import org.strigate.ferrot.data.local.dao.DownloadMetadataDao
import org.strigate.ferrot.data.local.entity.DownloadMetadataEntity
import org.strigate.ferrot.domain.model.DownloadMetadata

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadMetadataRepositoryImplTest {
    private lateinit var autoCloseable: AutoCloseable
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    @Mock
    private lateinit var downloadMetadataDao: DownloadMetadataDao

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun save_insertsMappedEntity() = runTest(testDispatcher) {
        val repository = DownloadMetadataRepositoryImpl(downloadMetadataDao)
        `when`(downloadMetadataDao.insertReplace(sampleEntity()))
            .thenReturn(6L)

        val result = repository.save(sampleMetadata())

        assertEquals(6L, result)
        verify(downloadMetadataDao)
            .insertReplace(sampleEntity())
    }

    @Test
    fun getByDownloadIdAsFlow_mapsEntityToDomain() = runTest(testDispatcher) {
        `when`(downloadMetadataDao.getByDownloadIdAsFlow(2L))
            .thenReturn(flowOf(sampleEntity(downloadId = 2L)))

        val repository = DownloadMetadataRepositoryImpl(downloadMetadataDao)
        val result = repository.getByDownloadIdAsFlow(2L).first()
        assertEquals(sampleMetadata(downloadId = 2L), result)
    }

    @Test
    fun getByDownloadIdAsFlow_returnsNull_whenDaoEmitsNull() = runTest(testDispatcher) {
        `when`(downloadMetadataDao.getByDownloadIdAsFlow(2L))
            .thenReturn(flowOf(null))

        val repository = DownloadMetadataRepositoryImpl(downloadMetadataDao)
        assertNull(repository.getByDownloadIdAsFlow(2L).first())
    }

    @Test
    fun queryMethods_delegateToDao() = runTest(testDispatcher) {
        `when`(downloadMetadataDao.getDownloadIdsBySourceAndVideoId("youtube", "abc123"))
            .thenReturn(listOf(1L, 9L))
        `when`(downloadMetadataDao.getAllThumbnailFilePaths())
            .thenReturn(listOf("/tmp/one.jpg", "/tmp/two.jpg"))
        `when`(downloadMetadataDao.deleteByDownloadId(2L))
            .thenReturn(1)

        val repository = DownloadMetadataRepositoryImpl(downloadMetadataDao)

        assertEquals(
            listOf(1L, 9L),
            repository.getDownloadIdsBySourceAndVideoId("youtube", "abc123")
        )
        assertEquals(listOf("/tmp/one.jpg", "/tmp/two.jpg"), repository.getAllThumbnailFilePaths())
        assertEquals(1, repository.deleteByDownloadId(2L))

        verify(downloadMetadataDao)
            .getDownloadIdsBySourceAndVideoId("youtube", "abc123")
        verify(downloadMetadataDao)
            .getAllThumbnailFilePaths()
        verify(downloadMetadataDao)
            .deleteByDownloadId(2L)
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private fun sampleMetadata(downloadId: Long = 1L) = DownloadMetadata(
        downloadId = downloadId,
        videoId = "abc123",
        source = "youtube",
        title = "Sample title",
        thumbnailFilePath = "/tmp/thumb.jpg",
        durationSeconds = 91,
    )

    private fun sampleEntity(downloadId: Long = 1L) = DownloadMetadataEntity(
        downloadId = downloadId,
        videoId = "abc123",
        source = "youtube",
        title = "Sample title",
        thumbnailFilePath = "/tmp/thumb.jpg",
        durationSeconds = 91,
    )
}
