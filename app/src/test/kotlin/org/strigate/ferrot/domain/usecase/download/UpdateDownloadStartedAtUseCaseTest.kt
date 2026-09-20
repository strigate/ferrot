package org.strigate.ferrot.domain.usecase.download

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.strigate.ferrot.domain.repository.DownloadRepository

class UpdateDownloadStartedAtUseCaseTest {
    @Test
    fun invoke_delegatesTimestamp() = runTest {
        val repository = mock(DownloadRepository::class.java)
        UpdateDownloadStartedAtUseCase(repository)(3L, 10L)
        verify(repository).updateStartedAtById(3L, 10L)
    }
}
