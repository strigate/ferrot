package org.strigate.ferrot.domain.usecase.availableupdate

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.test.MainDispatcherRule
import org.strigate.ferrot.app.provider.UpdatePathProvider
import org.strigate.ferrot.domain.repository.AvailableUpdateRepository
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class ClearAvailableUpdateFilesAndDataUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val testDispatcher: TestDispatcher = mainDispatcherRule.testDispatcher
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var availableUpdateRepository: AvailableUpdateRepository

    @Mock
    private lateinit var updatePathProvider: UpdatePathProvider

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun invoke_returnsTrue_whenDatabaseDeleteSucceeds() = runTest(testDispatcher) {
        val updatesDir = File(
            Files.createTempDirectory("clear-update-db-success").toFile(), "missing",
        )
        `when`(availableUpdateRepository.delete())
            .thenReturn(1)
        `when`(updatePathProvider.updatesDir())
            .thenReturn(updatesDir)

        val result = createUseCase().invoke()

        assertTrue(result)
    }

    @Test
    fun invoke_returnsTrue_whenFilesDeleteSucceeds() = runTest(testDispatcher) {
        val updatesDir = Files.createTempDirectory("clear-update-files-success").toFile()
            .apply {
                resolve("update.apk").writeText("apk")
            }

        `when`(availableUpdateRepository.delete())
            .thenReturn(0)
        `when`(updatePathProvider.updatesDir())
            .thenReturn(updatesDir)

        val result = createUseCase().invoke()

        assertTrue(result)
        assertFalse(updatesDir.exists())
    }

    @Test
    fun invoke_returnsFalse_whenDatabaseAndFileDeleteFail() = runTest(testDispatcher) {
        val parentDir = Files.createTempDirectory("clear-update-fail").toFile()
        val updatesDir = object : File(parentDir, "updates") {
            override fun exists(): Boolean = true
            override fun isDirectory(): Boolean = true
            override fun listFiles(): Array<File> = emptyArray()
            override fun delete(): Boolean = false
        }

        `when`(availableUpdateRepository.delete())
            .thenReturn(0)
        `when`(updatePathProvider.updatesDir())
            .thenReturn(updatesDir)

        val result = createUseCase().invoke()

        assertFalse(result)
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private fun createUseCase() = ClearAvailableUpdateFilesAndDataUseCase(
        availableUpdateRepository = availableUpdateRepository,
        updatePathProvider = updatePathProvider,
    )
}
