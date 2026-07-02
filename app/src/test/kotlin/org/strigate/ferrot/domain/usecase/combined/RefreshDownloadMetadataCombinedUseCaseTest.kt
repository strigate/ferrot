package org.strigate.ferrot.domain.usecase.combined

import android.util.Log
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.test.MainDispatcherRule
import org.strigate.ferrot.app.integration.CookieFileStore
import org.strigate.ferrot.app.provider.DownloadPathProvider
import org.strigate.ferrot.domain.model.Download
import org.strigate.ferrot.domain.model.DownloadMetadata
import org.strigate.ferrot.domain.model.DownloadStatus
import org.strigate.ferrot.domain.usecase.DownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.DownloadUseCase
import org.strigate.ferrot.domain.usecase.CookieSetUseCase
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.YoutubeDlAndroidUseCase
import org.strigate.ferrot.domain.usecase.download.GetDownloadByIdUseCase
import org.strigate.ferrot.domain.usecase.downloadmetadata.GetDownloadMetadataByIdAsFlowUseCase
import org.strigate.ferrot.domain.usecase.downloadmetadata.SaveDownloadMetadataUseCase
import org.strigate.ferrot.domain.usecase.youtubedl_android.DownloadThumbnailUseCase
import org.strigate.ferrot.domain.usecase.youtubedl_android.GetVideoInfoUseCase
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshDownloadMetadataCombinedUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val testDispatcher: TestDispatcher = mainDispatcherRule.testDispatcher
    private lateinit var autoCloseable: AutoCloseable
    private var logMock: MockedStatic<Log>? = null

    @Mock
    private lateinit var downloadUseCase: DownloadUseCase

    @Mock
    private lateinit var downloadMetadataUseCase: DownloadMetadataUseCase

    @Mock
    private lateinit var youtubeDlAndroidUseCase: YoutubeDlAndroidUseCase

    @Mock
    private lateinit var downloadPathProvider: DownloadPathProvider

    @Mock
    private lateinit var settingsUseCase: SettingsUseCase

    @Mock
    private lateinit var cookieSetUseCase: CookieSetUseCase

    @Mock
    private lateinit var cookieFileStore: CookieFileStore

    @Mock
    private lateinit var getDownloadByIdUseCase: GetDownloadByIdUseCase

    @Mock
    private lateinit var getDownloadMetadataByIdAsFlowUseCase: GetDownloadMetadataByIdAsFlowUseCase

    @Mock
    private lateinit var saveDownloadMetadataUseCase: SaveDownloadMetadataUseCase

    @Mock
    private lateinit var getVideoInfoUseCase: GetVideoInfoUseCase

    @Mock
    private lateinit var downloadThumbnailUseCase: DownloadThumbnailUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        logMock = mockStatic(Log::class.java)

        `when`(downloadUseCase.getDownloadByIdUseCase)
            .thenReturn(getDownloadByIdUseCase)
        `when`(downloadMetadataUseCase.getDownloadMetadataByIdAsFlowUseCase)
            .thenReturn(getDownloadMetadataByIdAsFlowUseCase)
        `when`(downloadMetadataUseCase.saveDownloadMetadataUseCase)
            .thenReturn(saveDownloadMetadataUseCase)
        `when`(youtubeDlAndroidUseCase.getVideoInfoUseCase)
            .thenReturn(getVideoInfoUseCase)
        `when`(youtubeDlAndroidUseCase.downloadThumbnailUseCase)
            .thenReturn(downloadThumbnailUseCase)
    }

    @Test
    fun invoke_returnsFalse_whenDownloadMissing() = runTest(testDispatcher) {
        `when`(getDownloadByIdUseCase.invoke(42L))
            .thenReturn(null)

        val useCase = createUseCase()
        val result = useCase(42L)

        assertFalse(result)
        verify(saveDownloadMetadataUseCase, never())
            .invoke(anyObject())
    }

    @Test
    fun invoke_savesMergedMetadata_whenRefreshSucceeds() = runTest(testDispatcher) {
        val download = sampleDownload()
        val outputDir = Files.createTempDirectory("refresh-meta-success").toFile()
        val videoInfo = videoInfo(
            id = "video-1",
            title = "Fresh title",
            extractorKey = "YouTube",
            duration = 321,
        )
        var savedMetadata: DownloadMetadata? = null

        `when`(getDownloadByIdUseCase.invoke(download.id))
            .thenReturn(download)
        `when`(getDownloadMetadataByIdAsFlowUseCase.invoke(download.id))
            .thenReturn(flowOf(sampleMetadata()))
        `when`(downloadPathProvider.uidDir(download.uid))
            .thenReturn(outputDir)
        `when`(getVideoInfoUseCase.invoke(download.url))
            .thenReturn(videoInfo)
        `when`(
            downloadThumbnailUseCase.invoke(
                url = download.url,
                outputDir = outputDir,
                videoId = "video-1",
            ),
        )
            .thenReturn("/tmp/new-thumb.jpg")

        doAnswer { invocation ->
            savedMetadata = invocation.getArgument(0)
            Unit
        }.`when`(saveDownloadMetadataUseCase).invoke(anyObject())

        val result = createUseCase()(download.id)

        assertTrue(result)
        assertEquals(
            DownloadMetadata(
                downloadId = download.id,
                videoId = "video-1",
                source = "youtube",
                title = "Fresh title",
                thumbnailFilePath = "/tmp/new-thumb.jpg",
                durationSeconds = 321,
            ),
            savedMetadata,
        )
    }

    @Test
    fun invoke_usesExistingThumbnail_whenRefreshFails() = runTest(testDispatcher) {
        val download = sampleDownload(id = 8L)
        val outputDir = Files.createTempDirectory("refresh-meta-thumb-fallback").toFile()
        val existingThumbnail = File(outputDir, "existing-thumb.jpg").apply {
            writeText("thumb")
        }
        val existingMetadata = sampleMetadata(
            downloadId = download.id,
            videoId = "old-id",
            source = "vimeo",
            title = "Old title",
            thumbnailFilePath = existingThumbnail.absolutePath,
            durationSeconds = 90,
        )
        var savedMetadata: DownloadMetadata? = null

        `when`(getDownloadByIdUseCase.invoke(download.id))
            .thenReturn(download)
        `when`(getDownloadMetadataByIdAsFlowUseCase.invoke(download.id))
            .thenReturn(flowOf(existingMetadata))
        `when`(downloadPathProvider.uidDir(download.uid))
            .thenReturn(outputDir)
        `when`(getVideoInfoUseCase.invoke(download.url))
            .thenThrow(RuntimeException("metadata unavailable"))

        `when`(
            downloadThumbnailUseCase.invoke(
                url = download.url,
                outputDir = outputDir,
                videoId = "old-id",
            ),
        ).thenThrow(RuntimeException("thumbnail unavailable"))

        doAnswer { invocation ->
            savedMetadata = invocation.getArgument(0)
            Unit
        }.`when`(saveDownloadMetadataUseCase).invoke(anyObject())

        val result = createUseCase()(download.id)
        assertTrue(result)
        assertEquals(existingMetadata, savedMetadata)
    }

    @Test
    fun invoke_returnsFalse_whenNothingRecovered() = runTest(testDispatcher) {
        val download = sampleDownload(id = 12L)
        val outputDir = Files.createTempDirectory("refresh-meta-empty").toFile()

        `when`(getDownloadByIdUseCase.invoke(download.id))
            .thenReturn(download)
        `when`(getDownloadMetadataByIdAsFlowUseCase.invoke(download.id))
            .thenReturn(flowOf(null))
        `when`(downloadPathProvider.uidDir(download.uid))
            .thenReturn(outputDir)
        `when`(getVideoInfoUseCase.invoke(download.url))
            .thenThrow(RuntimeException("metadata unavailable"))

        `when`(
            downloadThumbnailUseCase.invoke(
                url = download.url,
                outputDir = outputDir,
                videoId = null,
            ),
        ).thenThrow(RuntimeException("thumbnail unavailable"))

        val result = createUseCase()(download.id)
        assertFalse(result)
        verify(saveDownloadMetadataUseCase, never())
            .invoke(anyObject())
    }

    @Test
    fun invoke_keepsExistingDuration_whenFetchedDurationInvalid() = runTest(testDispatcher) {
        val download = sampleDownload(id = 15L)
        val outputDir = Files.createTempDirectory("refresh-meta-duration").toFile()
        val existingMetadata = sampleMetadata(
            downloadId = download.id,
            durationSeconds = 444,
        )
        val videoInfo = videoInfo(
            id = "video-15",
            title = "New title",
            extractorKey = "YouTube",
            duration = 0,
        )
        var savedMetadata: DownloadMetadata? = null

        `when`(getDownloadByIdUseCase.invoke(download.id))
            .thenReturn(download)
        `when`(getDownloadMetadataByIdAsFlowUseCase.invoke(download.id))
            .thenReturn(flowOf(existingMetadata))
        `when`(downloadPathProvider.uidDir(download.uid))
            .thenReturn(outputDir)
        `when`(getVideoInfoUseCase.invoke(download.url))
            .thenReturn(videoInfo)

        `when`(
            downloadThumbnailUseCase.invoke(
                url = download.url,
                outputDir = outputDir,
                videoId = "video-15",
            ),
        )
            .thenReturn("/tmp/thumb-15.jpg")

        doAnswer { invocation ->
            savedMetadata = invocation.getArgument(0)
            Unit
        }.`when`(saveDownloadMetadataUseCase).invoke(anyObject())

        val result = createUseCase()(download.id)
        assertTrue(result)
        assertEquals(444, savedMetadata?.durationSeconds)
    }

    @After
    fun tearDown() {
        logMock?.close()
        logMock = null
        autoCloseable.close()
    }

    private fun createUseCase() = RefreshDownloadMetadataCombinedUseCase(
        downloadUseCase = downloadUseCase,
        downloadMetadataUseCase = downloadMetadataUseCase,
        youtubeDlAndroidUseCase = youtubeDlAndroidUseCase,
        downloadPathProvider = downloadPathProvider,
        settingsUseCase = settingsUseCase,
        cookieSetUseCase = cookieSetUseCase,
        cookieFileStore = cookieFileStore,
    )

    private fun sampleDownload(id: Long = 7L) = Download(
        id = id,
        uid = "uid-$id",
        url = "https://example.com/watch/$id",
        status = DownloadStatus.QUEUED,
        seen = false,
    )

    private fun sampleMetadata(
        downloadId: Long = 7L,
        videoId: String? = "existing-id",
        source: String? = "youtube",
        title: String? = "Existing title",
        thumbnailFilePath: String? = "/tmp/existing.jpg",
        durationSeconds: Int? = 111,
    ) = DownloadMetadata(
        downloadId = downloadId,
        videoId = videoId,
        source = source,
        title = title,
        thumbnailFilePath = thumbnailFilePath,
        durationSeconds = durationSeconds,
    )

    private fun videoInfo(
        id: String?,
        title: String?,
        extractorKey: String?,
        duration: Int,
    ): VideoInfo {
        return VideoInfo().apply {
            setField("id", id)
            setField("title", title)
            setField("extractorKey", extractorKey)
            setField("duration", duration)
        }
    }

    private fun VideoInfo.setField(name: String, value: Any?) {
        val field = VideoInfo::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(this, value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyObject(): T = org.mockito.Mockito.any<T>() ?: null as T
}
