package org.strigate.ferrot.domain.usecase.youtubedl_android

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.app.YoutubeDlRuntimeInitializer
import org.strigate.ferrot.app.integration.YoutubeDlClient
import org.strigate.ferrot.app.integration.MediaToolsClient
import org.strigate.ferrot.app.integration.withMediaProbeTimeout
import org.strigate.ferrot.domain.model.DownloadMediaType
import org.strigate.ferrot.domain.model.QualityProfile
import org.strigate.ferrot.domain.usecase.settings.GetIncludeAttributionEnabledSettingAsFlowUseCase
import org.strigate.ferrot.test.MainDispatcherRule
import org.strigate.ferrot.domain.usecase.settings.GetStripMediaMetadataEnabledSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.download.StripMediaMetadataUseCase
import org.strigate.ferrot.domain.usecase.download.MetadataStripException
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.verifyNoInteractions
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadWithProgressUseCaseTest {
    private lateinit var autoCloseable: AutoCloseable

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testDispatcher: TestDispatcher = mainDispatcherRule.testDispatcher

    private lateinit var fakeClient: FakeYoutubeDlClient

    @Mock
    private lateinit var youtubeDlRuntimeInitializer: YoutubeDlRuntimeInitializer

    @Mock
    private lateinit var getIncludeAttributionEnabledSettingAsFlowUseCase: GetIncludeAttributionEnabledSettingAsFlowUseCase

    @Mock
    private lateinit var getStripMediaMetadataEnabledSettingAsFlowUseCase: GetStripMediaMetadataEnabledSettingAsFlowUseCase

    @Mock
    private lateinit var stripMediaMetadataUseCase: StripMediaMetadataUseCase

    @Mock
    private lateinit var context: Context

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)

        fakeClient = FakeYoutubeDlClient()
        `when`(getStripMediaMetadataEnabledSettingAsFlowUseCase.invoke())
            .thenReturn(MutableStateFlow(false))
        `when`(getIncludeAttributionEnabledSettingAsFlowUseCase.invoke())
            .thenReturn(MutableStateFlow(true))
    }

    @Test
    fun invoke_buildsVideoRequest_andReportsOutputPath() = runTest(testDispatcher) {
        val outputPathFile = temporaryFolder.newFile("download-with-progress.txt")
        var reportedOutputPath: String? = null
        fakeClient.onExecute = { request, _, _, _ ->
            outputPathFile.writeText("/storage/emulated/0/Movies/video.mp4\n")
            fakeClient.capturedRequest = request
            YoutubeDLResponse(
                command = listOf("yt-dlp"),
                exitCode = 0,
                elapsedTime = 100L,
                out = "",
                err = "",
            )
        }

        val result = createUseCase().invoke(
            url = "https://example.com/video",
            template = "/tmp/%(title)s.%(ext)s",
            profile = QualityProfile.MAX,
            processId = "process-1",
            bytesProvider = { 123L },
            outputPathFile = outputPathFile,
            onOutputFilePath = { reportedOutputPath = it },
        ).toList()

        assertTrue(result.isEmpty())
        assertEquals("/storage/emulated/0/Movies/video.mp4", reportedOutputPath)
        assertEquals("bv*+ba/b", fakeClient.capturedRequest?.getOption("-f"))
        assertTrue(fakeClient.capturedRequest?.hasOption("--postprocessor-args") == true)
        assertEquals("/tmp/%(title)s.%(ext)s", fakeClient.capturedRequest?.getOption("-o"))
        assertEquals("aria2c", fakeClient.capturedRequest?.getOption("--external-downloader"))
        verify(youtubeDlRuntimeInitializer)
            .initializeIfNeeded()

        assertEquals(listOf("process-1"), fakeClient.destroyedProcessIds)
        verifyNoInteractions(stripMediaMetadataUseCase)
    }

    @Test
    fun invoke_buildsAudioRequest_forAudioDownloads() = runTest(testDispatcher) {
        fakeClient.onExecute = { request, _, _, _ ->
            fakeClient.capturedRequest = request
            YoutubeDLResponse(
                command = listOf("yt-dlp"),
                exitCode = 0,
                elapsedTime = 50L,
                out = "",
                err = "",
            )
        }

        val result = createUseCase().invoke(
            url = "https://example.com/audio",
            template = "/tmp/%(title)s.%(ext)s",
            profile = QualityProfile.CAP_2160,
            processId = "process-2",
            bytesProvider = { 456L },
            downloadMediaType = DownloadMediaType.AUDIO,
        ).toList()

        assertTrue(result.isEmpty())
        assertEquals("ba/b", fakeClient.capturedRequest?.getOption("-f"))
        assertEquals("/tmp/%(title)s.%(ext)s", fakeClient.capturedRequest?.getOption("-o"))
        assertTrue(fakeClient.capturedRequest?.hasOption("--extract-audio") == true)
    }

    @Test
    fun invoke_addsCookiesToRequest_whenCookieFilePathProvided() = runTest(testDispatcher) {
        fakeClient.onExecute = { request, _, _, _ ->
            fakeClient.capturedRequest = request
            YoutubeDLResponse(
                command = listOf("yt-dlp"),
                exitCode = 0,
                elapsedTime = 50L,
                out = "",
                err = "",
            )
        }

        createUseCase().invoke(
            url = "https://example.com/video",
            template = "/tmp/%(title)s.%(ext)s",
            profile = QualityProfile.MAX,
            processId = "process-cookies",
            bytesProvider = { 456L },
            cookieFilePath = "/tmp/cookies.txt",
        ).toList()

        assertEquals("/tmp/cookies.txt", fakeClient.capturedRequest?.getOption("--cookies"))
    }

    @Test
    fun invoke_throwsWhenYoutubeDlReturnsNonZeroExitCode() = runTest(testDispatcher) {
        fakeClient.onExecute = { _, _, _, _ ->
            YoutubeDLResponse(
                command = listOf("yt-dlp"),
                exitCode = 1,
                elapsedTime = 10L,
                out = "",
                err = "boom",
            )
        }

        val failure = runCatching {
            createUseCase().invoke(
                url = "https://example.com/video",
                template = "/tmp/%(title)s.%(ext)s",
                profile = QualityProfile.MAX,
                processId = "process-3",
                bytesProvider = { 0L },
            ).toList()
        }.exceptionOrNull()

        assertNotNull(failure)
        assertTrue(failure is IllegalStateException)
        assertEquals("Exit code 1", failure?.message)
        assertEquals(listOf("process-3"), fakeClient.destroyedProcessIds)
    }

    @Test
    fun invoke_readsAttributionPreferenceForEachDownload() = runTest(testDispatcher) {
        val attributionEnabled = MutableStateFlow(false)

        `when`(getIncludeAttributionEnabledSettingAsFlowUseCase.invoke())
            .thenReturn(attributionEnabled)

        val useCase = createUseCase()
        for (enabled in listOf(false, true)) {
            attributionEnabled.value = enabled
            useCase(
                url = "https://example.com/video",
                template = "/tmp/%(title)s.%(ext)s",
                profile = QualityProfile.MAX,
                processId = "process-attribution-$enabled",
                bytesProvider = { 0L },
            ).toList()

            assertEquals(enabled, fakeClient.capturedRequest?.hasOption("--postprocessor-args"))
            assertTrue(fakeClient.capturedRequest?.hasOption("--add-metadata") == true)
        }
    }

    @Test
    fun invoke_cleansBothMediaTypesBeforeReportingOutput() = runTest(testDispatcher) {
        `when`(getStripMediaMetadataEnabledSettingAsFlowUseCase.invoke())
            .thenReturn(MutableStateFlow(true))

        for (type in DownloadMediaType.entries) {
            val pathFile = temporaryFolder.newFile("path-$type.txt")
            val mediaFile = temporaryFolder.newFile("media-$type.mp4")
            var cleaned = false
            var reported = false
            doAnswer {
                cleaned = true
                null
            }.`when`(stripMediaMetadataUseCase).invoke(mediaFile)
            fakeClient.onExecute = { _, _, _, _ ->
                pathFile.writeText(mediaFile.absolutePath)
                YoutubeDLResponse(emptyList(), 0, 0L, "", "")
            }
            createUseCase()(
                url = "https://example.com/video",
                template = "/tmp/%(title)s.%(ext)s",
                profile = QualityProfile.MAX,
                processId = "strip-$type",
                bytesProvider = { 1L },
                downloadMediaType = type,
                outputPathFile = pathFile,
                onOutputFilePath = {
                    assertTrue(cleaned)
                    reported = true
                },
            ).toList()

            assertTrue(reported)
            assertTrue(fakeClient.capturedRequest?.hasOption("--add-metadata") == false)
            assertTrue(fakeClient.capturedRequest?.hasOption("--postprocessor-args") == false)
            verify(stripMediaMetadataUseCase)
                .invoke(mediaFile)
        }
    }

    @Test
    fun invoke_doesNotReportOutputWhenCleanupFails() = runTest(testDispatcher) {
        `when`(getStripMediaMetadataEnabledSettingAsFlowUseCase.invoke())
            .thenReturn(MutableStateFlow(true))

        val pathFile = temporaryFolder.newFile("failed-path.txt")
        val mediaFile = temporaryFolder.newFile("failed.mp4")
        doAnswer { throw MetadataStripException() }
            .`when`(stripMediaMetadataUseCase)
            .invoke(mediaFile)

        fakeClient.onExecute = { _, _, _, _ ->
            pathFile.writeText(mediaFile.absolutePath)
            YoutubeDLResponse(emptyList(), 0, 0L, "", "")
        }
        var reported = false
        val failure = runCatching {
            createUseCase()(
                url = "https://example.com/video",
                template = "/tmp/%(title)s.%(ext)s",
                profile = QualityProfile.MAX,
                processId = "failed-strip",
                bytesProvider = { 0L },
                outputPathFile = pathFile,
                onOutputFilePath = { reported = true },
            ).toList()
        }.exceptionOrNull()

        assertTrue(failure is MetadataStripException)
        assertEquals(false, reported)
    }

    @Test
    fun invoke_reportsProbeTimeoutWithoutHanging() = runTest(testDispatcher) {
        `when`(getStripMediaMetadataEnabledSettingAsFlowUseCase.invoke())
            .thenReturn(MutableStateFlow(true))
        val pathFile = temporaryFolder.newFile("timeout-path.txt")
        val mediaFile = temporaryFolder.newFile("timeout.mp4").apply { writeText("original") }
        val mediaToolsClient = object : MediaToolsClient(context) {
            override suspend fun probe(file: File): String = withContext(testDispatcher) {
                withMediaProbeTimeout { awaitCancellation() }
            }
        }
        fakeClient.onExecute = { _, _, _, _ ->
            pathFile.writeText(mediaFile.absolutePath)
            YoutubeDLResponse(emptyList(), 0, 0L, "", "")
        }
        var reported = false

        val failure = runCatching {
            createUseCase(metadataStripper = StripMediaMetadataUseCase(mediaToolsClient))(
                url = "https://example.com/video",
                template = "/tmp/%(title)s.%(ext)s",
                profile = QualityProfile.MAX,
                processId = "probe-timeout",
                bytesProvider = { 0L },
                outputPathFile = pathFile,
                onOutputFilePath = { reported = true },
            ).toList()
        }.exceptionOrNull()

        assertTrue(failure is MetadataStripException)
        assertEquals(
            "Media inspection timed out",
            generateSequence(failure) { it.cause }.last().message
        )
        assertEquals(false, reported)
        assertEquals("original", mediaFile.readText())
        assertEquals(listOf("probe-timeout"), fakeClient.destroyedProcessIds)
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private fun createUseCase(
        metadataStripper: StripMediaMetadataUseCase = stripMediaMetadataUseCase,
    ) = DownloadWithProgressUseCase(
        buildVideoDownloadRequestUseCase = BuildVideoDownloadRequestUseCase(),
        buildAudioDownloadRequestUseCase = BuildAudioDownloadRequestUseCase(),
        youtubeDlRuntimeInitializer = youtubeDlRuntimeInitializer,
        youtubeDlClient = fakeClient,
        getIncludeAttributionEnabledSettingAsFlowUseCase = getIncludeAttributionEnabledSettingAsFlowUseCase,
        getStripMediaMetadataEnabledSettingAsFlowUseCase = getStripMediaMetadataEnabledSettingAsFlowUseCase,
        stripMediaMetadataUseCase = metadataStripper,
    )

    private class FakeYoutubeDlClient : YoutubeDlClient() {
        var capturedRequest: YoutubeDLRequest? = null
        var destroyedProcessIds: MutableList<String> = mutableListOf()
        var onExecute: (YoutubeDLRequest, String, Boolean, ((Float, Long, String) -> Unit)?) -> YoutubeDLResponse =
            { _, _, _, _ ->
                YoutubeDLResponse(
                    command = emptyList(),
                    exitCode = 0,
                    elapsedTime = 0L,
                    out = "",
                    err = "",
                )
            }

        override fun execute(
            request: YoutubeDLRequest,
            processId: String,
            redirectErrorStream: Boolean,
            callback: ((Float, Long, String) -> Unit)?,
        ): YoutubeDLResponse {
            capturedRequest = request
            return onExecute(request, processId, redirectErrorStream, callback)
        }

        override fun destroyProcessById(processId: String): Boolean {
            destroyedProcessIds += processId
            return true
        }
    }
}
