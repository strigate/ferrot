package org.strigate.ferrot.domain.usecase.downloadvideo

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.domain.repository.DownloadVideoRepository

@OptIn(ExperimentalCoroutinesApi::class)
class GetDownloadIdsBySha256UseCaseTest {
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var downloadVideoRepository: DownloadVideoRepository

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun invoke_returnsEmptyList_whenSha256IsBlank() = runTest {
        val result = createUseCase().invoke("   ")

        assertEquals(emptyList<Long>(), result)
        verify(downloadVideoRepository, never())
            .getDownloadIdsBySha256(org.mockito.ArgumentMatchers.anyString())
    }

    @Test
    fun invoke_returnsRepositoryResult_whenSha256IsValid() = runTest {
        val sha256 = "abc123"
        val expected = listOf(1L, 2L, 3L)
        `when`(downloadVideoRepository.getDownloadIdsBySha256(sha256))
            .thenReturn(expected)

        val result = createUseCase().invoke(sha256)

        assertEquals(expected, result)
        verify(downloadVideoRepository)
            .getDownloadIdsBySha256(sha256)
    }

    @Test
    fun invoke_returnsEmptyList_whenRepositoryThrows() = runTest {
        val sha256 = "def456"
        `when`(downloadVideoRepository.getDownloadIdsBySha256(sha256))
            .thenThrow(RuntimeException("boom"))

        val result = createUseCase().invoke(sha256)

        assertEquals(emptyList<Long>(), result)
        verify(downloadVideoRepository)
            .getDownloadIdsBySha256(sha256)
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private fun createUseCase() = GetDownloadIdsBySha256UseCase(
        downloadVideoRepository = downloadVideoRepository,
    )
}
