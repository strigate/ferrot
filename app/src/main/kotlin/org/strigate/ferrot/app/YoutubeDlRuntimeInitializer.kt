package org.strigate.ferrot.app

import android.content.Context
import android.util.Log
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.di.module.IoDispatcher
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeDlRuntimeInitializer @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val initialized = AtomicBoolean(false)
    private val initializeMutex = Mutex()

    suspend fun initializeIfNeeded() {
        if (initialized.get()) {
            return
        }
        initializeMutex.withLock {
            if (initialized.get()) {
                return
            }
            withContext(ioDispatcher) {
                YoutubeDL.getInstance().init(appContext)
                FFmpeg.getInstance().init(appContext)
                Aria2c.getInstance().init(appContext)
            }
            initialized.set(true)
            Log.d(LOG_TAG, "Initialized YoutubeDL runtime")
        }
    }
}
