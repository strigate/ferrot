package org.strigate.ferrot.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.Constants.Work.Name.ONETIME_UPDATE_DEPENDENCIES
import org.strigate.ferrot.domain.usecase.AvailableUpdateUseCase
import org.strigate.ferrot.work.RequeuePendingDownloadsWorker
import org.strigate.ferrot.work.UpdateDependenciesWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MyPackageReplacedReceiver : BroadcastReceiver() {
    @Inject
    lateinit var availableUpdateUseCase: AvailableUpdateUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                availableUpdateUseCase.clearAvailableUpdateFilesAndDataUseCase()

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true)
                    .build()

                val updateDependenciesOneTimeWorkRequest =
                    OneTimeWorkRequestBuilder<UpdateDependenciesWorker>()
                        .setConstraints(constraints)
                        .setBackoffCriteria(
                            BackoffPolicy.EXPONENTIAL,
                            30,
                            TimeUnit.SECONDS,
                        )
                        .build()
                val requeuePendingDownloadsOneTimeWorkRequest =
                    OneTimeWorkRequestBuilder<RequeuePendingDownloadsWorker>()
                        .build()

                WorkManager.getInstance(appContext)
                    .beginUniqueWork(
                        ONETIME_UPDATE_DEPENDENCIES,
                        ExistingWorkPolicy.REPLACE,
                        updateDependenciesOneTimeWorkRequest,
                    )
                    .then(requeuePendingDownloadsOneTimeWorkRequest)
                    .enqueue()

            } catch (throwable: Throwable) {
                Log.w(LOG_TAG, throwable)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
