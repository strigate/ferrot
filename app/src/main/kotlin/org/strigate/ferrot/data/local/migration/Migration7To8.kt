package org.strigate.ferrot.data.local.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.executeSQL

val MIGRATION_7_8 = object : Migration(7, 8) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.executeSQL(
            """
            ALTER TABLE download_metadata
            ADD COLUMN videoId TEXT
            """.trimIndent()
        )
        connection.executeSQL(
            """
            ALTER TABLE download_metadata
            ADD COLUMN source TEXT
            """.trimIndent()
        )
        connection.executeSQL(
            """
            DROP INDEX IF EXISTS index_download_metadata_videoId
            """.trimIndent()
        )
        connection.executeSQL(
            """
            DROP INDEX IF EXISTS index_download_metadata_source_videoId
            """.trimIndent()
        )
        connection.executeSQL(
            """
            CREATE INDEX IF NOT EXISTS index_download_metadata_source_videoId
            ON download_metadata(source, videoId)
            """.trimIndent()
        )
    }
}
