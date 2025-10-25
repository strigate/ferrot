package org.strigate.ferrot.app.di.module

import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import org.strigate.ferrot.app.di.BootstrapCallbacks
import org.strigate.ferrot.data.local.callback.BootstrapCallback
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BootstrapModule {
    @Provides
    @Singleton
    @IntoSet
    @BootstrapCallbacks
    fun provideBootstrapCallback(): RoomDatabase.Callback {
        return BootstrapCallback()
    }
}
