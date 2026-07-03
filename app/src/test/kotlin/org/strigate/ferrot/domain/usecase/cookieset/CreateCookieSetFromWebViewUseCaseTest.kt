package org.strigate.ferrot.domain.usecase.cookieset

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.strigate.ferrot.app.integration.CookieFileStore
import org.strigate.ferrot.app.provider.CookieSetPathProvider
import org.strigate.ferrot.cookies.CookieHeaderFileBuilder
import org.strigate.ferrot.cookies.CookieSetDomainParser
import org.strigate.ferrot.cookies.WebViewCookieDomainResolver
import org.strigate.ferrot.domain.model.CookieSet
import org.strigate.ferrot.domain.model.CookieSetDomain
import org.strigate.ferrot.domain.model.CookieSetSource
import org.strigate.ferrot.domain.model.CookieSetWithDomains
import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.repository.CookieSetRepository
import org.strigate.ferrot.domain.repository.DownloadRepository
import java.io.File
import java.nio.file.Files

class CreateCookieSetFromWebViewUseCaseTest {
    @Test
    fun invoke_savesWebViewCookiesForCurrentDomain() = runTest {
        val rootDir = Files.createTempDirectory("cookie-webview").toFile()
        val repository = SavingCookieSetRepository()
        val downloadRepository = RecordingDownloadRepository()
        val cookieFileStore = CookieFileStore(TempCookieSetPathProvider(rootDir))
        val useCase = CreateCookieSetFromWebViewUseCase(
            cookieSetRepository = repository,
            webViewCookieDomainResolver = WebViewCookieDomainResolver(CookieSetDomainParser()),
            cookieHeaderFileBuilder = CookieHeaderFileBuilder(),
            cookieFileStore = cookieFileStore,
            deleteCookieSetUseCase = DeleteCookieSetUseCase(
                cookieSetRepository = repository,
                downloadRepository = downloadRepository,
                cookieFileStore = cookieFileStore,
            ),
        )

        try {
            val result = useCase(
                url = "https://mobile.x.com/home",
                rawCookieHeader = "auth=one; ct0=two",
            )

            assertEquals("x.com", result.cookieSet.name)
            assertEquals(CookieSetSource.WEBVIEW, result.cookieSet.source)
            assertEquals("x.com", result.domains.single().domain)
            assertEquals(true, result.domains.single().includeSubdomains)
            assertTrue(
                File(result.cookieSet.cookieFilePath).readText()
                    .contains(".x.com\tTRUE\t/\tTRUE\t\tauth\tone")
            )
        } finally {
            rootDir.deleteRecursively()
        }
    }

    @Test
    fun invoke_deletesExistingCookieSetForSameDomain() = runTest {
        val rootDir = Files.createTempDirectory("cookie-webview").toFile()
        val repository = SavingCookieSetRepository()
        val downloadRepository = RecordingDownloadRepository()
        repository.seedCookieSet(
            CookieSetWithDomains(
                cookieSet = CookieSet(
                    id = 99L,
                    name = "Old x.com",
                    source = CookieSetSource.IMPORTED_FILE,
                    cookieFilePath = File(rootDir, "99/cookies.txt").absolutePath,
                ),
                domains = listOf(
                    CookieSetDomain(
                        cookieSetId = 99L,
                        domain = "x.com",
                        includeSubdomains = true,
                    )
                ),
            )
        )
        val cookieFileStore = CookieFileStore(TempCookieSetPathProvider(rootDir))
        val useCase = CreateCookieSetFromWebViewUseCase(
            cookieSetRepository = repository,
            webViewCookieDomainResolver = WebViewCookieDomainResolver(CookieSetDomainParser()),
            cookieHeaderFileBuilder = CookieHeaderFileBuilder(),
            cookieFileStore = cookieFileStore,
            deleteCookieSetUseCase = DeleteCookieSetUseCase(
                cookieSetRepository = repository,
                downloadRepository = downloadRepository,
                cookieFileStore = cookieFileStore,
            ),
        )

        try {
            val result = useCase(
                url = "https://x.com/home",
                rawCookieHeader = "auth=new",
            )

            assertFalse(repository.containsCookieSet(99L))
            assertEquals(listOf(99L), downloadRepository.clearedCookieSetIds)
            assertEquals(
                result.cookieSet.id,
                repository.getCookieSetIdsByDomains(listOf("x.com")).single()
            )
        } finally {
            rootDir.deleteRecursively()
        }
    }

    private class RecordingDownloadRepository : DownloadRepository {
        val clearedCookieSetIds = mutableListOf<Long>()

        override suspend fun save(download: Download): Long = error("unused")

        override suspend fun getAll(): List<Download> = error("unused")

        override suspend fun getById(id: Long): Download? = error("unused")

