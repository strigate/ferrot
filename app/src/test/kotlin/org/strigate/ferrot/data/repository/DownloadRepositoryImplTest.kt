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
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.data.local.dao.DownloadDao
import org.strigate.ferrot.data.local.entity.DownloadEntity
import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.data.local.entity.DownloadStatus as EntityStatus

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadRepositoryImplTest {
    private lateinit var autoCloseable: AutoCloseable
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var downloadDao: DownloadDao

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
        val download = sampleDownload()
        var insertedEntity: DownloadEntity? = null
        doAnswer { invocation ->
            insertedEntity = invocation.getArgument(0)
            12L
        }.`when`(downloadDao).insert(anyObject())
        val repository = DownloadRepositoryImpl(downloadDao)

        val result = repository.save(download)

        assertEquals(12L, result)
        assertEquals(download.id, insertedEntity?.id)
        assertEquals(download.uid, insertedEntity?.uid)
        assertEquals(download.url, insertedEntity?.url)
        assertEquals(EntityStatus.QUEUED, insertedEntity?.status)
        assertEquals(download.seen, insertedEntity?.seen)
        assertEquals(download.errorMessage, insertedEntity?.errorMessage)
        assertEquals(download.completedAtMillis, insertedEntity?.completedAtMillis)
        assertNull(insertedEntity?.startedAtMillis)

        verify(downloadDao).insert(insertedEntity ?: error("Entity not captured"))
    }

    @Test
    fun getAll_mapsEntitiesToDomain() = runTest(testDispatcher) {
        `when`(downloadDao.getAll()).thenReturn(
            listOf(
                sampleEntity(id = 1L, status = EntityStatus.DOWNLOADING),
                sampleEntity(id = 2L, status = EntityStatus.COMPLETED, completedAtMillis = 300L),
            ),
        )

        val repository = DownloadRepositoryImpl(downloadDao)
        val result = repository.getAll()

        assertEquals(
            listOf(
                sampleDownload(id = 1L, status = DownloadStatus.DOWNLOADING),
                sampleDownload(
                    id = 2L,
                    status = DownloadStatus.COMPLETED,
                    completedAtMillis = 300L,
                ),
            ),
            result,
        )
    }

    @Test
    fun getById_returnsMappedDomain_whenDaoReturnsEntity() = runTest(testDispatcher) {
        `when`(downloadDao.getById(4L))
            .thenReturn(sampleEntity(id = 4L, status = EntityStatus.FAILED))

        val repository = DownloadRepositoryImpl(downloadDao)
        val result = repository.getById(4L)
        assertEquals(sampleDownload(id = 4L, status = DownloadStatus.FAILED), result)
    }

    @Test
    fun getById_returnsNull_whenDaoReturnsNull() = runTest(testDispatcher) {
        `when`(downloadDao.getById(4L))
            .thenReturn(null)

        val repository = DownloadRepositoryImpl(downloadDao)
        assertNull(repository.getById(4L))
    }

    @Test
    fun getByIdAsFlow_mapsEntityToDomain() = runTest(testDispatcher) {
        `when`(downloadDao.getByIdAsFlow(8L))
            .thenReturn(flowOf(sampleEntity(id = 8L, status = EntityStatus.PAUSED)))

        val repository = DownloadRepositoryImpl(downloadDao)
        val result = repository.getByIdAsFlow(8L).first()

        assertEquals(sampleDownload(id = 8L, status = DownloadStatus.PAUSED), result)
    }

    @Test
    fun getByIdAsFlow_returnsNull_whenDaoEmitsNull() = runTest(testDispatcher) {
        `when`(downloadDao.getByIdAsFlow(8L))
            .thenReturn(flowOf(null))

        val repository = DownloadRepositoryImpl(downloadDao)
        assertNull(repository.getByIdAsFlow(8L).first())
    }

    @Test
    fun updateMethods_delegateToDao() = runTest(testDispatcher) {
        `when`(downloadDao.updateStatusById(3L, EntityStatus.STOPPED)).thenReturn(1)
        `when`(downloadDao.updateErrorMessageById(3L, "boom")).thenReturn(1)
        `when`(downloadDao.updateSeenById(3L, true)).thenReturn(1)
        `when`(downloadDao.updateStartedAtById(3L, 100L)).thenReturn(1)
        `when`(downloadDao.updateCompletedAtById(3L, 200L)).thenReturn(1)
        `when`(downloadDao.deleteById(3L)).thenReturn(1)

        val repository = DownloadRepositoryImpl(downloadDao)

        assertEquals(1, repository.updateStatusById(3L, DownloadStatus.STOPPED))
        assertEquals(1, repository.updateErrorMessageById(3L, "boom"))
        assertEquals(1, repository.updateSeenById(3L, true))
        assertEquals(1, repository.updateStartedAtById(3L, 100L))
        assertEquals(1, repository.updateCompletedAtById(3L, 200L))
        assertEquals(1, repository.deleteById(3L))

        verify(downloadDao).updateStatusById(3L, EntityStatus.STOPPED)
        verify(downloadDao).updateErrorMessageById(3L, "boom")
        verify(downloadDao).updateSeenById(3L, true)
        verify(downloadDao).updateStartedAtById(3L, 100L)
        verify(downloadDao).updateCompletedAtById(3L, 200L)
        verify(downloadDao).deleteById(3L)
    }

    private fun sampleDownload(
        id: Long = 9L,
        status: DownloadStatus = DownloadStatus.QUEUED,
        completedAtMillis: Long? = null,
    ) = Download(
        id = id,
        uid = "uid-$id",
        url = "https://example.com/$id",
        status = status,
        seen = false,
        errorMessage = "error-$id",
        completedAtMillis = completedAtMillis,
    )

    private fun sampleEntity(
        id: Long = 9L,
        status: EntityStatus = EntityStatus.QUEUED,
        completedAtMillis: Long? = null,
    ) = DownloadEntity(
        id = id,
        uid = "uid-$id",
        url = "https://example.com/$id",
        status = status,
        seen = false,
        errorMessage = "error-$id",
        enqueuedAtMillis = 10L,
        startedAtMillis = 20L,
        completedAtMillis = completedAtMillis,
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObject(): T = Mockito.any<T>() ?: null as T
}
