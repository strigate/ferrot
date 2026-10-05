package org.strigate.ferrot.domain.usecase.download

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.strigate.ferrot.app.integration.MediaToolsClient
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max

class StripMediaMetadataUseCase @Inject constructor(
    private val mediaToolsClient: MediaToolsClient,
) {
    suspend operator fun invoke(file: File): Unit = withContext(Dispatchers.IO) {
        var replacement: File? = null
        try {
            require(file.isFile && file.length() > 0L) { "Media file is missing or empty" }
            val original = inspect(mediaToolsClient.probe(file))
            require(original.streams.any { it.type == "video" || it.type == "audio" }) {
                "No playable media streams"
            }
            val output = File.createTempFile(".metadata-", ".${file.extension}", file.parentFile)
            replacement = output
            val arguments = buildList {
                addAll(listOf("-y", "-i", file.absolutePath))
                original.streams.forEach { addAll(listOf("-map", "0:${it.index}")) }
                addAll(
                    listOf(
                        "-c", "copy",
                        "-map_metadata", "-1",
                        "-map_metadata:s", "-1",
                        "-map_chapters", "-1",
                        "-fflags", "+bitexact",
                        "-metadata", "encoder=",
                        "-metadata:s", "encoder=",
                    ),
                )
                if (file.extension.equals("mp3", ignoreCase = true)) {
                    addAll(listOf("-id3v2_version", "0", "-write_id3v1", "0"))
                }
                add(output.absolutePath)
            }
            mediaToolsClient.remux(arguments)
            require(output.length() > 0L) { "Metadata cleanup produced an empty file" }
            val cleaned = inspect(mediaToolsClient.probe(output))
            require(original.streams.map { it.copy(index = 0) } == cleaned.streams.map {
                it.copy(
                    index = 0
                )
            }) {
                "Metadata cleanup changed media streams or playback properties"
            }
            if (original.duration != null && cleaned.duration != null) {
                require(
                    abs(original.duration - cleaned.duration) <= max(
                        1.0,
                        original.duration * 0.005
                    )
                ) {
                    "Metadata cleanup changed media duration"
                }
            }
            currentCoroutineContext().ensureActive()
            Files.move(output.toPath(), file.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            throw MetadataStripException(exception)
        } finally {
            replacement?.delete()
        }
    }

    private fun inspect(json: String): MediaInfo {
        val info = JSONObject(json)
        val streams = info.getJSONArray("streams")
        val mediaStreams = buildList {
            for (index in 0 until streams.length()) {
                val stream = streams.getJSONObject(index)
                val type = stream.optString("codec_type")
                if (type !in listOf("video", "audio", "subtitle")) continue
                if (stream.optJSONObject("disposition")?.optInt("attached_pic") == 1) continue
                val properties = listOf(
                    "codec_name",
                    "width",
                    "height",
                    "sample_rate",
                    "channels",
                    "sample_aspect_ratio",
                    "color_range",
                    "color_space",
                    "color_transfer",
                    "color_primaries",
                ).associateWith { stream.optString(it) }
                val sideData = stream.optJSONArray("side_data_list")
                var rotation = 0
                if (sideData != null) {
                    for (sideIndex in 0 until sideData.length()) {
                        val data = sideData.getJSONObject(sideIndex)
                        if (data.has("rotation")) rotation = data.getInt("rotation")
                    }
                }
                add(MediaStream(stream.getInt("index"), type, properties, rotation))
            }
        }
        return MediaInfo(
            streams = mediaStreams,
            duration = info.optJSONObject("format")?.optString("duration")?.toDoubleOrNull(),
        )
    }

    private data class MediaInfo(val streams: List<MediaStream>, val duration: Double?)

    private data class MediaStream(
        val index: Int,
        val type: String,
        val properties: Map<String, String>,
        val rotation: Int,
    )
}

class MetadataStripException(cause: Throwable? = null) :
    IOException("Could not strip media metadata", cause)