        override fun getByIdAsFlow(id: Long): Flow<Download?> = error("unused")

        override suspend fun updateStatusById(id: Long, status: DownloadStatus): Int =
            error("unused")

        override suspend fun updateErrorMessageById(id: Long, errorMessage: String?): Int =
            error("unused")

        override suspend fun updateSeenByIds(ids: Collection<Long>, seen: Boolean): Int =
            error("unused")

        override suspend fun updatePendingDeleteByIds(
            ids: Collection<Long>,
            pendingDelete: Boolean
        ): Int =
            error("unused")

        override suspend fun updateArchivedByIds(ids: Collection<Long>, archived: Boolean): Int =
            error("unused")

        override suspend fun updateStartedAtById(id: Long, startedAtMillis: Long?): Int =
            error("unused")

        override suspend fun updateCompletedAtById(id: Long, completedAtMillis: Long?): Int =
            error("unused")

        override suspend fun updateCookieSetIdById(id: Long, cookieSetId: Long?): Int =
            error("unused")

        override suspend fun clearCookieSetId(cookieSetId: Long): Int {
            clearedCookieSetIds += cookieSetId
            return 1
        }

        override suspend fun deleteById(id: Long): Int = error("unused")
    }

    private class SavingCookieSetRepository : CookieSetRepository {
        private val cookieSets = mutableMapOf<Long, CookieSet>()
        private val cookieSetDomains = mutableMapOf<Long, List<CookieSetDomain>>()
        private var nextId = 1L

        override suspend fun saveCookieSet(cookieSet: CookieSet): Long {
            val id = nextId++
            cookieSets[id] = cookieSet.copy(id = id)
            return id
        }

        override suspend fun saveDomains(domains: List<CookieSetDomain>) {
            domains.groupBy { it.cookieSetId }.forEach { (cookieSetId, cookieSetDomains) ->
                this.cookieSetDomains[cookieSetId] = cookieSetDomains
            }
        }

        override fun getAllWithDomainsAsFlow(): Flow<List<CookieSetWithDomains>> {
            return flowOf(getSavedCookieSets())
        }

        override suspend fun getAllWithDomains(): List<CookieSetWithDomains> {
            return getSavedCookieSets()
        }

        override suspend fun getCookieSetIdsByDomains(domains: Collection<String>): List<Long> {
            return cookieSetDomains
                .filterValues { savedDomains -> savedDomains.any { it.domain in domains } }
                .keys
                .toList()
        }

        override suspend fun getByIdWithDomains(id: Long): CookieSetWithDomains? {
            val cookieSet = cookieSets[id] ?: return null
            return CookieSetWithDomains(
                cookieSet = cookieSet,
                domains = cookieSetDomains[id].orEmpty(),
            )
        }

        override suspend fun updateCookieFilePath(id: Long, cookieFilePath: String): Int {
            cookieSets[id] = cookieSets.getValue(id).copy(cookieFilePath = cookieFilePath)
            return 1
        }

        override suspend fun updateLastUsedAt(id: Long, lastUsedAtMillis: Long): Int =
            error("unused")

        override suspend fun deleteCookieSetById(id: Long): Int {
            cookieSets.remove(id)
            cookieSetDomains.remove(id)
            return 1
        }

        fun seedCookieSet(cookieSet: CookieSetWithDomains) {
            cookieSets[cookieSet.cookieSet.id] = cookieSet.cookieSet
            cookieSetDomains[cookieSet.cookieSet.id] = cookieSet.domains
            nextId = maxOf(nextId, cookieSet.cookieSet.id + 1L)
        }

        fun containsCookieSet(cookieSetId: Long): Boolean = cookieSets.containsKey(cookieSetId)

        private fun getSavedCookieSets(): List<CookieSetWithDomains> {
            return cookieSets.values.map { cookieSet ->
                CookieSetWithDomains(
                    cookieSet = cookieSet,
                    domains = cookieSetDomains[cookieSet.id].orEmpty(),
                )
            }
        }
    }

    private class TempCookieSetPathProvider(
        private val rootDir: File,
    ) : CookieSetPathProvider {
        override fun cookiesDir(): File = rootDir.apply { mkdirs() }

        override fun cookieSetDir(cookieSetId: Long): File {
            return File(cookiesDir(), cookieSetId.toString()).apply { mkdirs() }
        }

        override fun cookieFile(cookieSetId: Long): File {
            return File(cookieSetDir(cookieSetId), "cookies.txt")
        }

        override fun tempDir(): File {
            return File(cookiesDir(), "temp").apply { mkdirs() }
        }

        override fun tempCookieFile(cookieSetId: Long, fileName: String): File {
            return File(tempDir(), "${cookieSetId}_$fileName")
        }
    }
}
