package org.strigate.ferrot.data.local.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.executeSQL

val MIGRATION_6_7 = object : Migration(6, 7) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.executeSQL(
            """
            ALTER TABLE download_video
            ADD COLUMN sha256 TEXT
            """.trimIndent()
        )
        connection.executeSQL(
            """
            CREATE INDEX IF NOT EXISTS index_download_video_sha256
            ON download_video(sha256)
            """.trimIndent()
        )
    }
}
