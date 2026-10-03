package org.strigate.ferrot.domain.usecase.download

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.domain.repository.DownloadRepository

class UpdateDownloadCompletedAtUseCaseTest {
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var repository: DownloadRepository

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun invoke_delegatesNullableTimestamp() = runTest {
        UpdateDownloadCompletedAtUseCase(repository)(3L, null)
        verify(repository)
            .updateCompletedAtById(3L, null)
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }
}
