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
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.data.local.dao.DownloadWithMetadataViewDao
import org.strigate.ferrot.data.local.view.DownloadWithMetadataView
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.model.DownloadWithMetadata
import org.strigate.ferrot.data.local.entity.DownloadStatus as EntityStatus

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadWithMetadataRepositoryImplTest {
    private lateinit var autoCloseable: AutoCloseable
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var downloadWithMetadataViewDao: DownloadWithMetadataViewDao

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }


    @Test
    fun getDownloadsWithMetadataAsFlow_mapsViewsToDomain() = runTest(testDispatcher) {
        `when`(downloadWithMetadataViewDao.getDownloadsAsFlow())
            .thenReturn(
                flowOf(
                    listOf(
                        sampleView(id = 1L, status = EntityStatus.DOWNLOADING),
                        sampleView(
                            id = 2L,
                            status = EntityStatus.COMPLETED,
                            completedAtMillis = 88L,
                            pendingDelete = true,
                        ),
                    ),
                ),
            )

        val repository = DownloadWithMetadataRepositoryImpl(downloadWithMetadataViewDao)
        val result = repository.getDownloadsWithMetadataAsFlow().first()
        assertEquals(
            listOf(
                sampleDomain(id = 1L, status = DownloadStatus.DOWNLOADING),
                sampleDomain(
                    id = 2L,
                    status = DownloadStatus.COMPLETED,
                    completedAtMillis = 88L,
                    pendingDelete = true,
                ),
            ),
            result,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        autoCloseable.close()
    }

    private fun sampleView(
        id: Long,
        status: EntityStatus,
        completedAtMillis: Long? = null,
        pendingDelete: Boolean = false,
    ) = DownloadWithMetadataView(
        id = id,
        url = "https://example.com/$id",
        resolvedTitle = "Title $id",
        thumbnailFilePath = "/tmp/$id.jpg",
        status = status,
        seen = id % 2L == 0L,
        pendingDelete = pendingDelete,
        archived = false,
        progressPercent = 35F,
        etaSeconds = 12L,
        bytesDownloaded = 1024L,
        expectedBytes = 4096L,
        enqueuedAtMillis = 10L,
        startedAtMillis = 20L,
        completedAtMillis = completedAtMillis,
    )

    private fun sampleDomain(
        id: Long,
        status: DownloadStatus,
        completedAtMillis: Long? = null,
        pendingDelete: Boolean = false,
    ) = DownloadWithMetadata(
        id = id,
        url = "https://example.com/$id",
        title = "Title $id",
        thumbnailFilePath = "/tmp/$id.jpg",
        status = status,
        seen = id % 2L == 0L,
        pendingDelete = pendingDelete,
        archived = false,
        progressPercent = 35F,
        etaSeconds = 12L,
        bytesDownloaded = 1024L,
        expectedBytes = 4096L,
        completedAtMillis = completedAtMillis,
    )
}
