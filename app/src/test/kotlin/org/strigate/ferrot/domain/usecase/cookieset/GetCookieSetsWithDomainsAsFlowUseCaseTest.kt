package org.strigate.ferrot.domain.usecase.cookieset

import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.domain.model.CookieSetWithDomains
import org.strigate.ferrot.domain.repository.CookieSetRepository

class GetCookieSetsWithDomainsAsFlowUseCaseTest {
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var repository: CookieSetRepository

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun invoke_returnsRepositoryFlow() {
        val flow = flowOf(emptyList<CookieSetWithDomains>())
        `when`(repository.getAllWithDomainsAsFlow())
            .thenReturn(flow)

        assertSame(flow, GetCookieSetsWithDomainsAsFlowUseCase(repository)())
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }
}
