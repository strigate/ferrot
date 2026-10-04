package org.strigate.ferrot.data.local.migration

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.async.executeSQL

val MIGRATION_10_11 = object : Migration(10, 11) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.executeSQL(
            """
            CREATE TABLE IF NOT EXISTS cookie_set (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                source TEXT NOT NULL,
                cookieFilePath TEXT NOT NULL,
                userAgent TEXT,
                createdAtMillis INTEGER NOT NULL,
                updatedAtMillis INTEGER NOT NULL,
                lastUsedAtMillis INTEGER
            )
            """.trimIndent()
        )
        connection.executeSQL(
            """
            CREATE INDEX IF NOT EXISTS index_cookie_set_updatedAtMillis
            ON cookie_set(updatedAtMillis)
            """.trimIndent()
        )
        connection.executeSQL(
            """
            CREATE TABLE IF NOT EXISTS cookie_set_domain (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                cookieSetId INTEGER NOT NULL,
                domain TEXT NOT NULL,
                includeSubdomains INTEGER NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                FOREIGN KEY(cookieSetId) REFERENCES cookie_set(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.executeSQL(
            """
            CREATE INDEX IF NOT EXISTS index_cookie_set_domain_cookieSetId
            ON cookie_set_domain(cookieSetId)
            """.trimIndent()
        )
        connection.executeSQL(
            """
            CREATE INDEX IF NOT EXISTS index_cookie_set_domain_domain
            ON cookie_set_domain(domain)
            """.trimIndent()
        )
        connection.executeSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_cookie_set_domain_cookieSetId_domain
            ON cookie_set_domain(cookieSetId, domain)
            """.trimIndent()
        )
    }
}
