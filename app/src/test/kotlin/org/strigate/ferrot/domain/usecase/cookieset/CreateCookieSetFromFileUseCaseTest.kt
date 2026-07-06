package org.strigate.ferrot.domain.usecase.cookieset

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.strigate.ferrot.app.integration.CookieFileStore
import org.strigate.ferrot.app.provider.CookieSetPathProvider
import org.strigate.ferrot.cookies.CookieSetDomainParser
import org.strigate.ferrot.domain.model.CookieSet
import org.strigate.ferrot.domain.model.CookieSetDomain
import org.strigate.ferrot.domain.model.CookieSetWithDomains
import org.strigate.ferrot.domain.repository.CookieSetRepository
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files

class CreateCookieSetFromFileUseCaseTest {
    @Test
    fun invoke_returnsNullAndDeletesCookieSet_whenFileCannotBeOpened() = runTest {
        val rootDir = Files.createTempDirectory("cookie-file-import").toFile()
        val cacheDir = File(rootDir, "cache").apply { mkdirs() }
        val repository = SavingCookieSetRepository()
        val pathProvider = TempCookieSetPathProvider(rootDir)
        val cookieFileStore = CookieFileStore(pathProvider)
        val uri = mock(Uri::class.java)
        val contentResolver = mock(ContentResolver::class.java)
        val appContext = mock(Context::class.java)
        `when`(appContext.cacheDir).thenReturn(cacheDir)
        `when`(appContext.contentResolver).thenReturn(contentResolver)
        `when`(contentResolver.openInputStream(uri)).thenReturn(null)
        val useCase = createUseCase(
            appContext = appContext,
            repository = repository,
            cookieFileStore = cookieFileStore,
        )

        try {
            val result = useCase(
                name = "",
                uri = uri,
                rawDomains = "",
                includeSubdomains = true,
            )

            assertNull(result)
            assertTrue(repository.getAllWithDomains().isEmpty())
            assertFalse(pathProvider.cookieSetDirectory(1L).exists())
            assertTrue(cacheDir.listFiles().orEmpty().isEmpty())
        } finally {
            rootDir.deleteRecursively()
        }
    }

    @Test
    fun invoke_returnsNullAndDeletesCookieSet_whenNoDomainsAreFound() = runTest {
        val rootDir = Files.createTempDirectory("cookie-file-import").toFile()
        val cacheDir = File(rootDir, "cache").apply { mkdirs() }
        val repository = SavingCookieSetRepository()
        val pathProvider = TempCookieSetPathProvider(rootDir)
        val cookieFileStore = CookieFileStore(pathProvider)
        val uri = mock(Uri::class.java)
        val contentResolver = mock(ContentResolver::class.java)
        val appContext = mock(Context::class.java)
        `when`(appContext.cacheDir).thenReturn(cacheDir)
        `when`(appContext.contentResolver).thenReturn(contentResolver)
        `when`(contentResolver.openInputStream(uri))
            .thenReturn(ByteArrayInputStream("not a netscape cookie file".toByteArray()))
        val useCase = createUseCase(
            appContext = appContext,
            repository = repository,
            cookieFileStore = cookieFileStore,
        )

        try {
            val result = useCase(
                name = "",
                uri = uri,
                rawDomains = "",
                includeSubdomains = true,
            )

            assertNull(result)
            assertTrue(repository.getAllWithDomains().isEmpty())
            assertFalse(pathProvider.cookieSetDirectory(1L).exists())
            assertTrue(cacheDir.listFiles().orEmpty().isEmpty())
        } finally {
            rootDir.deleteRecursively()
        }
    }

    private fun createUseCase(
        appContext: Context,
        repository: CookieSetRepository,
        cookieFileStore: CookieFileStore,
    ): CreateCookieSetFromFileUseCase {
        return CreateCookieSetFromFileUseCase(
            appContext = appContext,
            cookieSetRepository = repository,
            cookieSetDomainParser = CookieSetDomainParser(),
            cookieFileStore = cookieFileStore,
            deleteCookieSetUseCase = DeleteCookieSetUseCase(
                cookieSetRepository = repository,
                cookieFileStore = cookieFileStore,
            ),
        )
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
            return flowOf(emptyList())
        }

        override suspend fun getAllWithDomains(): List<CookieSetWithDomains> {
            return cookieSets.values.map { cookieSet ->
                CookieSetWithDomains(
                    cookieSet = cookieSet,
                    domains = cookieSetDomains[cookieSet.id].orEmpty(),
                )
            }
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
    }

    private class TempCookieSetPathProvider(
        private val rootDir: File,
    ) : CookieSetPathProvider {
        override fun cookiesDir(): File = rootDir.apply { mkdirs() }

        override fun cookieSetDir(cookieSetId: Long): File {
            return cookieSetDirectory(cookieSetId).apply { mkdirs() }
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

        fun cookieSetDirectory(cookieSetId: Long): File {
            return File(rootDir, cookieSetId.toString())
        }
    }
}
