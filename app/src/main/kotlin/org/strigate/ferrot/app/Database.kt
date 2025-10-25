package org.strigate.ferrot.app

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.strigate.ferrot.app.Constants.Database.DATABASE_NAME
import org.strigate.ferrot.data.local.dao.AvailableUpdateDao
import org.strigate.ferrot.data.local.dao.DownloadDao
import org.strigate.ferrot.data.local.dao.DownloadMetadataDao
import org.strigate.ferrot.data.local.dao.DownloadProgressDao
import org.strigate.ferrot.data.local.dao.DownloadWithMetadataViewDao
import org.strigate.ferrot.data.local.entity.AvailableUpdateEntity
import org.strigate.ferrot.data.local.entity.DownloadEntity
import org.strigate.ferrot.data.local.entity.DownloadMetadataEntity
import org.strigate.ferrot.data.local.entity.DownloadProgressEntity
import org.strigate.ferrot.data.local.typeconverter.DownloadStatusTypeConverter
import org.strigate.ferrot.data.local.view.DownloadWithMetadataView

@androidx.room.Database(
    entities = [
        AvailableUpdateEntity::class,
        DownloadEntity::class,
        DownloadProgressEntity::class,
        DownloadMetadataEntity::class,
    ],
    views = [
        DownloadWithMetadataView::class,
    ],
    exportSchema = false,
    version = 1,
)
@TypeConverters(
    DownloadStatusTypeConverter::class,
)
abstract class Database : RoomDatabase() {
    abstract fun availableUpdateDao(): AvailableUpdateDao
    abstract fun downloadDao(): DownloadDao
    abstract fun downloadProgressDao(): DownloadProgressDao
    abstract fun downloadMetadataDao(): DownloadMetadataDao
    abstract fun downloadWithMetadataViewDao(): DownloadWithMetadataViewDao

    companion object {
        @Volatile
        private var instance: Database? = null

        fun getInstance(
            context: Context,
            bootstrapCallbacks: Set<Callback>,
        ): Database {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context, bootstrapCallbacks).also { instance = it }
            }
        }

        private fun buildDatabase(
            context: Context,
            bootstrapCallbacks: Set<Callback>,
        ): Database {
            val roomDatabaseBuilder = Room
                .databaseBuilder(context, Database::class.java, DATABASE_NAME)
                .applyMigrations()

            bootstrapCallbacks.forEach {
                roomDatabaseBuilder.addCallback(it)
            }
            return roomDatabaseBuilder.build()
        }
    }
}

private fun <T : RoomDatabase> RoomDatabase.Builder<T>.applyMigrations(): RoomDatabase.Builder<T> {
    return addMigrations(
//        MIGRATION_1_2,
    )
}
