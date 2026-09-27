package org.strigate.ferrot.domain.usecase.downloadaudio

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.strigate.ferrot.domain.model.DownloadAudio
import org.strigate.ferrot.domain.repository.DownloadAudioRepository

class SaveDownloadAudioUseCaseTest {
    @Test
    fun invoke_returnsWhetherRowWasInserted() = runTest {
        val repository = mock(DownloadAudioRepository::class.java)
        val audio = DownloadAudio(1L, "/audio.mp3", "mp3")
        `when`(repository.save(audio))
            .thenReturn(1L, 0L)

        assertTrue(SaveDownloadAudioUseCase(repository)(audio))
        assertFalse(SaveDownloadAudioUseCase(repository)(audio))
    }
}
