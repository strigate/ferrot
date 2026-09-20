package org.strigate.ferrot.domain.usecase.state

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.strigate.ferrot.domain.repository.StateRepository

class SaveBootTimeMillisUseCaseTest {
    @Test
    fun invoke_savesValue() = runTest {
        val repository = mock(StateRepository::class.java)
        SaveBootTimeMillisUseCase(repository)(43L)
        verify(repository).saveBootTimeMillis(43L)
    }
}
