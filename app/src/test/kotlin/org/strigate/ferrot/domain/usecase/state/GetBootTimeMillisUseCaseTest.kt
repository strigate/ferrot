package org.strigate.ferrot.domain.usecase.state

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.domain.repository.StateRepository

class GetBootTimeMillisUseCaseTest {
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var repository: StateRepository

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun invoke_returnsRepositoryFlow() = runTest {
        `when`(repository.getBootTimeMillisAsFlow())
            .thenReturn(flowOf(42L))

        assertEquals(42L, GetBootTimeMillisUseCase(repository)().first())
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }
}
