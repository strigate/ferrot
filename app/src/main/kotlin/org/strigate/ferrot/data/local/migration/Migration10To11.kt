package org.strigate.ferrot.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
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
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_cookie_set_updatedAtMillis
            ON cookie_set(updatedAtMillis)
            """.trimIndent()
        )
        db.execSQL(
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
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_cookie_set_domain_cookieSetId
            ON cookie_set_domain(cookieSetId)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_cookie_set_domain_domain
            ON cookie_set_domain(domain)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_cookie_set_domain_cookieSetId_domain
            ON cookie_set_domain(cookieSetId, domain)
            """.trimIndent()
        )
        db.execSQL(
            """
            ALTER TABLE download
            ADD COLUMN cookieSetId INTEGER
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_download_cookieSetId
            ON download(cookieSetId)
            """.trimIndent()
        )
    }
}
