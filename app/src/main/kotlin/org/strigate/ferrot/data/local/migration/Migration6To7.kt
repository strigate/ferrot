package org.strigate.ferrot.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE download_video
            ADD COLUMN sha256 TEXT
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_download_video_sha256
            ON download_video(sha256)
            """.trimIndent()
        )
    }
}
