package org.strigate.ferrot.domain.usecase.cookieset

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.strigate.ferrot.domain.model.CookieSet
import org.strigate.ferrot.domain.model.CookieSetDomain
import org.strigate.ferrot.domain.model.CookieSetSource
import org.strigate.ferrot.domain.model.CookieSetWithDomains
import org.strigate.ferrot.domain.model.DownloadSwipeAction
import org.strigate.ferrot.domain.repository.CookieSetRepository
import org.strigate.ferrot.domain.repository.SettingsRepository
import org.strigate.ferrot.domain.usecase.settings.GetUseCookiesSettingAsFlowUseCase

class ResolveCookieSetForUrlUseCaseTest {
    @Test
    fun invoke_returnsSingleMatchingCookieSet_forSubdomainMatch() = runTest {
        val repository = FakeCookieSetRepository(
            cookieSets = listOf(
                cookieSet(id = 1L, domain = "x.com", includeSubdomains = true),
                cookieSet(id = 2L, domain = "instagram.com", includeSubdomains = true),
            )
        )

        val result = createUseCase(repository)("https://mobile.x.com/post/1")

        assertEquals(1L, result?.cookieSet?.id)
    }

    @Test
    fun invoke_returnsNull_whenCookiesDisabled() = runTest {
        val repository = FakeCookieSetRepository(
            cookieSets = listOf(cookieSet(id = 1L, domain = "x.com", includeSubdomains = true))
        )

        val result = createUseCase(repository, useCookies = false)("https://x.com/post/1")

        assertNull(result)
    }

    @Test
    fun invoke_returnsMostRecentlyUpdatedCookieSet_whenMultipleCookieSetsMatch() = runTest {
        val repository = FakeCookieSetRepository(
            cookieSets = listOf(
                cookieSet(
                    id = 1L,
                    domain = "x.com",
                    includeSubdomains = true,
                    updatedAtMillis = 100L,
                ),
                cookieSet(
                    id = 2L,
                    domain = "x.com",
                    includeSubdomains = true,
                    updatedAtMillis = 200L,
                ),
            )
        )

        val result = createUseCase(repository)("https://x.com/post/1")

        assertEquals(2L, result?.cookieSet?.id)
    }

    private fun createUseCase(
        repository: CookieSetRepository,
        useCookies: Boolean = true,
    ) = ResolveCookieSetForUrlUseCase(
        cookieSetRepository = repository,
        getUseCookiesSettingAsFlowUseCase = GetUseCookiesSettingAsFlowUseCase(
            settingsRepository = FakeSettingsRepository(useCookies),
        ),
    )

    private fun cookieSet(
        id: Long,
        domain: String,
        includeSubdomains: Boolean,
        updatedAtMillis: Long = 0L,
    ) = CookieSetWithDomains(
        cookieSet = CookieSet(
            id = id,
            name = "CookieSet $id",
            source = CookieSetSource.IMPORTED_FILE,
            cookieFilePath = "/tmp/$id.txt",
            updatedAtMillis = updatedAtMillis,
        ),
        domains = listOf(
            CookieSetDomain(
                cookieSetId = id,
                domain = domain,
                includeSubdomains = includeSubdomains,
            )
        ),
    )

    private class FakeCookieSetRepository(
        private val cookieSets: List<CookieSetWithDomains>,
    ) : CookieSetRepository {
        override suspend fun saveCookieSet(cookieSet: CookieSet): Long = error("unused")
        override suspend fun saveDomains(domains: List<CookieSetDomain>) = error("unused")
        override fun getAllWithDomainsAsFlow(): Flow<List<CookieSetWithDomains>> =
            flowOf(cookieSets)

        override suspend fun getAllWithDomains(): List<CookieSetWithDomains> = cookieSets

        override suspend fun getCookieSetIdsByDomains(domains: Collection<String>): List<Long> =
            error("unused")

        override suspend fun getByIdWithDomains(id: Long): CookieSetWithDomains? = cookieSets
            .firstOrNull { it.cookieSet.id == id }

        override suspend fun updateCookieFilePath(id: Long, cookieFilePath: String): Int =
            error("unused")

        override suspend fun updateLastUsedAt(id: Long, lastUsedAtMillis: Long): Int =
            error("unused")

        override suspend fun deleteCookieSetById(id: Long): Int = error("unused")
    }

    private class FakeSettingsRepository(
        private val useCookies: Boolean,
    ) : SettingsRepository {
        override suspend fun saveDownloadWifiOnly(enabled: Boolean) = error("unused")
        override fun getDownloadWifiOnlyAsFlow(): Flow<Boolean> = error("unused")
        override suspend fun saveAutomaticDuplicateDownloadDeletion(enabled: Boolean) =
            error("unused")

        override fun getAutomaticDuplicateDownloadDeletionAsFlow(): Flow<Boolean> = error("unused")
        override suspend fun saveLeftSwipeAction(action: DownloadSwipeAction) = error("unused")
        override fun getLeftSwipeActionAsFlow(): Flow<DownloadSwipeAction> = error("unused")
        override suspend fun saveRightSwipeAction(action: DownloadSwipeAction) = error("unused")
        override fun getRightSwipeActionAsFlow(): Flow<DownloadSwipeAction> = error("unused")
        override suspend fun saveAutomaticUpdates(enabled: Boolean) = error("unused")
        override fun getAutomaticUpdatesAsFlow(): Flow<Boolean> = error("unused")
        override suspend fun saveAutomaticDependencyUpdates(enabled: Boolean) = error("unused")
        override fun getAutomaticDependencyUpdatesAsFlow(): Flow<Boolean> = error("unused")
        override suspend fun saveUseCookies(enabled: Boolean) = error("unused")
        override fun getUseCookiesAsFlow(): Flow<Boolean> = flowOf(useCookies)
    }
}
