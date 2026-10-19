package org.strigate.ferrot.domain.usecase.cookieset

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.di.module.IoDispatcher
import org.strigate.ferrot.app.integration.CookieFileStore
import org.strigate.ferrot.cookies.CookieSetDomainParser
import org.strigate.ferrot.cookies.ParsedCookieDomain
import org.strigate.ferrot.domain.model.CookieSet
import org.strigate.ferrot.domain.model.CookieSetDomain
import org.strigate.ferrot.domain.model.CookieSetSource
import org.strigate.ferrot.domain.model.CookieSetWithDomains
import org.strigate.ferrot.domain.repository.CookieSetRepository
import java.io.File
import javax.inject.Inject

class CreateCookieSetFromFileUseCase @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cookieSetRepository: CookieSetRepository,
    private val cookieSetDomainParser: CookieSetDomainParser,
    private val cookieFileStore: CookieFileStore,
    private val deleteCookieSetUseCase: DeleteCookieSetUseCase,
) {
    suspend operator fun invoke(
        name: String,
        uri: Uri,
        rawDomains: String,
        includeSubdomains: Boolean,
    ): CookieSetWithDomains? {
        val cookieSetName = name.trim().ifBlank {
            displayNameFromUri(uri) ?: DEFAULT_PROFILE_NAME
        }
        val cookieSetId = cookieSetRepository.saveCookieSet(
            CookieSet(
                name = cookieSetName,
                source = CookieSetSource.IMPORTED_FILE,
                cookieFilePath = "",
            )
        )

        return runCatching {
            val tempFile = copyUriToTempFile(uri) ?: return@runCatching null
            try {
                val cookieFile = cookieFileStore.copyCookies(cookieSetId, tempFile)
                val parsedDomains = cookieSetDomainParser
                    .parseDomainList(rawDomains, includeSubdomains)
                    .ifEmpty {
                        withContext(ioDispatcher) {
                            cookieSetDomainParser.parseNetscapeDomains(cookieFile)
                        }
                    }

                if (parsedDomains.isEmpty()) {
                    return@runCatching null
                }

                cookieSetRepository.updateCookieFilePath(cookieSetId, cookieFile.absolutePath)
                cookieSetRepository.saveDomains(
                    parsedDomains.map { domain ->
                        CookieSetDomain(
                            cookieSetId = cookieSetId,
                            domain = domain.domain,
                            includeSubdomains = domain.includeSubdomains,
                        )
                    }
                )
                deleteCookieSetsWithMatchingDomains(cookieSetId, parsedDomains)
                cookieSetRepository.getByIdWithDomains(cookieSetId)
            } finally {
                withContext(NonCancellable + ioDispatcher) {
                    tempFile.delete()
                }
            }
        }.getOrElse { throwable ->
            withContext(NonCancellable) {
                deleteCookieSetUseCase(cookieSetId)
            }
            throw throwable
        }.also { cookieSetWithDomains ->
            if (cookieSetWithDomains == null) {
                deleteCookieSetUseCase(cookieSetId)
            }
        }
    }

    private suspend fun deleteCookieSetsWithMatchingDomains(
        currentCookieSetId: Long,
        domains: List<ParsedCookieDomain>,
    ) {
        cookieSetRepository
            .getCookieSetIdsByDomains(domains.map { it.domain })
            .filter { cookieSetId -> cookieSetId != currentCookieSetId }
            .forEach { cookieSetId ->
                deleteCookieSetUseCase(cookieSetId)
            }
    }

    private suspend fun copyUriToTempFile(uri: Uri): File? {
        return withContext(ioDispatcher) {
            val tempFile = File.createTempFile("cookie-import", ".txt", appContext.cacheDir)
            try {
                val inputStream = appContext.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    tempFile.delete()
                    return@withContext null
                }
                inputStream.use {
                    tempFile.outputStream().use { outputStream ->
                        it.copyTo(outputStream)
                    }
                }
                tempFile
            } catch (throwable: Throwable) {
                tempFile.delete()
                throw throwable
            }
        }
    }

    private suspend fun displayNameFromUri(uri: Uri): String? {
        return withContext(ioDispatcher) {
            runCatching {
                appContext.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex < 0 || !cursor.moveToFirst()) {
                        return@use null
                    }
                    cursor.getString(nameIndex)
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                }
            }.getOrNull()
        }
    }

    companion object {
        private const val DEFAULT_PROFILE_NAME = "Imported cookies"
    }
}
