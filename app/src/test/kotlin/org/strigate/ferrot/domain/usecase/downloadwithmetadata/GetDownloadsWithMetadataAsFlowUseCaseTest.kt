package org.strigate.ferrot.domain.usecase.downloadwithmetadata

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.model.DownloadWithMetadata
import org.strigate.ferrot.domain.repository.DownloadWithMetadataRepository

class GetDownloadsWithMetadataAsFlowUseCaseTest {
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var downloadWithMetadataRepository: DownloadWithMetadataRepository

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun invoke_returnsDownloadsFlow_whenArchivedIsFalse() = runTest {
        val expectedDownloads = listOf(sampleDownloadWithMetadata(1L, archived = false))
        val expectedFlow = flowOf(expectedDownloads)
        `when`(downloadWithMetadataRepository.getDownloadsWithMetadataAsFlow())
            .thenReturn(expectedFlow)

        val result = createUseCase().invoke().first()

        assertEquals(expectedDownloads, result)
        assertEquals(
            listOf("getDownloadsWithMetadataAsFlow"),
            recordedInvocationNames(),
        )
    }

    @Test
    fun invoke_returnsArchivedDownloadsFlow_whenArchivedIsTrue() = runTest {
        val expectedDownloads = listOf(sampleDownloadWithMetadata(2L, archived = true))
        val expectedFlow = flowOf(expectedDownloads)
        `when`(downloadWithMetadataRepository.getArchivedDownloadsWithMetadataAsFlow())
            .thenReturn(expectedFlow)

        val result = createUseCase().invoke(archived = true).first()

        assertEquals(expectedDownloads, result)
        assertEquals(
            listOf("getArchivedDownloadsWithMetadataAsFlow"),
            recordedInvocationNames(),
        )
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private fun createUseCase() = GetDownloadsWithMetadataAsFlowUseCase(
        downloadWithMetadataRepository = downloadWithMetadataRepository,
    )

    private fun recordedInvocationNames(): List<String> {
        return mockingDetails(downloadWithMetadataRepository)
            .invocations
            .map { it.method.name }
    }

    private fun sampleDownloadWithMetadata(
        id: Long,
        archived: Boolean,
    ) = DownloadWithMetadata(
        id = id,
        url = "https://example.com/$id",
        title = "Title $id",
        thumbnailFilePath = null,
        status = DownloadStatus.QUEUED,
        seen = false,
        archived = archived,
        progressPercent = 0f,
        etaSeconds = null,
        bytesDownloaded = 0L,
        expectedBytes = null,
        completedAtMillis = null,
    )
}
