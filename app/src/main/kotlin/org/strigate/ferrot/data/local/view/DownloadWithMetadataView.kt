package org.strigate.ferrot.data.local.view

import androidx.room.DatabaseView
import org.strigate.ferrot.data.local.entity.DownloadStatus

@DatabaseView(
    viewName = "downloads_with_metadata_view",
    value = """
    SELECT
        download.id AS id,
        download.url AS url,
        download.status AS status,
        COALESCE(download_metadata.title, download.url) AS resolvedTitle,
        COALESCE(download_progress.progressPercent, 0) AS progressPercent,
        download_progress.etaSeconds AS etaSeconds,
        download_progress.bytesDownloaded AS bytesDownloaded,
        download_progress.expectedBytes AS expectedBytes,
        download.enqueuedAtMillis AS enqueuedAtMillis,
        download.startedAtMillis AS startedAtMillis,
        download.completedAtMillis AS completedAtMillis
    FROM download
    LEFT JOIN download_metadata ON download_metadata.downloadId = download.id
    LEFT JOIN download_progress ON download_progress.downloadId = download.id
    """,
)
data class DownloadWithMetadataView(
    val id: Long,
    val url: String,
    val status: DownloadStatus,
    val resolvedTitle: String,
    val progressPercent: Float,
    val etaSeconds: Long?,
    val bytesDownloaded: Long,
    val expectedBytes: Long?,
    val enqueuedAtMillis: Long,
    val startedAtMillis: Long?,
    val completedAtMillis: Long?,
)
