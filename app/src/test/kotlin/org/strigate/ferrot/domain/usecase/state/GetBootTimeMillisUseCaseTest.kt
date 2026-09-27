package org.strigate.ferrot.domain.usecase.state

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.strigate.ferrot.domain.repository.StateRepository

class GetBootTimeMillisUseCaseTest {
    @Test
    fun invoke_returnsRepositoryFlow() = runTest {
        val repository = mock(StateRepository::class.java)
        `when`(repository.getBootTimeMillisAsFlow())
            .thenReturn(flowOf(42L))

        assertEquals(42L, GetBootTimeMillisUseCase(repository)().first())
    }
}
