package org.strigate.ferrot.domain.usecase.downloadprogress

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.strigate.ferrot.domain.repository.DownloadProgressRepository

class UpdateDownloadExpectedBytesUseCaseTest {
    @Test
    fun invoke_returnsWhetherRowWasUpdated() = runTest {
        val repository = mock(DownloadProgressRepository::class.java)
        `when`(repository.updateExpectedBytes(1L, 500L))
            .thenReturn(1)
        `when`(repository.updateExpectedBytes(2L, 500L))
            .thenReturn(0)

        assertTrue(UpdateDownloadExpectedBytesUseCase(repository)(1L, 500L))
        assertFalse(UpdateDownloadExpectedBytesUseCase(repository)(2L, 500L))
    }
}
