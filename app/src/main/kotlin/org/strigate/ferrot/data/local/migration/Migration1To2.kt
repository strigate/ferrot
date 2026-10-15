package org.strigate.ferrot.data.local.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.executeSQL

val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.executeSQL("PRAGMA foreign_keys=OFF")
        connection.executeSQL(
            """
            CREATE TABLE IF NOT EXISTS download_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                uid TEXT NOT NULL,
                url TEXT NOT NULL,
                status TEXT NOT NULL,
                errorMessage TEXT,
                enqueuedAtMillis INTEGER NOT NULL,
                startedAtMillis INTEGER,
                completedAtMillis INTEGER
            )
            """.trimIndent()
        )
        connection.executeSQL(
            """
            INSERT INTO download_new (
                id, uid, url, status, errorMessage, enqueuedAtMillis, startedAtMillis, completedAtMillis
            )
            SELECT
                id, uid, url, status, errorMessage, enqueuedAtMillis, startedAtMillis, completedAtMillis
            FROM download
            """.trimIndent()
        )
        connection.executeSQL(
            """
            CREATE TABLE IF NOT EXISTS download_video (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                downloadId INTEGER NOT NULL,
                filePath TEXT NOT NULL,
                FOREIGN KEY(downloadId) REFERENCES download(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.executeSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_download_video_downloadId ON download_video(downloadId)")
        connection.executeSQL(
            """
            CREATE TABLE IF NOT EXISTS download_audio (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                downloadId INTEGER NOT NULL,
                filePath TEXT NOT NULL,
                FOREIGN KEY(downloadId) REFERENCES download(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.executeSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_download_audio_downloadId ON download_audio(downloadId)")
        connection.executeSQL(
            """
            INSERT INTO download_video (downloadId, filePath)
            SELECT id, filePath
            FROM download
            WHERE filePath IS NOT NULL AND TRIM(filePath) <> ''
            """.trimIndent()
        )
        connection.executeSQL("DROP TABLE download")
        connection.executeSQL("ALTER TABLE download_new RENAME TO download")

        connection.executeSQL("CREATE INDEX IF NOT EXISTS index_download_status ON download(status)")
        connection.executeSQL("CREATE INDEX IF NOT EXISTS index_download_enqueuedAtMillis ON download(enqueuedAtMillis)")
        connection.executeSQL("CREATE INDEX IF NOT EXISTS index_download_uid ON download(uid)")
        connection.executeSQL("PRAGMA foreign_keys=ON")
    }
}
