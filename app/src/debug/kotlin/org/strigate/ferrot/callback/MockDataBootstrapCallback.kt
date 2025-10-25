package org.strigate.ferrot.callback

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import org.strigate.ferrot.BuildConfig
import org.strigate.ferrot.R
import java.io.File
import java.io.FileOutputStream

class MockDataBootstrapCallback(
    private val appContext: Context,
) : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        if (!BuildConfig.BOOTSTRAP_MOCK_DATA) {
            return
        }
        val now = System.currentTimeMillis()
        fun copyRawToFilesDir(resId: Int, relativePath: String): String {
            val outputFile = File(appContext.filesDir, relativePath)
            outputFile.parentFile?.mkdirs()
            appContext.resources.openRawResource(resId).use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            return outputFile.absolutePath
        }

        fun insertDownload(
            id: Long,
            uid: String,
            url: String,
            status: String,
            filePath: String?,
            errorMessage: String?,
            enqueuedAt: Long,
            startedAt: Long?,
            completedAt: Long?,
        ) {
            db.execSQL(
                """
                INSERT INTO download (
                    id, uid, url, status, filePath, errorMessage,
                    enqueuedAtMillis, startedAtMillis, completedAtMillis
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    id,
                    uid,
                    url,
                    status,
                    filePath,
                    errorMessage,
                    enqueuedAt,
                    startedAt,
                    completedAt,
                ),
            )
        }

        fun insertMetadata(
            downloadId: Long,
            title: String?,
            thumbPath: String?,
        ) {
            db.execSQL(
                """
                INSERT INTO download_metadata (
                    downloadId, title, thumbnailFilePath
                ) VALUES (?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    downloadId,
                    title,
                    thumbPath,
                ),
            )
        }

        fun insertProgress(
            downloadId: Long,
            updatedAt: Long,
            percent: Float,
            bytesDownloaded: Long,
            etaSeconds: Long?,
            expectedBytes: Long?,
        ) {
            db.execSQL(
                """
                INSERT INTO download_progress (
                    downloadId, updatedAtMillis, progressPercent, bytesDownloaded, etaSeconds, expectedBytes
                ) VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    downloadId,
                    updatedAt,
                    percent.toDouble(),
                    bytesDownloaded,
                    etaSeconds,
                    expectedBytes,
                ),
            )
        }

        val mockThumbnail = copyRawToFilesDir(R.raw.mock_thumbnail, "thumbs/thumbnail_005.png")

        // 1 - COMPLETED
        insertDownload(
            id = 1L,
            uid = "example-001",
            url = "https://example.com/watch?v=dQw4w9WgXcQ",
            status = "COMPLETED",
            filePath = "downloads/example.mp4",
            errorMessage = null,
            enqueuedAt = now - 60 * 60 * 1000,
            startedAt = now - 55 * 60 * 1000,
            completedAt = now - 52 * 60 * 1000,
        )
        insertMetadata(
            downloadId = 1L,
            title = "Jetpack Compose Basics - State, Recomposition, Layout",
            thumbPath = null,
        )
        insertProgress(
            downloadId = 1L,
            updatedAt = now - 52 * 60 * 1000,
            percent = 100f,
            bytesDownloaded = 187_000_000L,
            etaSeconds = 0L,
            expectedBytes = 187_000_000L,
        )

        // 2 - DOWNLOADING
        insertDownload(
            id = 2L,
            uid = "example-002",
            url = "https://example.com/watch?v=dQw4w9WgXcQ",
            status = "DOWNLOADING",
            filePath = "downloads/example.mp4",
            errorMessage = null,
            enqueuedAt = now - 25 * 60 * 1000,
            startedAt = now - 24 * 60 * 1000,
            completedAt = null,
        )
        insertMetadata(
            downloadId = 2L,
            title = "Travel Vlog - Johannesburg Night Lights",
            thumbPath = mockThumbnail,
        )
        insertProgress(
            downloadId = 2L,
            updatedAt = now - 30_000,
            percent = 62.5f,
            bytesDownloaded = 131_072_000L,
            etaSeconds = 120L,
            expectedBytes = 209_715_200L,
        )

        // 3 - QUEUED
        insertDownload(
            id = 3L,
            uid = "example-003",
            url = "https://example.com/watch?v=dQw4w9WgXcQ",
            status = "QUEUED",
            filePath = null,
            errorMessage = null,
            enqueuedAt = now - 10 * 60 * 1000,
            startedAt = null,
            completedAt = null,
        )
        insertMetadata(
            downloadId = 3L,
            title = "Kotlin Coroutines: Flow vs Suspend - When to Use Which",
            thumbPath = null,
        )
        insertProgress(
            downloadId = 3L,
            updatedAt = now - 10 * 60 * 1000,
            percent = 0f,
            bytesDownloaded = 0L,
            etaSeconds = null,
            expectedBytes = 157_286_400L,
        )

        // 4 - FAILED
        insertDownload(
            id = 4L,
            uid = "example-004",
            url = "https://example.com/watch?v=dQw4w9WgXcQ",
            status = "FAILED",
            filePath = null,
            errorMessage = "Network timeout",
            enqueuedAt = now - 40 * 60 * 1000,
            startedAt = now - 39 * 60 * 1000,
            completedAt = null,
        )
        insertMetadata(
            downloadId = 4L,
            title = "WorkManager Deep Dive - Retries, Constraints, Backoff",
            thumbPath = null,
        )
        insertProgress(
            downloadId = 4L,
            updatedAt = now - 38 * 60 * 1000,
            percent = 17.0f,
            bytesDownloaded = 35_000_000L,
            etaSeconds = null,
            expectedBytes = 204_800_000L,
        )

        // 5 - PAUSED
        insertDownload(
            id = 5L,
            uid = "example-005",
            url = "https://example.com/watch?v=dQw4w9WgXcQ",
            status = "STOPPED",
            filePath = "downloads/example.mp4",
            errorMessage = null,
            enqueuedAt = now - 90 * 60 * 1000,
            startedAt = now - 85 * 60 * 1000,
            completedAt = null,
        )
        insertMetadata(
            downloadId = 5L,
            title = "Compose Performance: Rethinking State and Recomposition",
            thumbPath = null,
        )
        insertProgress(
            downloadId = 5L,
            updatedAt = now - 3 * 60 * 1000,
            percent = 37.0f,
            bytesDownloaded = 77_000_000L,
            etaSeconds = null,
            expectedBytes = 208_000_000L,
        )

        // 6 - WAITING_FOR_WIFI
        insertDownload(
            id = 6L,
            uid = "example-006",
            url = "https://example.com/watch?v=dQw4w9WgXcQ",
            status = "WAITING_FOR_WIFI",
            filePath = null,
            errorMessage = null,
            enqueuedAt = now - 15 * 60 * 1000,
            startedAt = null,
            completedAt = null,
        )
        insertMetadata(
            downloadId = 6L,
            title = "Coroutines In 10 Minutes - Structured Concurrency",
            thumbPath = null,
        )
        insertProgress(
            downloadId = 6L,
            updatedAt = now - 15 * 60 * 1000,
            percent = 0f,
            bytesDownloaded = 0L,
            etaSeconds = null,
            expectedBytes = 104_857_600L,
        )

        // 7 - METADATA
        insertDownload(
            id = 7L,
            uid = "example-007",
            url = "https://example.com/watch?v=dQw4w9WgXcQ",
            status = "METADATA",
            filePath = null,
            errorMessage = null,
            enqueuedAt = now - 2 * 60 * 1000,
            startedAt = now - 90 * 1000,
            completedAt = null,
        )
        insertMetadata(
            downloadId = 7L,
            title = null,
            thumbPath = null,
        )
        insertProgress(
            downloadId = 7L,
            updatedAt = now - 60 * 1000,
            percent = 0f,
            bytesDownloaded = 0L,
            etaSeconds = null,
            expectedBytes = null,
        )

        // 8 - WAITING_FOR_NETWORK
        insertDownload(
            id = 8L,
            uid = "example-008",
            url = "https://example.com/watch?v=dQw4w9WgXcQ",
            status = "WAITING_FOR_NETWORK",
            filePath = null,
            errorMessage = null,
            enqueuedAt = now - 5 * 60 * 1000,
            startedAt = null,
            completedAt = null,
        )
        insertMetadata(
            downloadId = 8L,
            title = "Room + Paging 3 - Building Fast Lists",
            thumbPath = null,
        )
        insertProgress(
            downloadId = 8L,
            updatedAt = now - 5 * 60 * 1000,
            percent = 0f,
            bytesDownloaded = 0L,
            etaSeconds = null,
            expectedBytes = 157_286_400L,
        )
    }
}
