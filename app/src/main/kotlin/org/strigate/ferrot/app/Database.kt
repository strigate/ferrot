package org.strigate.ferrot.app

import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.strigate.ferrot.data.local.dao.AvailableUpdateDao
import org.strigate.ferrot.data.local.dao.DownloadAudioDao
import org.strigate.ferrot.data.local.dao.DownloadDao
import org.strigate.ferrot.data.local.dao.DownloadMetadataDao
import org.strigate.ferrot.data.local.dao.DownloadProgressDao
import org.strigate.ferrot.data.local.dao.DownloadVideoDao
import org.strigate.ferrot.data.local.dao.DownloadWithMetadataViewDao
import org.strigate.ferrot.data.local.entity.AvailableUpdateEntity
import org.strigate.ferrot.data.local.entity.DownloadAudioEntity
import org.strigate.ferrot.data.local.entity.DownloadEntity
import org.strigate.ferrot.data.local.entity.DownloadMetadataEntity
import org.strigate.ferrot.data.local.entity.DownloadProgressEntity
import org.strigate.ferrot.data.local.entity.DownloadVideoEntity
import org.strigate.ferrot.data.local.migration.MIGRATION_1_2
import org.strigate.ferrot.data.local.migration.MIGRATION_2_3
import org.strigate.ferrot.data.local.migration.MIGRATION_3_4
import org.strigate.ferrot.data.local.migration.MIGRATION_4_5
import org.strigate.ferrot.data.local.migration.MIGRATION_5_6
import org.strigate.ferrot.data.local.migration.MIGRATION_6_7
import org.strigate.ferrot.data.local.migration.MIGRATION_7_8
import org.strigate.ferrot.data.local.typeconverter.DownloadStatusTypeConverter
import org.strigate.ferrot.data.local.view.DownloadWithMetadataView

@androidx.room.Database(
    entities = [
        AvailableUpdateEntity::class,
        DownloadEntity::class,
        DownloadVideoEntity::class,
        DownloadAudioEntity::class,
        DownloadProgressEntity::class,
        DownloadMetadataEntity::class,
    ],
    views = [
        DownloadWithMetadataView::class,
    ],
    exportSchema = false,
    version = 8,
)
@TypeConverters(
    DownloadStatusTypeConverter::class,
)
abstract class Database : RoomDatabase() {
    abstract fun availableUpdateDao(): AvailableUpdateDao
    abstract fun downloadDao(): DownloadDao
    abstract fun downloadVideoDao(): DownloadVideoDao
    abstract fun downloadAudioDao(): DownloadAudioDao
    abstract fun downloadProgressDao(): DownloadProgressDao
    abstract fun downloadMetadataDao(): DownloadMetadataDao
    abstract fun downloadWithMetadataViewDao(): DownloadWithMetadataViewDao
}

internal fun <T : RoomDatabase> RoomDatabase.Builder<T>.applyMigrations(): RoomDatabase.Builder<T> {
    return addMigrations(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
    )
}
