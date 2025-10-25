package org.strigate.ferrot.app.di.module

import androidx.work.WorkerFactory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.strigate.ferrot.app.di.WorkerFactory as Factory

@InstallIn(SingletonComponent::class)
@Module
abstract class WorkerModule {
    @Binds
    @Singleton
    abstract fun bindWorkerFactory(
        workerFactory: Factory,
    ): WorkerFactory
}
