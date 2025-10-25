package org.strigate.ferrot.app.di.module

import android.app.Application
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailabilityLight
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.strigate.ferrot.BuildConfig
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.analytics.NoOpAnalyticsLogger
import org.strigate.ferrot.analytics.firebase.FirebaseAnalyticsLogger
import org.strigate.ferrot.app.Constants.LOG_TAG
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    @Provides
    @Singleton
    fun providePlayServicesAvailable(app: Application): Boolean {
        return try {
            GoogleApiAvailabilityLight
                .getInstance()
                .isGooglePlayServicesAvailable(app) == ConnectionResult.SUCCESS
        } catch (_: Throwable) {
            false
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseApp(application: Application): FirebaseApp? {
        val firebaseAppId = BuildConfig.FIREBASE_APP_ID
        val firebaseApiKey = BuildConfig.FIREBASE_API_KEY
        val firebaseProjectId = BuildConfig.FIREBASE_PROJECT_ID
        val firebaseSenderId = BuildConfig.FIREBASE_SENDER_ID
        val firebaseBucket = BuildConfig.FIREBASE_BUCKET

        FirebaseApp.getApps(application).firstOrNull()?.let { return it }
        if (firebaseAppId.isBlank() || firebaseApiKey.isBlank() || firebaseProjectId.isBlank()) {
            return null
        }
        return try {
            val options = FirebaseOptions.Builder()
                .setApplicationId(firebaseAppId)
                .setApiKey(firebaseApiKey)
                .setProjectId(firebaseProjectId)
                .apply {
                    if (firebaseSenderId.isNotBlank()) {
                        setGcmSenderId(firebaseSenderId)
                    }
                    if (firebaseBucket.isNotBlank()) {
                        setStorageBucket(firebaseBucket)
                    }
                }
                .build()
            FirebaseApp.initializeApp(application, options)
        } catch (_: Throwable) {
            null
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(
        application: Application,
        firebaseApp: FirebaseApp?,
    ): FirebaseAnalytics? {
        return try {
            val firebaseInitialized = (firebaseApp != null)
            Log.d(LOG_TAG, "Firebase initialized: $firebaseInitialized")
            FirebaseAnalytics.getInstance(application)
        } catch (_: Throwable) {
            null
        }
    }

    @Provides
    @Singleton
    fun provideAnalyticsLogger(
        firebaseAnalytics: FirebaseAnalytics?,
        playServicesAvailable: Boolean,
    ): AnalyticsLogger {
        return if (firebaseAnalytics != null && playServicesAvailable) {
            FirebaseAnalyticsLogger(firebaseAnalytics)
        } else {
            NoOpAnalyticsLogger()
        }
    }
}
