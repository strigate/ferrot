package org.strigate.ferrot.app

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import org.strigate.ferrot.app.Constants.LOG_TAG

class Initializer : Initializer<Unit> {
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    override fun create(context: Context) {
        Log.d(LOG_TAG, "Initializing")
        YoutubeDL.getInstance().init(context)
        FFmpeg.getInstance().init(context)
        Aria2c.getInstance().init(context)
        Log.d(LOG_TAG, "Initialized")
    }
}
