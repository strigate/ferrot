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

class DeleteCookieSetsWithMissingFilesUseCaseTest {
    @Test
    fun invoke_deletesCookieSetsWhoseCookieFileIsMissingOrEmpty() = runTest {
        val rootDir = Files.createTempDirectory("cookie-cleanup").toFile()
        val pathProvider = TempCookieSetPathProvider(rootDir)
        val cookieFileStore = CookieFileStore(pathProvider)
        val cookieSetRepository = SavingCookieSetRepository()
        val downloadRepository = RecordingDownloadRepository()
        val useCase = DeleteCookieSetsWithMissingFilesUseCase(
            cookieSetRepository = cookieSetRepository,
            cookieFileStore = cookieFileStore,
            deleteCookieSetUseCase = DeleteCookieSetUseCase(
                cookieSetRepository = cookieSetRepository,
                downloadRepository = downloadRepository,
                cookieFileStore = cookieFileStore,
            ),
        )

        try {
            cookieSetRepository.seedCookieSet(cookieSet(id = 1L, domain = "x.com"))
            cookieFileStore.writeCookies(
                cookieSetId = 1L,
                content = "# Netscape HTTP Cookie File\n.x.com\tTRUE\t/\tTRUE\t0\tauth\tone",
            )
            cookieSetRepository.seedCookieSet(cookieSet(id = 2L, domain = "instagram.com"))
            cookieSetRepository.seedCookieSet(cookieSet(id = 3L, domain = "twitter.com"))
            pathProvider.cookieFile(3L).apply {
                parentFile?.mkdirs()
                writeText("")
            }

            useCase()

            assertTrue(cookieSetRepository.containsCookieSet(1L))
            assertFalse(cookieSetRepository.containsCookieSet(2L))
            assertFalse(cookieSetRepository.containsCookieSet(3L))
            assertEquals(listOf(2L, 3L), downloadRepository.clearedCookieSetIds)
        } finally {
            rootDir.deleteRecursively()
        }
    }

    private fun cookieSet(id: Long, domain: String): CookieSetWithDomains {
        return CookieSetWithDomains(
            cookieSet = CookieSet(
                id = id,
                name = domain,
                source = CookieSetSource.WEBVIEW,
                cookieFilePath = "",
            ),
            domains = listOf(
                CookieSetDomain(
                    cookieSetId = id,
                    domain = domain,
                    includeSubdomains = true,
                )
            ),
        )
    }

    private class SavingCookieSetRepository : CookieSetRepository {
        private val cookieSets = mutableMapOf<Long, CookieSet>()
        private val cookieSetDomains = mutableMapOf<Long, List<CookieSetDomain>>()

        override suspend fun saveCookieSet(cookieSet: CookieSet): Long = error("unused")

        override suspend fun saveDomains(domains: List<CookieSetDomain>) = error("unused")

        override fun getAllWithDomainsAsFlow(): Flow<List<CookieSetWithDomains>> {
            return flowOf(getSavedCookieSets())
        }

        override suspend fun getAllWithDomains(): List<CookieSetWithDomains> {
            return getSavedCookieSets()
        }

        override suspend fun getCookieSetIdsByDomains(domains: Collection<String>): List<Long> =
            error("unused")

        override suspend fun getByIdWithDomains(id: Long): CookieSetWithDomains? {
            val cookieSet = cookieSets[id] ?: return null
            return CookieSetWithDomains(
                cookieSet = cookieSet,
                domains = cookieSetDomains[id].orEmpty(),
            )
        }

        override suspend fun updateCookieFilePath(id: Long, cookieFilePath: String): Int =
            error("unused")

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
        ): Int {
            error("unused")
        }

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
