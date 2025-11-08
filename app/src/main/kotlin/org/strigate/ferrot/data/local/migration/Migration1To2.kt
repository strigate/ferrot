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
                videoFilePath TEXT,
                audioFilePath TEXT,
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
                id, uid, url, status, videoFilePath, audioFilePath,
                errorMessage, enqueuedAtMillis, startedAtMillis, completedAtMillis
            )
            SELECT
                id, uid, url, status, filePath, NULL,
                errorMessage, enqueuedAtMillis, startedAtMillis, completedAtMillis
            FROM download
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
