package org.strigate.ferrot.module

import android.content.Context
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import org.strigate.ferrot.app.di.BootstrapCallbacks
import org.strigate.ferrot.callback.MockDataBootstrapCallback
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

    @Provides
    @Singleton
    @IntoSet
    @BootstrapCallbacks
    fun provideMockDataBootstrapCallback(
        @ApplicationContext appContext: Context,
    ): RoomDatabase.Callback {
        return MockDataBootstrapCallback(appContext)
    }
}
