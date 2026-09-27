package org.strigate.ferrot.domain.usecase.cookieset

import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.strigate.ferrot.domain.model.CookieSetWithDomains
import org.strigate.ferrot.domain.repository.CookieSetRepository

class GetCookieSetsWithDomainsAsFlowUseCaseTest {
    @Test
    fun invoke_returnsRepositoryFlow() {
        val repository = mock(CookieSetRepository::class.java)
        val flow = flowOf(emptyList<CookieSetWithDomains>())
        `when`(repository.getAllWithDomainsAsFlow())
            .thenReturn(flow)

        assertSame(flow, GetCookieSetsWithDomainsAsFlowUseCase(repository)())
    }
}
