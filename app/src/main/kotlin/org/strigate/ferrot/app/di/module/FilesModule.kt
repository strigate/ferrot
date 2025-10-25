package org.strigate.ferrot.app.di.module

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.strigate.ferrot.app.provider.DownloadPathProvider
import org.strigate.ferrot.app.provider.DownloadPathProviderImpl
import org.strigate.ferrot.app.provider.UpdatePathProvider
import org.strigate.ferrot.app.provider.UpdatePathProviderImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FilesModule {
    @Binds
    @Singleton
    abstract fun bindUpdatePathProvider(
        implementation: UpdatePathProviderImpl,
    ): UpdatePathProvider

    @Binds
    @Singleton
    abstract fun bindDownloadPathProvider(
        implementation: DownloadPathProviderImpl,
    ): DownloadPathProvider
}
