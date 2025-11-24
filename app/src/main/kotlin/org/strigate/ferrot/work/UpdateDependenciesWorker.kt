package org.strigate.ferrot.work

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yausername.youtubedl_android.YoutubeDL
import org.strigate.ferrot.R
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.Constants.Work.Name.ONETIME_UPDATE_DEPENDENCIES
import org.strigate.ferrot.app.Constants.Work.Name.PERIODIC_UPDATE_DEPENDENCIES
import org.strigate.ferrot.app.ForegroundCoroutineWorker
import org.strigate.ferrot.domain.usecase.StateUseCase
import org.strigate.ferrot.extensions.toast
import java.time.Duration.between
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class UpdateDependenciesWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters,
    private val stateUseCase: StateUseCase,
) : ForegroundCoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        return try {
            enableForeground(
                notificationText = appContext.getString(R.string.worker_notification_text_updating_dependencies),
            )
            Log.d(LOG_TAG, "Updating YoutubeDL")
            val updateStatus = YoutubeDL.getInstance().updateYoutubeDL(
                updateChannel = YoutubeDL.UpdateChannel.STABLE,
                appContext = appContext,
            )
            Log.d(LOG_TAG, "YoutubeDL update completed: status=$updateStatus")

            when (updateStatus) {
                YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE ->
                    appContext.toast(appContext.getString(R.string.toast_dependencies_already_up_to_date))

                YoutubeDL.UpdateStatus.DONE ->
                    appContext.toast(appContext.getString(R.string.toast_dependencies_update_complete))

                else -> appContext.toast("$updateStatus")
            }
            markCheckSuccess()
        } catch (throwable: Throwable) {
            Log.wtf(LOG_TAG, "An error occurred while updating dependencies", throwable)
            markCheckFailure()
        }
    }

    private suspend fun markCheckSuccess(): Result {
        runCatching {
            stateUseCase.saveLastDependencyUpdateCheckMillisUseCase(System.currentTimeMillis())
        }.onFailure {
            Log.w(LOG_TAG, "Failed to save dependency update check timestamp", it)
        }
        return Result.success()
    }

    private suspend fun markCheckFailure(): Result {
        runCatching {
            stateUseCase.saveLastDependencyUpdateCheckMillisUseCase(System.currentTimeMillis())
        }.onFailure {
            Log.w(LOG_TAG, "Failed to save dependency update check timestamp", it)
        }
        return Result.failure()
    }

    companion object {
        fun enqueuePeriodicKeep(
            context: Context,
            repeatIntervalDays: Long = 3,
            targetHour: Int = 4,
            flexHours: Long = 2,
        ) {
            val defaultZoneId = ZoneId.systemDefault()
            val now = ZonedDateTime.now(defaultZoneId)
            val targetDateTime = now
                .withHour(targetHour)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

            val firstRun = if (now.isBefore(targetDateTime)) {
                targetDateTime
            } else {
                targetDateTime.plusDays(1)
            }
            val initialDelayMillis = between(now, firstRun).toMillis()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .setRequiresBatteryNotLow(false)
                .setRequiresStorageNotLow(true)
                .build()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<UpdateDependenciesWorker>(
                repeatInterval = repeatIntervalDays,
                repeatIntervalTimeUnit = TimeUnit.DAYS,
                flexTimeInterval = flexHours,
                flexTimeIntervalUnit = TimeUnit.HOURS,
            )
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_UPDATE_DEPENDENCIES,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest,
            )
        }

        fun enqueueOneTimeReplace(
            context: Context,
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresCharging(false)
                .setRequiresBatteryNotLow(false)
                .setRequiresStorageNotLow(true)
                .build()

            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<UpdateDependenciesWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONETIME_UPDATE_DEPENDENCIES,
                ExistingWorkPolicy.REPLACE,
                oneTimeWorkRequest,
            )
        }

        fun cancelPeriodic(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_UPDATE_DEPENDENCIES)
        }
    }
}
