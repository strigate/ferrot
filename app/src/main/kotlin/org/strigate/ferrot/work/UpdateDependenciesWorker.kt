package org.strigate.ferrot.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yausername.youtubedl_android.YoutubeDL
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.strigate.ferrot.R
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.Constants.Work.Name.UPDATE_DEPENDENCIES
import org.strigate.ferrot.app.ForegroundCoroutineWorker
import java.util.concurrent.TimeUnit

@HiltWorker
class UpdateDependenciesWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParameters: WorkerParameters,
) : ForegroundCoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        enableForeground(
            notificationText = appContext.getString(R.string.worker_notification_text_updating_dependencies),
        )
        return try {
            Log.d(LOG_TAG, "Updating YoutubeDL")
            val status = YoutubeDL.getInstance().updateYoutubeDL(
                updateChannel = YoutubeDL.UpdateChannel.STABLE,
                appContext = appContext,
            )
            Log.d(LOG_TAG, "YoutubeDL update completed: status=$status")
            Result.success()
        } catch (throwable: Throwable) {
            Log.wtf(LOG_TAG, "An error occurred while updating dependencies", throwable)
            Result.failure()
        }
    }

    companion object {
        fun enqueuePeriodic(
            context: Context,
            repeatIntervalDays: Long = 7,
            flexHours: Long = 12,
            requireUnmetered: Boolean = false,
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (requireUnmetered) {
                        NetworkType.UNMETERED
                    } else {
                        NetworkType.CONNECTED
                    },
                )
                .build()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<UpdateDependenciesWorker>(
                repeatIntervalDays, TimeUnit.DAYS,
                flexHours, TimeUnit.HOURS,
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UPDATE_DEPENDENCIES,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWorkRequest,
            )
        }
    }
}
