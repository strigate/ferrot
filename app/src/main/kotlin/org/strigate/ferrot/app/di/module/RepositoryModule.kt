package org.strigate.ferrot.app.di.module

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.strigate.ferrot.data.repository.AvailableUpdateRepositoryImpl
import org.strigate.ferrot.data.repository.DownloadMetadataRepositoryImpl
import org.strigate.ferrot.data.repository.DownloadProgressRepositoryImpl
import org.strigate.ferrot.data.repository.DownloadRepositoryImpl
import org.strigate.ferrot.data.repository.DownloadWithMetadataRepositoryImpl
import org.strigate.ferrot.data.repository.SettingsRepositoryImpl
import org.strigate.ferrot.data.repository.StateRepositoryImpl
import org.strigate.ferrot.domain.repository.AvailableUpdateRepository
import org.strigate.ferrot.domain.repository.DownloadMetadataRepository
import org.strigate.ferrot.domain.repository.DownloadProgressRepository
import org.strigate.ferrot.domain.repository.DownloadRepository
import org.strigate.ferrot.domain.repository.DownloadWithMetadataRepository
import org.strigate.ferrot.domain.repository.SettingsRepository
import org.strigate.ferrot.domain.repository.StateRepository
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        implementation: SettingsRepositoryImpl,
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindAvailableUpdateRepository(
        implementation: AvailableUpdateRepositoryImpl,
    ): AvailableUpdateRepository

    @Binds
    @Singleton
    abstract fun bindStateRepository(
        implementation: StateRepositoryImpl,
    ): StateRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        implementation: DownloadRepositoryImpl,
    ): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindDownloadProgressRepository(
        implementation: DownloadProgressRepositoryImpl,
    ): DownloadProgressRepository

    @Binds
    @Singleton
    abstract fun bindDownloadMetadataRepository(
        implementation: DownloadMetadataRepositoryImpl,
    ): DownloadMetadataRepository

    @Binds
    abstract fun bindDownloadWithMetadataRepository(
        implementation: DownloadWithMetadataRepositoryImpl,
    ): DownloadWithMetadataRepository
}
