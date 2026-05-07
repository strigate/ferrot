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
import org.strigate.ferrot.data.local.dao.DownloadAudioDao
import org.strigate.ferrot.data.local.entity.DownloadAudioEntity
import org.strigate.ferrot.domain.model.DownloadAudio

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadAudioRepositoryImplTest {
    private lateinit var autoCloseable: AutoCloseable
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    @Mock
    private lateinit var downloadAudioDao: DownloadAudioDao

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun save_insertsMappedEntity() = runTest(testDispatcher) {
        val repository = DownloadAudioRepositoryImpl(downloadAudioDao)

        `when`(downloadAudioDao.insertReplace(sampleEntity()))
            .thenReturn(4L)

        val result = repository.save(sampleAudio())
        assertEquals(4L, result)
        verify(downloadAudioDao)
            .insertReplace(sampleEntity())
    }

    @Test
    fun getByDownloadIdAsFlow_mapsEntityToDomain() = runTest(testDispatcher) {
        `when`(downloadAudioDao.getByDownloadIdAsFlow(3L))
            .thenReturn(flowOf(sampleEntity(downloadId = 3L)))

        val repository = DownloadAudioRepositoryImpl(downloadAudioDao)
        val result = repository.getByDownloadIdAsFlow(3L).first()
        assertEquals(sampleAudio(downloadId = 3L), result)
    }

    @Test
    fun getByDownloadIdAsFlow_returnsNull_whenDaoEmitsNull() = runTest(testDispatcher) {
        `when`(downloadAudioDao.getByDownloadIdAsFlow(3L))
            .thenReturn(flowOf(null))

        val repository = DownloadAudioRepositoryImpl(downloadAudioDao)
        assertNull(repository.getByDownloadIdAsFlow(3L).first())
    }

    @Test
    fun queryMethods_delegateToDao() = runTest(testDispatcher) {
        `when`(downloadAudioDao.getAllFilePaths())
            .thenReturn(listOf("/tmp/audio.m4a"))
        `when`(downloadAudioDao.deleteByDownloadId(3L))
            .thenReturn(1)

        val repository = DownloadAudioRepositoryImpl(downloadAudioDao)
        assertEquals(listOf("/tmp/audio.m4a"), repository.getAllFilePaths())
        assertEquals(1, repository.deleteByDownloadId(3L))

        verify(downloadAudioDao)
            .getAllFilePaths()
        verify(downloadAudioDao)
            .deleteByDownloadId(3L)
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private fun sampleAudio(downloadId: Long = 1L) = DownloadAudio(
        downloadId = downloadId,
        filePath = "/tmp/audio.m4a",
        fileExtension = "m4a",
    )

    private fun sampleEntity(downloadId: Long = 1L) = DownloadAudioEntity(
        id = 0L,
        downloadId = downloadId,
        filePath = "/tmp/audio.m4a",
        fileExtension = "m4a",
    )
}
