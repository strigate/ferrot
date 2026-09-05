package org.strigate.ferrot.domain.usecase.downloadmetadata

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class IsDownloadThumbnailAvailableUseCaseTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val useCase = IsDownloadThumbnailAvailableUseCase()

    @Test
    fun rejectsMissingEmptyAndDirectoryPaths() = runTest {
        assertFalse(useCase(null))
        assertFalse(useCase(""))
        assertFalse(useCase(temporaryFolder.root.resolve("missing.jpg").path))
        assertFalse(useCase(temporaryFolder.newFile().path))
        assertFalse(useCase(temporaryFolder.root.path))
    }

    @Test
    fun acceptsNonemptyFile() = runTest {
        val file = temporaryFolder.newFile()
        file.writeBytes(byteArrayOf(1))

        assertTrue(useCase(file.path))
    }
}
