package org.strigate.ferrot.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys=OFF")
        db.execSQL(
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
        db.execSQL(
            """
            INSERT INTO download_new (
                id, uid, url, status, errorMessage, enqueuedAtMillis, startedAtMillis, completedAtMillis
            )
            SELECT
                id, uid, url, status, errorMessage, enqueuedAtMillis, startedAtMillis, completedAtMillis
            FROM download
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS download_video (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                downloadId INTEGER NOT NULL,
                filePath TEXT NOT NULL,
                FOREIGN KEY(downloadId) REFERENCES download(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_download_video_downloadId ON download_video(downloadId)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS download_audio (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                downloadId INTEGER NOT NULL,
                filePath TEXT NOT NULL,
                FOREIGN KEY(downloadId) REFERENCES download(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_download_audio_downloadId ON download_audio(downloadId)")
        db.execSQL(
            """
            INSERT INTO download_video (downloadId, filePath)
            SELECT id, filePath
            FROM download
            WHERE filePath IS NOT NULL AND TRIM(filePath) <> ''
            """.trimIndent()
        )
        db.execSQL("DROP TABLE download")
        db.execSQL("ALTER TABLE download_new RENAME TO download")

        db.execSQL("CREATE INDEX IF NOT EXISTS index_download_status ON download(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_download_enqueuedAtMillis ON download(enqueuedAtMillis)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_download_uid ON download(uid)")
        db.execSQL("PRAGMA foreign_keys=ON")
    }
}
