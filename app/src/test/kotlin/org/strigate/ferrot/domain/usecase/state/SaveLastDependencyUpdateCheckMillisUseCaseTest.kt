package org.strigate.ferrot.domain.usecase.state

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.strigate.ferrot.domain.repository.StateRepository

class SaveLastDependencyUpdateCheckMillisUseCaseTest {
    @Test
    fun invoke_savesValue() = runTest {
        val repository = mock(StateRepository::class.java)
        SaveLastDependencyUpdateCheckMillisUseCase(repository)(44L)
        verify(repository).saveLastDependencyUpdateCheckMillis(44L)
    }
}
