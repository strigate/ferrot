package org.strigate.ferrot.app.di.module

import android.content.Context
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.strigate.ferrot.app.Database
import org.strigate.ferrot.app.di.BootstrapCallbacks
import org.strigate.ferrot.data.local.dao.AvailableUpdateDao
import org.strigate.ferrot.data.local.dao.DownloadDao
import org.strigate.ferrot.data.local.dao.DownloadMetadataDao
import org.strigate.ferrot.data.local.dao.DownloadProgressDao
import org.strigate.ferrot.data.local.dao.DownloadWithMetadataViewDao
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        @BootstrapCallbacks bootstrapCallbacks: Set<@JvmSuppressWildcards RoomDatabase.Callback>,
    ): Database {
        return Database.getInstance(context, bootstrapCallbacks)
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
