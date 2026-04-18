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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.strigate.ferrot.BuildConfig
import org.strigate.ferrot.R
import org.strigate.ferrot.app.Constants
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.app.Constants.Work.Name.ONETIME_DOWNLOAD_AVAILABLE_UPDATE
import org.strigate.ferrot.app.Constants.Work.Name.PERIODIC_DOWNLOAD_AVAILABLE_UPDATE_FIRST
import org.strigate.ferrot.app.Constants.Work.Name.PERIODIC_DOWNLOAD_AVAILABLE_UPDATE_SECOND
import org.strigate.ferrot.app.ForegroundCoroutineWorker
import org.strigate.ferrot.app.NotificationService
import org.strigate.ferrot.app.actions.availableUpdateNotificationExtras
import org.strigate.ferrot.app.actions.buildDeleteAvailableUpdateNotificationAction
import org.strigate.ferrot.app.actions.buildInstallAvailableUpdateNotificationAction
import org.strigate.ferrot.app.provider.UpdatePathProvider
import org.strigate.ferrot.domain.usecase.AvailableUpdateUseCase
import org.strigate.ferrot.domain.usecase.StateUseCase
import org.strigate.ferrot.extensions.toast
import org.strigate.ferrot.util.calculateDailyInitialDelayMillis
import org.strigate.ferrot.util.isAppInForeground
import org.strigate.ferrot.util.setExpeditedIfAllowed
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

