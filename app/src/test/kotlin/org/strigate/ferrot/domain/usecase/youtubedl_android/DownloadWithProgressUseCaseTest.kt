package org.strigate.ferrot.domain.usecase.youtubedl_android

import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.test.MainDispatcherRule
import org.strigate.ferrot.app.YoutubeDlRuntimeInitializer
import org.strigate.ferrot.app.integration.YoutubeDlClient
import org.strigate.ferrot.domain.model.DownloadMediaType
import org.strigate.ferrot.domain.model.QualityProfile
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadWithProgressUseCaseTest {
    private lateinit var autoCloseable: AutoCloseable
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)
    private lateinit var fakeClient: FakeYoutubeDlClient

    @Mock
    private lateinit var youtubeDlRuntimeInitializer: YoutubeDlRuntimeInitializer

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        fakeClient = FakeYoutubeDlClient()
    }

    @Test
    fun invoke_buildsVideoRequest_andReportsOutputPath() = runTest(testDispatcher) {
        val outputPathFile = Files.createTempFile("download-with-progress", ".txt").toFile()
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
        assertEquals("/tmp/%(title)s.%(ext)s", fakeClient.capturedRequest?.getOption("-o"))
        assertEquals("aria2c", fakeClient.capturedRequest?.getOption("--external-downloader"))
        verify(youtubeDlRuntimeInitializer)
            .initializeIfNeeded()
        assertEquals(listOf("process-1"), fakeClient.destroyedProcessIds)
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

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private fun createUseCase() = DownloadWithProgressUseCase(
        buildVideoDownloadRequestUseCase = BuildVideoDownloadRequestUseCase(),
        buildAudioDownloadRequestUseCase = BuildAudioDownloadRequestUseCase(),
        youtubeDlRuntimeInitializer = youtubeDlRuntimeInitializer,
        youtubeDlClient = fakeClient,
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
