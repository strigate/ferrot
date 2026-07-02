package org.strigate.ferrot.domain.usecase.cookieset

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.strigate.ferrot.cookies.CookieSetDomainParser
import org.strigate.ferrot.cookies.WebViewCookieDomainResolver
import org.strigate.ferrot.domain.model.CookieSet
import org.strigate.ferrot.domain.model.CookieSetDomain
import org.strigate.ferrot.domain.model.CookieSetWithDomains
import org.strigate.ferrot.domain.repository.CookieSetRepository

class GetExistingCookieSetDomainForWebViewUrlUseCaseTest {
    @Test
    fun invoke_returnsStrippedDomainWhenCookieSetExists() = runTest {
        val repository = DomainMatchingCookieSetRepository(existingDomains = setOf("x.com"))
        val useCase = GetExistingCookieSetDomainForWebViewUrlUseCase(
            cookieSetRepository = repository,
            webViewCookieDomainResolver = WebViewCookieDomainResolver(CookieSetDomainParser()),
        )

        val result = useCase("https://mobile.x.com/home")

        assertEquals("x.com", result)
    }

    @Test
    fun invoke_returnsNullWhenNoCookieSetExists() = runTest {
        val repository =
            DomainMatchingCookieSetRepository(existingDomains = setOf("example.com"))
        val useCase = GetExistingCookieSetDomainForWebViewUrlUseCase(
            cookieSetRepository = repository,
            webViewCookieDomainResolver = WebViewCookieDomainResolver(CookieSetDomainParser()),
        )

        val result = useCase("https://x.com/home")

        assertNull(result)
    }

    private class DomainMatchingCookieSetRepository(
        private val existingDomains: Set<String>,
    ) : CookieSetRepository {
        override suspend fun saveCookieSet(cookieSet: CookieSet): Long = error("unused")

        override suspend fun saveDomains(domains: List<CookieSetDomain>) = error("unused")

        override fun getAllWithDomainsAsFlow(): Flow<List<CookieSetWithDomains>> {
            return flowOf(emptyList())
        }

        override suspend fun getAllWithDomains(): List<CookieSetWithDomains> = error("unused")

        override suspend fun getCookieSetIdsByDomains(domains: Collection<String>): List<Long> {
            return if (domains.any { it in existingDomains }) listOf(1L) else emptyList()
        }

        override suspend fun getByIdWithDomains(id: Long): CookieSetWithDomains? =
            error("unused")

        override suspend fun updateCookieFilePath(id: Long, cookieFilePath: String): Int =
            error("unused")

        override suspend fun updateLastUsedAt(id: Long, lastUsedAtMillis: Long): Int =
            error("unused")

        override suspend fun deleteCookieSetById(id: Long): Int = error("unused")
    }
}
