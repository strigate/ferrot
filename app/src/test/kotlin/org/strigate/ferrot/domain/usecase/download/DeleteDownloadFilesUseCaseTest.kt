package org.strigate.ferrot.domain.usecase.download

import android.util.Log
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
import org.junit.rules.TemporaryFolder
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.app.provider.DownloadPathProvider
import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.test.MainDispatcherRule
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteDownloadFilesUseCaseTest {
    private lateinit var autoCloseable: AutoCloseable

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testDispatcher: TestDispatcher = mainDispatcherRule.testDispatcher

    private var logMock: MockedStatic<Log>? = null

    @Mock
    private lateinit var downloadPathProvider: DownloadPathProvider

    @Mock
    private lateinit var getDownloadByIdUseCase: GetDownloadByIdUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)

        logMock = mockStatic(Log::class.java)
    }

    @Test
    fun invoke_returnsFalse_whenDownloadMissing() = runTest(testDispatcher) {
        `when`(getDownloadByIdUseCase.invoke(21L))
            .thenReturn(null)

        val result = createUseCase().invoke(21L)

        assertFalse(result)
    }

    @Test
    fun invoke_returnsTrue_whenUidDirMissing() = runTest(testDispatcher) {
        val download = sampleDownload(22L)
        val missingDir = File(temporaryFolder.newFolder("delete-files-missing"), "nope")

        `when`(getDownloadByIdUseCase.invoke(download.id))
            .thenReturn(download)
        `when`(downloadPathProvider.uidDir(download.uid))
            .thenReturn(missingDir)

        val result = createUseCase().invoke(download.id)

        assertTrue(result)
    }

    @Test
    fun invoke_returnsTrue_whenUidDirDeleted() = runTest(testDispatcher) {
        val download = sampleDownload(23L)
        val uidDir = temporaryFolder.newFolder("delete-files-success").apply {
            resolve("file.txt").writeText("content")
        }
        `when`(getDownloadByIdUseCase.invoke(download.id))
            .thenReturn(download)
        `when`(downloadPathProvider.uidDir(download.uid))
            .thenReturn(uidDir)

        val result = createUseCase().invoke(download.id)

        assertTrue(result)
        assertFalse(uidDir.exists())
    }

    @Test
    fun invoke_returnsFalse_whenDeletionFails() = runTest(testDispatcher) {
        val download = sampleDownload(24L)
        val parentDir = temporaryFolder.newFolder("delete-files-failure")
        val uidDir = object : File(parentDir, "uid-${download.id}") {
            override fun exists(): Boolean = true
            override fun isDirectory(): Boolean = true
            override fun listFiles(): Array<File> = emptyArray()
            override fun delete(): Boolean = false
        }
        `when`(getDownloadByIdUseCase.invoke(download.id))
            .thenReturn(download)
        `when`(downloadPathProvider.uidDir(download.uid))
            .thenReturn(uidDir)

        val result = createUseCase().invoke(download.id)
        assertFalse(result)
    }

    @After
    fun tearDown() {
        logMock?.close()
        autoCloseable.close()
    }

    private fun createUseCase() = DeleteDownloadFilesUseCase(
        downloadPathProvider = downloadPathProvider,
        getDownloadByIdUseCase = getDownloadByIdUseCase,
    )

    private fun sampleDownload(id: Long) = Download(
        id = id,
        uid = "uid-$id",
        url = "https://example.com/$id",
        status = DownloadStatus.QUEUED,
        seen = false,
    )
}
