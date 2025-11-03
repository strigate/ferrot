package org.strigate.ferrot.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.strigate.ferrot.BuildConfig
import org.strigate.ferrot.R
import org.strigate.ferrot.app.Constants
import org.strigate.ferrot.app.Constants.Action.ACTION_INSTALL_AVAILABLE_UPDATE
import org.strigate.ferrot.app.Constants.Extras.EXTRA_ACTION
import org.strigate.ferrot.app.Constants.Extras.EXTRA_AVAILABLE_UPDATE_APK_FILE_PATH
import org.strigate.ferrot.app.Constants.Extras.EXTRA_AVAILABLE_UPDATE_VERSION_TAG
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.Constants.Work.Name.ONETIME_DOWNLOAD_AVAILABLE_UPDATE
import org.strigate.ferrot.app.Constants.Work.Name.PERIODIC_DOWNLOAD_AVAILABLE_UPDATE
import org.strigate.ferrot.app.ForegroundCoroutineWorker
import org.strigate.ferrot.app.NotificationService
import org.strigate.ferrot.app.provider.UpdatePathProvider
import org.strigate.ferrot.domain.usecase.AvailableUpdateUseCase
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.time.Duration.between
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import java.util.concurrent.TimeUnit

@HiltWorker
class DownloadAvailableUpdateWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val updatePathProvider: UpdatePathProvider,
    private val notificationService: NotificationService,
    private val availableUpdateUseCase: AvailableUpdateUseCase,
) : ForegroundCoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        return try {
            val savedAvailableUpdate = runCatching {
                availableUpdateUseCase.getAvailableUpdateAsFlowUseCase().first()
            }.getOrNull()

            val currentTag = BuildConfig.VERSION_TAG
            val latestRelease = fetchLatestRelease()

            val latestTag = latestRelease.optString("tag_name")
            val isDraft = latestRelease.optBoolean("draft", false)
            val isPre = latestRelease.optBoolean("prerelease", false)
            Log.d(LOG_TAG, "Latest release: latestTag=$latestTag isDraft=$isDraft isPre=$isPre")

            if (latestTag.isBlank() || isDraft || isPre) {
                Log.d(LOG_TAG, "No valid release to check")
                clearAvailableUpdate()
                return Result.success()
            }

            val savedTag = savedAvailableUpdate?.tag
            if (savedTag != null && isNewerVersion(savedTag, latestTag)) {
                Log.d(LOG_TAG, "Saved update ($savedTag) is newer than latest ($latestTag)")
                return Result.success()
            }
            if (!isNewerVersion(latestTag, currentTag)) {
                Log.d(LOG_TAG, "Already up to date: latest=$latestTag current=$currentTag")
                clearAvailableUpdate()
                return Result.success()
            }
            if (savedTag != null && isNewerVersion(latestTag, savedTag)) {
                Log.d(LOG_TAG, "Found newer update: latest=$latestTag replaces saved=$savedTag")
                deleteIfExists(savedAvailableUpdate.localFilePath)
                clearAvailableUpdate()
            }

            val apkAsset = pickApkAsset(latestRelease.optJSONArray("assets")) ?: run {
                Log.w(LOG_TAG, "No APK asset found on latest release $latestTag")
                clearAvailableUpdate()
                return Result.success()
            }
            val downloadUrl = apkAsset.optString("browser_download_url")
            if (downloadUrl.isBlank()) {
                Log.w(LOG_TAG, "APK asset missing browser_download_url")
                clearAvailableUpdate()
                return Result.success()
            }

            val apkFile = updatePathProvider.apkFileFor(latestTag)
            val expectedDigest = apkAsset.optString("digest")

            if (apkFile.exists()) {
                when {
                    expectedDigest.startsWith("sha256:", true) -> {
                        if (validateSha256(apkFile, expectedDigest)) {
                            Log.d(LOG_TAG, "Update already downloaded & verified: ${apkFile.name}")
                            saveAvailableUpdate(latestTag, apkFile.absolutePath)
                            notifyAvailableUpdate(latestTag, apkFile.absolutePath)
                            return Result.success()
                        }
                        Log.w(LOG_TAG, "Existing file sha256 mismatch. Re-downloading")
                        apkFile.delete()
                        clearAvailableUpdate()
                    }

                    apkFile.length() > 0L -> {
                        Log.d(LOG_TAG, "Update file already present: ${apkFile.name}")
                        saveAvailableUpdate(latestTag, apkFile.absolutePath)
                        notifyAvailableUpdate(latestTag, apkFile.absolutePath)
                        return Result.success()
                    }

                    else -> {
                        Log.w(LOG_TAG, "Existing file empty. Re-downloading")
                        apkFile.delete()
                        clearAvailableUpdate()
                    }
                }
            }

            val partFile = File(apkFile.parentFile, apkFile.name + ".part")
            Log.d(LOG_TAG, "Downloading update to ${partFile.absolutePath}")

            enableForeground(
                notificationText = appContext.getString(R.string.worker_notification_text_downloading_app_update),
            )

            try {
                downloadFile(downloadUrl, partFile)
                if (expectedDigest.startsWith("sha256:", true)) {
                    if (!validateSha256(partFile, expectedDigest)) {
                        partFile.delete()
                        clearAvailableUpdate()
                        return Result.retry()
                    }
                }
                if (apkFile.exists()) apkFile.delete()
                if (!partFile.renameTo(apkFile)) {
                    Log.w(LOG_TAG, "Failed to rename part file to final output")
                    partFile.delete()
                    clearAvailableUpdate()
                    return Result.retry()
                }

                Log.d(LOG_TAG, "Update downloaded successfully: ${apkFile.name}")
                saveAvailableUpdate(latestTag, apkFile.absolutePath)
                notifyAvailableUpdate(latestTag, apkFile.absolutePath)
                Result.success()
            } catch (throwable: Throwable) {
                Log.wtf(LOG_TAG, "Download failed", throwable)
                partFile.delete()
                clearAvailableUpdate()
                Result.retry()
            }
        } catch (throwable: Throwable) {
            Log.wtf(LOG_TAG, "Update check failed", throwable)
            clearAvailableUpdate()
            Result.retry()
        }
    }

    private fun notifyAvailableUpdate(versionTag: String, apkFilePath: String) {
        val contentTitle = appContext.getString(R.string.notification_app_update_title)
        val contentText = appContext.getString(R.string.available_update_ready, versionTag)
        notificationService.notifyAvailableUpdate(
            contentTitle = contentTitle,
            contentText = contentText,
            extras = mapOf(
                EXTRA_ACTION to ACTION_INSTALL_AVAILABLE_UPDATE,
                EXTRA_AVAILABLE_UPDATE_APK_FILE_PATH to apkFilePath,
                EXTRA_AVAILABLE_UPDATE_VERSION_TAG to versionTag,
            ),
        )
    }

    private suspend fun fetchLatestRelease(): JSONObject = withContext(Dispatchers.IO) {
        val url = URL(appContext.getString(R.string.github_latest_release))
        var httpUrlConnection: HttpURLConnection? = null
        try {
            httpUrlConnection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "${Constants.NAME}/${BuildConfig.VERSION_TAG}")
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
            }
            val responseCode = httpUrlConnection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorMessage = runCatching {
                    httpUrlConnection.errorStream?.readBytes()?.decodeToString()?.take(300)
                }.getOrNull()

                val message = "Fetch latest failed with response code"
                Log.w(LOG_TAG, "$message: $responseCode: $errorMessage")
                throw IOException("$message: $responseCode")
            }
            httpUrlConnection.inputStream.bufferedReader().use { bufferedReader ->
                JSONObject(bufferedReader.readText())
            }
        } finally {
            httpUrlConnection?.disconnect()
        }
    }

    private suspend fun downloadFile(url: String, outFile: File) = withContext(Dispatchers.IO) {
        var httpUrlConnection: HttpURLConnection? = null
        try {
            httpUrlConnection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "*/*")
                setRequestProperty("User-Agent", "${Constants.NAME}/${BuildConfig.VERSION_TAG}")
                connectTimeout = 20000
                readTimeout = 60000
                instanceFollowRedirects = true
            }
            val responseCode = httpUrlConnection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorMessage = runCatching {
                    httpUrlConnection.errorStream?.readBytes()?.decodeToString()?.take(300)
                }.getOrNull()

                val message = "Download failed with response code"
                Log.w(LOG_TAG, "$message: $responseCode: $errorMessage")
                throw IOException("$message: $responseCode")
            }
            httpUrlConnection.inputStream.use { inputStream ->
                FileOutputStream(outFile).use { fileOutputStream ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = inputStream.read(buffer)
                        if (read == -1) break
                        fileOutputStream.write(buffer, 0, read)
                    }
                    fileOutputStream.flush()
                }
            }
        } finally {
            httpUrlConnection?.disconnect()
        }
    }

    private fun pickApkAsset(assets: JSONArray?): JSONObject? {
        if (assets == null || assets.length() == 0) return null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString("name").lowercase(Locale.ROOT)
            if (name == appContext.getString(R.string.github_release_apk_name)) return asset
        }
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val contentType = asset.optString("content_type").lowercase(Locale.ROOT)
            if (contentType.contains("android.package-archive")) return asset
        }
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString("name").lowercase(Locale.ROOT)
            if (name.endsWith(".apk")) return asset
        }
        return null
    }

    private fun isNewerVersion(latestVersionTag: String, currentVersionTag: String): Boolean {
        fun parseVersion(tag: String) = tag
            .removePrefix("v")
            .split(".")
            .map { it.toIntOrNull() ?: 0 }

        val latestParts = parseVersion(latestVersionTag)
        val currentParts = parseVersion(currentVersionTag)
        val maxParts = maxOf(latestParts.size, currentParts.size)

        for (index in 0 until maxParts) {
            val latestPart = latestParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }
        return false
    }

    private fun calculateSha256(file: File): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { fileInputStream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val bytesRead = fileInputStream.read(buffer)
                if (bytesRead == -1) break
                messageDigest.update(buffer, 0, bytesRead)
            }
        }
        return messageDigest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun validateSha256(file: File, digestString: String): Boolean {
        val expected = digestString.substringAfter(":", "").lowercase(Locale.ROOT)
        val actual = calculateSha256(file)
        val ok = expected.equals(actual, ignoreCase = true)
        if (!ok) {
            Log.w(LOG_TAG, "sha256 mismatch. expected=$expected actual=$actual")
        }
        return ok
    }

    private suspend fun saveAvailableUpdate(tag: String, path: String?) {
        try {
            availableUpdateUseCase.saveAvailableUpdateUseCase(tag, path)
        } catch (throwable: Throwable) {
            Log.w(LOG_TAG, "Failed to persist available update row", throwable)
        }
    }

    private suspend fun clearAvailableUpdate() {
        try {
            availableUpdateUseCase.clearAvailableUpdateUseCase()
        } catch (throwable: Throwable) {
            Log.w(LOG_TAG, "Failed to clear available update row", throwable)
        }
    }

    private fun deleteIfExists(path: String?) {
        runCatching {
            if (!path.isNullOrBlank()) {
                val file = File(path)
                if (file.exists()) file.delete()
            }
        }
    }

    companion object {
        fun enqueuePeriodicKeep(
            context: Context,
            targetHour: Int = 3,
            flexHours: Long = 1,
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

            val periodicWorkRequest = PeriodicWorkRequestBuilder<DownloadAvailableUpdateWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS,
                flexTimeInterval = flexHours,
                flexTimeIntervalUnit = TimeUnit.HOURS,
            )
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .addTag(Constants.Work.Tag.DOWNLOAD_AVAILABLE_UPDATE)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_DOWNLOAD_AVAILABLE_UPDATE,
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

            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<DownloadAvailableUpdateWorker>()
                .setConstraints(constraints)
                .addTag(Constants.Work.Tag.DOWNLOAD_AVAILABLE_UPDATE)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONETIME_DOWNLOAD_AVAILABLE_UPDATE,
                ExistingWorkPolicy.REPLACE,
                oneTimeWorkRequest,
            )
        }

        fun cancelPeriodic(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_DOWNLOAD_AVAILABLE_UPDATE)
        }
    }
}
