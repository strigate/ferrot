package org.strigate.ferrot.domain.usecase.download

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.app.integration.MediaToolsClient
import org.strigate.ferrot.app.integration.runMediaProcess
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.WatchService

class StripMediaMetadataIntegrationTest {
    private lateinit var autoCloseable: AutoCloseable

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val ffmpeg = File("/usr/bin/ffmpeg")

    private val ffprobe = File("/usr/bin/ffprobe")

    private lateinit var client: MediaToolsClient

    @Mock
    private lateinit var context: Context

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)

        assumeTrue(
            "Desktop FFmpeg and ffprobe are required",
            ffmpeg.canExecute() && ffprobe.canExecute(),
        )
        client = object : MediaToolsClient(context) {
            override suspend fun probe(file: File): String = runMediaProcess(
                command = listOf(
                    ffprobe.path,
                    "-v",
                    "error",
                    "-show_streams",
                    "-show_format",
                    "-show_chapters",
                    "-of",
                    "json",
                    file.path,
                ),
                environment = emptyMap(),
                temporaryDirectory = temporaryFolder.root,
            )

            override suspend fun remux(arguments: List<String>) {
                runFfmpeg(arguments)
            }
        }
    }

    @Test
    fun stripsTagsWithoutChangingDecodedMedia() = runTest {
        val metadata = temporaryFolder.newFile("metadata.txt").apply {
            writeText(";FFMETADATA1\ntitle=Private title\nartist=Private author\ncomment=Private URL\n[CHAPTER]\nTIMEBASE=1/1000\nSTART=0\nEND=500\ntitle=Private chapter\n")
        }
        for (extension in listOf("mp4", "mov", "mkv", "webm", "mp3")) {
            val file = File(temporaryFolder.root, "media with 'quotes' $extension.$extension")
            val audio = extension == "mp3"
            runFfmpeg(
                arguments = listOf(
                    "-f",
                    "lavfi",
                    "-i",
                    if (audio) "sine=frequency=440:duration=1" else "color=c=red:s=64x64:r=10:d=1",
                    "-f",
                    "ffmetadata",
                    "-i",
                    metadata.path,
                    "-map",
                    if (audio) "0:a" else "0:v",
                    "-map_metadata",
                    "1",
                    "-map_chapters",
                    "1",
                    if (audio) "-c:a" else "-c:v",
                    if (audio) "libmp3lame" else if (extension == "webm") "libvpx" else "libx264",
                    file.path,
                ),
            )
            val originalHash = decodedHash(file)
            assertTrue(client.probe(file).contains("Private"))

            StripMediaMetadataUseCase(client)(file)

            val info = client.probe(file)
            assertFalse("Private metadata survived in $extension", info.contains("Private"))
            assertEquals(0, JSONObject(info).getJSONArray("chapters").length())
            assertEquals("Decoded media changed in $extension", originalHash, decodedHash(file))
        }
        assertFalse(temporaryFolder.root.listFiles()!!.any { it.name.startsWith(".metadata-") })
    }

    @Test
    fun preservesRotationAndRemovesAttachments() = runTest {
        val original = File(temporaryFolder.root, "original.mp4")
        runFfmpeg(
            arguments = listOf(
                "-f",
                "lavfi",
                "-i",
                "color=c=red:s=64x32:r=10:d=1",
                "-c:v",
                "libx264",
                original.path,
            ),
        )
        val rotated = File(temporaryFolder.root, "rotated.mp4")
        runFfmpeg(
            arguments = listOf(
                "-display_rotation",
                "90",
                "-i",
                original.path,
                "-c",
                "copy",
                rotated.path,
            ),
        )
        StripMediaMetadataUseCase(client)(rotated)
        val rotation = JSONObject(client.probe(rotated)).getJSONArray("streams").getJSONObject(0)
            .getJSONArray("side_data_list").getJSONObject(0).getInt("rotation")
        assertEquals(90, rotation)

        val attachment = temporaryFolder.newFile("private.json").apply {
            writeText("Private source URL")
        }
        val attached = File(temporaryFolder.root, "attached.mkv")
        runFfmpeg(
            arguments = listOf(
                "-i",
                original.path,
                "-c",
                "copy",
                "-attach",
                attachment.path,
                "-metadata:s:t",
                "mimetype=application/json",
                attached.path,
            ),
        )
        assertEquals(2, JSONObject(client.probe(attached)).getJSONArray("streams").length())
        StripMediaMetadataUseCase(client)(attached)
        assertEquals(1, JSONObject(client.probe(attached)).getJSONArray("streams").length())

        val cover = File(temporaryFolder.root, "cover.jpg")
        runFfmpeg(listOf("-f", "lavfi", "-i", "color=c=blue:s=64x64", "-frames:v", "1", cover.path))
        val mp3 = File(temporaryFolder.root, "cover.mp3")
        runFfmpeg(
            arguments = listOf(
                "-f",
                "lavfi",
                "-i",
                "sine=frequency=440:duration=1",
                "-i",
                cover.path,
                "-map",
                "0:a",
                "-map",
                "1:v",
                "-c:a",
                "libmp3lame",
                "-c:v",
                "copy",
                "-disposition:v",
                "attached_pic",
                mp3.path,
            ),
        )
        assertEquals(2, JSONObject(client.probe(mp3)).getJSONArray("streams").length())
        StripMediaMetadataUseCase(client)(mp3)
        assertEquals(1, JSONObject(client.probe(mp3)).getJSONArray("streams").length())
    }

    @Test
    fun cancellationTerminatesNativeProcessAndCleansLogs() = runTest {
        val pidFile = File(temporaryFolder.root, "process.pid")
        FileSystems.getDefault().newWatchService().use { watcher ->
            temporaryFolder.root.toPath().register(watcher, ENTRY_CREATE)
            val command = listOf(
                "/bin/sh",
                "-c",
                "echo $$ > \"$1.tmp\"; mv \"$1.tmp\" \"$1\"; exec cat",
                "test",
                pidFile.path,
            )
            val process = async(Dispatchers.IO) {
                runMediaProcess(
                    command = command,
                    environment = emptyMap(),
                    temporaryDirectory = temporaryFolder.root,
                )
            }
            try {
                val processId = awaitProcessId(file = pidFile, watcher = watcher)
                process.cancelAndJoin()
                assertFalse(ProcessHandle.of(processId).map { it.isAlive }.orElse(false))
                assertFalse(
                    temporaryFolder.root.listFiles()!!.any { it.name.startsWith("media-process-") })
            } finally {
                process.cancelAndJoin()
            }
        }
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private suspend fun awaitProcessId(file: File, watcher: WatchService): Long =
        runInterruptible(Dispatchers.IO) {
            while (!file.exists()) {
                val event = watcher.take()
                event.pollEvents()
                check(event.reset()) { "Process readiness watcher closed" }
            }
            file.readText().trim().toLong()
        }

    private suspend fun runFfmpeg(arguments: List<String>): String = runMediaProcess(
        command = listOf(ffmpeg.path, "-nostdin", "-v", "error", "-y") + arguments,
        environment = emptyMap(),
        temporaryDirectory = temporaryFolder.root,
    )

    private suspend fun decodedHash(file: File): String = runFfmpeg(
        arguments = listOf(
            "-i",
            file.path,
            "-map",
            "0:V?",
            "-map",
            "0:a?",
            "-f",
            "hash",
            "-hash",
            "sha256",
            "-",
        ),
    ).trim()
}
