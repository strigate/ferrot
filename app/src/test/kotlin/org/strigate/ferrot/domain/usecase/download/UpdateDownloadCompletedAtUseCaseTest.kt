package org.strigate.ferrot.domain.usecase.download

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.strigate.ferrot.domain.repository.DownloadRepository

class UpdateDownloadCompletedAtUseCaseTest {
    @Test
    fun invoke_delegatesNullableTimestamp() = runTest {
        val repository = mock(DownloadRepository::class.java)
        UpdateDownloadCompletedAtUseCase(repository)(3L, null)
        verify(repository).updateCompletedAtById(3L, null)
    }
}
