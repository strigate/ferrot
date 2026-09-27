package org.strigate.ferrot.domain.usecase.cookieset

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.strigate.ferrot.domain.repository.CookieSetRepository

class UpdateCookieSetLastUsedAtUseCaseTest {
    @Test
    fun invoke_updatesSpecifiedTimestamp() = runTest {
        val repository = mock(CookieSetRepository::class.java)
        UpdateCookieSetLastUsedAtUseCase(repository)(4L, 99L)
        verify(repository).updateLastUsedAt(4L, 99L)
    }
}
