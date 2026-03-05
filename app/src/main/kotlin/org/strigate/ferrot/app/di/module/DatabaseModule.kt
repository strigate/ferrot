package org.strigate.ferrot.app.di.module

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.strigate.ferrot.app.Constants.Database.DATABASE_NAME
import org.strigate.ferrot.app.Database
import org.strigate.ferrot.app.applyMigrations
import org.strigate.ferrot.data.local.dao.AvailableUpdateDao
import org.strigate.ferrot.data.local.dao.DownloadAudioDao
import org.strigate.ferrot.data.local.dao.DownloadDao
import org.strigate.ferrot.data.local.dao.DownloadMetadataDao
import org.strigate.ferrot.data.local.dao.DownloadProgressDao
import org.strigate.ferrot.data.local.dao.DownloadVideoDao
import org.strigate.ferrot.data.local.dao.DownloadWithMetadataViewDao
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): Database {
        return Room
            .databaseBuilder(appContext, Database::class.java, DATABASE_NAME)
            .applyMigrations()
            .build()
    }

    @Provides
    @Singleton
    fun provideAvailableUpdateDao(database: Database): AvailableUpdateDao {
        return database.availableUpdateDao()
    }

    @Provides
    @Singleton
    fun provideDownloadDao(database: Database): DownloadDao {
        return database.downloadDao()
    }

    @Provides
    @Singleton
    fun provideDownloadVideoDao(database: Database): DownloadVideoDao {
        return database.downloadVideoDao()
    }

    @Provides
    @Singleton
    fun provideAudioDownloadDao(database: Database): DownloadAudioDao {
        return database.downloadAudioDao()
    }

    @Provides
    @Singleton
    fun provideDownloadProgressDao(database: Database): DownloadProgressDao {
        return database.downloadProgressDao()
    }

    @Provides
    @Singleton
    fun provideDownloadMetadataDao(database: Database): DownloadMetadataDao {
        return database.downloadMetadataDao()
    }

    @Provides
    @Singleton
    fun provideDownloadWithMetadataViewDao(database: Database): DownloadWithMetadataViewDao {
        return database.downloadWithMetadataViewDao()
    }
}
