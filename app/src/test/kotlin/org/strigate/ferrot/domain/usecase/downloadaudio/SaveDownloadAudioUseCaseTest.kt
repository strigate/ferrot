package org.strigate.ferrot.domain.usecase.downloadaudio

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.domain.model.DownloadAudio
import org.strigate.ferrot.domain.repository.DownloadAudioRepository

class SaveDownloadAudioUseCaseTest {
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var repository: DownloadAudioRepository

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun invoke_returnsWhetherRowWasInserted() = runTest {
        val audio = DownloadAudio(1L, "/audio.mp3", "mp3")
        `when`(repository.save(audio))
            .thenReturn(1L, 0L)

        assertTrue(SaveDownloadAudioUseCase(repository)(audio))
        assertFalse(SaveDownloadAudioUseCase(repository)(audio))
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }
}
