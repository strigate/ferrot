package org.strigate.ferrot.domain.usecase.downloadvideo

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.domain.model.DownloadVideo
import org.strigate.ferrot.domain.repository.DownloadVideoRepository

class SaveDownloadVideoUseCaseTest {
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var repository: DownloadVideoRepository

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun invoke_returnsWhetherRowWasInserted() = runTest {
        val video = DownloadVideo(1L, "/video.mp4", "mp4", "sha")
        `when`(repository.save(video))
            .thenReturn(1L, 0L)

        assertTrue(SaveDownloadVideoUseCase(repository)(video))
        assertFalse(SaveDownloadVideoUseCase(repository)(video))
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }
}
