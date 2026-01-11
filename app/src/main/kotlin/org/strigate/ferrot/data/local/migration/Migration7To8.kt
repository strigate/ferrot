package org.strigate.ferrot.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE download_metadata
            ADD COLUMN videoId TEXT
            """.trimIndent()
        )
        db.execSQL(
            """
            ALTER TABLE download_metadata
            ADD COLUMN source TEXT
            """.trimIndent()
        )
        db.execSQL(
            """
            DROP INDEX IF EXISTS index_download_metadata_videoId
            """.trimIndent()
        )
        db.execSQL(
            """
            DROP INDEX IF EXISTS index_download_metadata_source_videoId
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_download_metadata_source_videoId
            ON download_metadata(source, videoId)
            """.trimIndent()
        )
    }
}
