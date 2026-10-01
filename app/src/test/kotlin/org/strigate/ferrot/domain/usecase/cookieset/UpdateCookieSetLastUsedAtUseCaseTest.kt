package org.strigate.ferrot.domain.usecase.cookieset

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.domain.repository.CookieSetRepository

class UpdateCookieSetLastUsedAtUseCaseTest {
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var repository: CookieSetRepository

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun invoke_updatesSpecifiedTimestamp() = runTest {
        UpdateCookieSetLastUsedAtUseCase(repository)(4L, 99L)
        verify(repository)
            .updateLastUsedAt(4L, 99L)
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }
}
