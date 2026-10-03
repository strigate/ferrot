package org.strigate.ferrot.domain.usecase.downloadprogress

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.domain.repository.DownloadProgressRepository

class UpdateDownloadExpectedBytesUseCaseTest {
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var repository: DownloadProgressRepository

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun invoke_returnsWhetherRowWasUpdated() = runTest {
        `when`(repository.updateExpectedBytes(1L, 500L))
            .thenReturn(1)
        `when`(repository.updateExpectedBytes(2L, 500L))
            .thenReturn(0)

        assertTrue(UpdateDownloadExpectedBytesUseCase(repository)(1L, 500L))
        assertFalse(UpdateDownloadExpectedBytesUseCase(repository)(2L, 500L))
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }
}
