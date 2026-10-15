package org.strigate.ferrot.app.integration

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

open class MediaToolsClient @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    open suspend fun probe(file: File): String = withMediaProbeTimeout {
        execute(
            tool = "ffprobe",
            arguments = listOf(
                "-v", "error", "-show_streams", "-show_format", "-of", "json",
                file.absolutePath,
            ),
        )
    }

    open suspend fun remux(arguments: List<String>): Unit = withMediaRemuxTimeout {
        execute(
            tool = "ffmpeg",
            arguments = listOf("-nostdin", "-hide_banner", "-loglevel", "error") + arguments
        )
    }

    private suspend fun execute(tool: String, arguments: List<String>): String {
        val packages = File(appContext.noBackupFilesDir, "youtubedl-android/packages")
        val libraryPath = listOf("python", "ffmpeg", "aria2c").joinToString(":") {
            File(packages, "$it/usr/lib").absolutePath
        }
        return runMediaProcess(
            command = listOf(
                File(
                    appContext.applicationInfo.nativeLibraryDir,
                    "lib$tool.so"
                ).absolutePath
            ) + arguments,
            environment = mapOf(
                "LD_LIBRARY_PATH" to libraryPath,
                "TMPDIR" to appContext.cacheDir.absolutePath,
            ),
            temporaryDirectory = appContext.cacheDir,
        )
    }
}

internal suspend fun withMediaProbeTimeout(block: suspend () -> String): String {
    val result = withTimeoutOrNull(60_000L.milliseconds) { block() }
    currentCoroutineContext().ensureActive()
    return result ?: throw IOException("Media inspection timed out")
}

internal suspend fun withMediaRemuxTimeout(block: suspend () -> Unit) {
    val completed = withTimeoutOrNull(30.minutes) {
        block()
        true
    }
    currentCoroutineContext().ensureActive()
    if (completed == null) throw IOException("Media processing timed out")
}

private const val MAX_MEDIA_PROCESS_OUTPUT_BYTES = 8L * 1024 * 1024

internal suspend fun runMediaProcess(
    command: List<String>,
    environment: Map<String, String>,
    temporaryDirectory: File,
): String = withContext(Dispatchers.IO) {
    val output = File.createTempFile("media-process-", ".log", temporaryDirectory)
    var process: Process? = null
    try {
        currentCoroutineContext().ensureActive()
        val runningProcess = ProcessBuilder(command)
            .apply { environment().putAll(environment) }
            .redirectErrorStream(true)
            .redirectOutput(output)
            .start()
        process = runningProcess
        while (runningProcess.isAlive) {
            if (output.length() > MAX_MEDIA_PROCESS_OUTPUT_BYTES) {
                throw IOException("Media processing output is too large")
            }
            delay(100L.milliseconds)
        }
        currentCoroutineContext().ensureActive()
        if (runningProcess.exitValue() != 0) {
            throw IOException("Media processing failed (exit ${runningProcess.exitValue()})")
        }
        if (output.length() > MAX_MEDIA_PROCESS_OUTPUT_BYTES) {
            throw IOException("Media processing output is too large")
        }
        output.readText()
    } finally {
        process?.let {
            if (it.isAlive) {
                it.destroyForcibly()
                it.waitFor(5L, TimeUnit.SECONDS)
            }
        }
        output.delete()
    }
}