class DownloadAvailableUpdateWorker(
    private val appContext: Context,
    workerParameters: WorkerParameters,
    private val notificationService: NotificationService,
    private val stateUseCase: StateUseCase,
    private val updatePathProvider: UpdatePathProvider,
    private val availableUpdateUseCase: AvailableUpdateUseCase,
) : ForegroundCoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val tag = "DownloadAvailableUpdate:"
        try {
            val savedAvailableUpdate = runCatching {
                availableUpdateUseCase.getAvailableUpdateAsFlowUseCase().first()
            }.getOrNull()

            val currentTag = BuildConfig.VERSION_TAG
            val latestRelease = fetchLatestRelease()
            val latestTag = latestRelease.optString("tag_name")
            val isDraft = latestRelease.optBoolean("draft", false)
            val isPre = latestRelease.optBoolean("prerelease", false)
            Log.d(LOG_TAG, "$tag Latest release: tag=$latestTag draft=$isDraft prerelease=$isPre")

            if (latestTag.isBlank() || isDraft || isPre) {
                Log.d(LOG_TAG, "$tag No valid release to process")
                clearAvailableUpdate()
                return markCheckSuccess()
            }

            val savedTag = savedAvailableUpdate?.tag
            if (savedTag != null && isNewerVersion(savedTag, latestTag)) {
                val message = buildString {
                    append("$tag Saved update is newer than latest release: ")
                    append("saved=$savedTag latest=$latestTag")
                }
                Log.d(LOG_TAG, message)
                return markCheckSuccess()
            }
            if (!isNewerVersion(latestTag, currentTag)) {
                if (isAppInForeground()) {
                    appContext.toast(R.string.toast_no_updates_available, true)
                }
                Log.d(LOG_TAG, "$tag Already up to date: latest=$latestTag current=$currentTag")
                clearAvailableUpdate()
                return markCheckSuccess()
            }
            if (savedTag != null && isNewerVersion(latestTag, savedTag)) {
                val message = buildString {
                    append("$tag Found newer update: ")
                    append("latest=$latestTag replaces saved=$savedTag")
                }
                Log.d(LOG_TAG, message)
                clearAvailableUpdate()
            }

            val apkAsset = pickApkAsset(latestRelease.optJSONArray("assets")) ?: run {
                Log.w(LOG_TAG, "$tag No APK asset found for tag=$latestTag")
                clearAvailableUpdate()
                return markCheckSuccess()
            }
            val downloadUrl = apkAsset.optString("browser_download_url")
            if (downloadUrl.isBlank()) {
                Log.w(LOG_TAG, "$tag APK asset missing browser_download_url")
                clearAvailableUpdate()
                return markCheckSuccess()
            }

            val expectedSizeBytes = apkAsset.optLong("size", -1L).takeIf { it > 0L }
            val apkFile = updatePathProvider.apkFileFor(latestTag)
            val expectedDigest = apkAsset.optString("digest")

            if (apkFile.exists()) {
                when {
                    expectedDigest.startsWith("sha256:", true) -> {
                        if (validateSha256(apkFile, expectedDigest)) {
                            val message = buildString {
                                append("$tag Update already downloaded and verified: ")
                                append("file=${apkFile.name}")
                            }
                            Log.d(LOG_TAG, message)
                            saveAvailableUpdate(latestTag, apkFile.absolutePath)
                            notifyAvailableUpdate(latestTag, apkFile.absolutePath)
                            return markCheckSuccess()
                        }
                        Log.w(LOG_TAG, "$tag Existing file sha256 mismatch, re-downloading")
                        apkFile.delete()
                        clearAvailableUpdate()
                    }

                    apkFile.length() > 0L -> {
                        if (expectedSizeBytes != null && apkFile.length() != expectedSizeBytes) {
                            val message = buildString {
                                append("Existing file size mismatch. ")
                                append("expected=$expectedSizeBytes actual=${apkFile.length()}. ")
                                append("Re-downloading")
                            }
                            Log.w(LOG_TAG, "$tag $message")
                            apkFile.delete()
                            clearAvailableUpdate()
                        } else {
                            Log.d(LOG_TAG, "$tag Update file already present: file=${apkFile.name}")
                            saveAvailableUpdate(latestTag, apkFile.absolutePath)
                            notifyAvailableUpdate(latestTag, apkFile.absolutePath)
                            return markCheckSuccess()
                        }
                    }

                    else -> {
                        Log.w(LOG_TAG, "$tag Existing file is empty, re-downloading")
                        apkFile.delete()
                        clearAvailableUpdate()
                    }
                }
            }

            val partFile = File(apkFile.parentFile, apkFile.name + ".part")
            if (partFile.exists()) {
                partFile.delete()
            }
            Log.d(LOG_TAG, "$tag Downloading update to path=${partFile.absolutePath}")

            enableForeground(
                notificationText = appContext.getString(R.string.notification_text_downloading_app_update),
            )
            if (isAppInForeground()) {
                appContext.toast(R.string.toast_downloading_update, true)
            }

            try {
                downloadFile(
                    url = downloadUrl,
                    outFile = partFile,
                    expectedBytesFromRelease = expectedSizeBytes,
                )
                if (expectedDigest.startsWith("sha256:", true)) {
                    if (!validateSha256(partFile, expectedDigest)) {
                        partFile.delete()
                        clearAvailableUpdate()
                        return markCheckRetry()
                    }
                }
                if (apkFile.exists()) apkFile.delete()
                if (!partFile.renameTo(apkFile)) {
                    Log.w(LOG_TAG, "$tag Failed to rename part file to final output")
                    partFile.delete()
                    clearAvailableUpdate()
                    return markCheckRetry()
                }

                Log.d(LOG_TAG, "$tag Update download completed: file=${apkFile.name}")
                saveAvailableUpdate(latestTag, apkFile.absolutePath)
                notifyAvailableUpdate(latestTag, apkFile.absolutePath)
                return markCheckSuccess()
            } catch (throwable: Throwable) {
                Log.wtf(LOG_TAG, "$tag Download failed", throwable)
                partFile.delete()
                clearAvailableUpdate()
                return markCheckRetry()
            }
        } catch (throwable: Throwable) {
            Log.wtf(LOG_TAG, "DownloadAvailableUpdate: Update check failed", throwable)
            clearAvailableUpdate()
            return markCheckRetry()
        }
    }

    private fun notifyAvailableUpdate(versionTag: String, apkFilePath: String) {
        val contentTitle = appContext.getString(R.string.notification_title_app_update)
        val contentText = appContext.getString(R.string.available_update_ready, versionTag)
        notificationService.notifyAvailableUpdate(
            contentTitle = contentTitle,
            contentText = contentText,
            extras = availableUpdateNotificationExtras(),
            actions = listOf(
                buildInstallAvailableUpdateNotificationAction(
                    context = appContext,
                    apkFilePath = apkFilePath,
                    versionTag = versionTag,
                ),
                buildDeleteAvailableUpdateNotificationAction(appContext),
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
                Log.w(LOG_TAG, "DownloadAvailableUpdate: $message: $responseCode: $errorMessage")
                throw IOException("$message: $responseCode")
            }
            httpUrlConnection.inputStream.bufferedReader().use { bufferedReader ->
                JSONObject(bufferedReader.readText())
            }
        } finally {
            httpUrlConnection?.disconnect()
        }
    }

    private suspend fun downloadFile(
        url: String,
        outFile: File,
        expectedBytesFromRelease: Long?,
    ) = withContext(Dispatchers.IO) {
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
                Log.w(LOG_TAG, "DownloadAvailableUpdate: $message: $responseCode: $errorMessage")
                throw IOException("$message: $responseCode")
            }
            val totalBytes = expectedBytesFromRelease ?: httpUrlConnection
                .contentLengthLong
                .takeIf { it > 0L }

            var downloadedBytes = 0L
            var lastPublishedAtMillis = 0L

            fun publishProgress(force: Boolean) {
                val now = System.currentTimeMillis()
                val progressPercent = if (totalBytes != null && totalBytes > 0L) {
                    ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                } else {
                    -1
                }
                val shouldPublish = force || (now - lastPublishedAtMillis >= 1_000L)
                if (!shouldPublish) {
                    return
                }
                val contentText = if (totalBytes != null && totalBytes > 0L) {
                    buildString {
                        progressPercent
                            .takeIf { it >= 0 }
                            ?.let { append("$it% - ") }

                        append("${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}")
                    }
                } else {
                    formatBytes(downloadedBytes)
                }
                updateForeground(
                    notificationText = appContext.getString(R.string.notification_text_downloading_app_update),
                    progress = progressPercent.takeIf { it >= 0 },
                    indeterminate = progressPercent < 0,
                    contentText = contentText,
                )
                lastPublishedAtMillis = now
            }

            publishProgress(force = true)
            httpUrlConnection.inputStream.use { inputStream ->
                FileOutputStream(outFile).use { fileOutputStream ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = inputStream.read(buffer)
                        if (read == -1) break
                        fileOutputStream.write(buffer, 0, read)
                        downloadedBytes += read
                        val now = System.currentTimeMillis()
                        if (now - lastPublishedAtMillis >= 1_000L) {
                            publishProgress(force = false)
                        }
                    }
                    fileOutputStream.flush()
                }
            }
            if (totalBytes != null && downloadedBytes != totalBytes) {
                val message = buildString {
                    append("Downloaded bytes mismatch. ")
                    append("expected=$totalBytes actual=$downloadedBytes")
                }
                Log.w(LOG_TAG, "DownloadAvailableUpdate: $message")
                throw IOException("Downloaded bytes mismatch")
            }
            publishProgress(force = true)
        } finally {
            httpUrlConnection?.disconnect()
        }
    }

    private fun formatBytes(byteCount: Long): String {
        if (byteCount <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = byteCount.toDouble()
        var index = 0
        while (size >= 1024 && index < units.lastIndex) {
            size /= 1024.0
            index++
        }
        return String.format(Locale.getDefault(), "%.1f %s", size, units[index])
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
            val message = buildString {
                append("DownloadAvailableUpdate: sha256 mismatch: ")
                append("expected=$expected actual=$actual")
            }
            Log.w(LOG_TAG, message)
        }
        return ok
    }

    private suspend fun saveAvailableUpdate(tag: String, path: String?) {
        try {
            availableUpdateUseCase.saveAvailableUpdateUseCase(tag, path)
        } catch (throwable: Throwable) {
            val message = "DownloadAvailableUpdate: Failed to persist available update row"
            Log.w(LOG_TAG, message, throwable)
        }
    }

    private suspend fun clearAvailableUpdate() {
        try {
            availableUpdateUseCase.clearAvailableUpdateFilesAndDataUseCase()
        } catch (throwable: Throwable) {
            val message = "DownloadAvailableUpdate: Failed to clear available update data"
            Log.w(LOG_TAG, message, throwable)
        }
    }

    private suspend fun markCheckSuccess(): Result {
        return saveLastCheckAndReturn(Result.success())
    }

    private suspend fun markCheckRetry(): Result {
        return saveLastCheckAndReturn(Result.retry())
    }

    private suspend fun saveLastCheckAndReturn(result: Result): Result {
        runCatching {
            stateUseCase.saveLastAvailableUpdateCheckMillisUseCase(System.currentTimeMillis())
        }.onFailure {
            Log.w(LOG_TAG, "DownloadAvailableUpdate: Failed to save last check timestamp", it)
        }
        return result
    }

    companion object {
        fun enqueuePeriodicKeep(
            context: Context,
            flexHours: Long = 1,
        ) {
            enqueuePeriodicDailyUpdate(
                context = context,
                uniqueWorkName = PERIODIC_DOWNLOAD_AVAILABLE_UPDATE_FIRST,
                targetHour = 3,
                flexHours = flexHours,
            )
            enqueuePeriodicDailyUpdate(
                context = context,
                uniqueWorkName = PERIODIC_DOWNLOAD_AVAILABLE_UPDATE_SECOND,
                targetHour = 12,
                flexHours = flexHours,
            )
        }

        private fun enqueuePeriodicDailyUpdate(
            context: Context,
            uniqueWorkName: String,
            targetHour: Int,
            flexHours: Long,
        ) {
            val initialDelayMillis = calculateDailyInitialDelayMillis(targetHour)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
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
                uniqueWorkName,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWorkRequest,
            )
        }

        fun enqueueOneTimeReplace(
            context: Context,
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<DownloadAvailableUpdateWorker>()
                .setConstraints(constraints)
                .addTag(Constants.Work.Tag.DOWNLOAD_AVAILABLE_UPDATE)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .setExpeditedIfAllowed()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONETIME_DOWNLOAD_AVAILABLE_UPDATE,
                ExistingWorkPolicy.REPLACE,
                oneTimeWorkRequest,
            )
        }

        fun cancelPeriodic(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(PERIODIC_DOWNLOAD_AVAILABLE_UPDATE_FIRST)
            WorkManager.getInstance(context)
                .cancelUniqueWork(PERIODIC_DOWNLOAD_AVAILABLE_UPDATE_SECOND)
        }
    }
}
