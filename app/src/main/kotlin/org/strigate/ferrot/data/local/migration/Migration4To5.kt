package org.strigate.ferrot.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP VIEW IF EXISTS `downloads_with_metadata_view`")
        db.execSQL(
            "CREATE VIEW `downloads_with_metadata_view` AS SELECT\n" +
                    "        download.id AS id,\n" +
                    "        download.url AS url,\n" +
                    "        download.status AS status,\n" +
                    "        COALESCE(download_metadata.title, download.url) AS resolvedTitle,\n" +
                    "        download_metadata.thumbnailFilePath AS thumbnailFilePath,\n" +
                    "        COALESCE(download_progress.progressPercent, 0) AS progressPercent,\n" +
                    "        download_progress.etaSeconds AS etaSeconds,\n" +
                    "        download_progress.bytesDownloaded AS bytesDownloaded,\n" +
                    "        download_progress.expectedBytes AS expectedBytes,\n" +
                    "        download.enqueuedAtMillis AS enqueuedAtMillis,\n" +
                    "        download.startedAtMillis AS startedAtMillis,\n" +
                    "        download.completedAtMillis AS completedAtMillis\n" +
                    "    FROM download\n" +
                    "    LEFT JOIN download_metadata ON download_metadata.downloadId = download.id\n" +
                    "    LEFT JOIN download_progress ON download_progress.downloadId = download.id"
        )
    }
}
