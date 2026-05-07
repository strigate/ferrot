package org.strigate.ferrot.domain.usecase.combined

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.test.MainDispatcherRule
import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.download.GetAllDownloadsUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class GetPendingDownloadsCombinedUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val testDispatcher: TestDispatcher = mainDispatcherRule.testDispatcher
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var downloadUseCase: DownloadUseCase

    @Mock
    private lateinit var getAllDownloadsUseCase: GetAllDownloadsUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        `when`(downloadUseCase.getAllDownloadsUseCase)
            .thenReturn(getAllDownloadsUseCase)
    }

    @Test
    fun invoke_returnsOnlyRequeueableDownloads() = runTest(testDispatcher) {
        val downloads = listOf(
            sampleDownload(1L, DownloadStatus.QUEUED),
            sampleDownload(2L, DownloadStatus.WAITING_FOR_NETWORK),
            sampleDownload(3L, DownloadStatus.WAITING_FOR_WIFI),
            sampleDownload(4L, DownloadStatus.PAUSED),
            sampleDownload(5L, DownloadStatus.METADATA),
            sampleDownload(6L, DownloadStatus.DOWNLOADING),
            sampleDownload(7L, DownloadStatus.COMPLETED),
            sampleDownload(8L, DownloadStatus.FAILED),
            sampleDownload(9L, DownloadStatus.STOPPED),
        )
        `when`(getAllDownloadsUseCase.invoke())
            .thenReturn(downloads)

        val result = createUseCase().invoke()

        assertEquals(
            listOf(
                sampleDownload(1L, DownloadStatus.QUEUED),
                sampleDownload(2L, DownloadStatus.WAITING_FOR_NETWORK),
                sampleDownload(3L, DownloadStatus.WAITING_FOR_WIFI),
                sampleDownload(4L, DownloadStatus.PAUSED),
            ),
            result,
        )
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private fun createUseCase() = GetPendingDownloadsCombinedUseCase(
        downloadUseCase = downloadUseCase,
    )

    private fun sampleDownload(id: Long, status: DownloadStatus) = Download(
        id = id,
        uid = "uid-$id",
        url = "https://example.com/$id",
        status = status,
        seen = false,
    )
}
