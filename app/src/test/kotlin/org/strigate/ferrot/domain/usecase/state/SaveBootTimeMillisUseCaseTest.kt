package org.strigate.ferrot.domain.usecase.state

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.domain.repository.StateRepository

class SaveBootTimeMillisUseCaseTest {
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var repository: StateRepository

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun invoke_savesValue() = runTest {
        SaveBootTimeMillisUseCase(repository)(43L)
        verify(repository)
            .saveBootTimeMillis(43L)
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }
}
