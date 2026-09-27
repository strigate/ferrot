package org.strigate.ferrot.domain.usecase.downloadvideo

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.strigate.ferrot.domain.model.DownloadVideo
import org.strigate.ferrot.domain.repository.DownloadVideoRepository

class SaveDownloadVideoUseCaseTest {
    @Test
    fun invoke_returnsWhetherRowWasInserted() = runTest {
        val repository = mock(DownloadVideoRepository::class.java)
        val video = DownloadVideo(1L, "/video.mp4", "mp4", "sha")
        `when`(repository.save(video))
            .thenReturn(1L, 0L)

        assertTrue(SaveDownloadVideoUseCase(repository)(video))
        assertFalse(SaveDownloadVideoUseCase(repository)(video))
    }
}
