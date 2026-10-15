package org.strigate.ferrot.data.local.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.executeSQL

val MIGRATION_3_4 = object : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.executeSQL(
            """
            ALTER TABLE download_video
            ADD COLUMN fileExtension TEXT NOT NULL DEFAULT ''
            """.trimIndent(),
        )
        connection.executeSQL(
            """
            ALTER TABLE download_audio
            ADD COLUMN fileExtension TEXT NOT NULL DEFAULT ''
            """.trimIndent(),
        )
        connection.executeSQL(
            """
            ALTER TABLE download_metadata
            ADD COLUMN durationSeconds INTEGER
            """.trimIndent(),
        )
    }
}
