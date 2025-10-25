package org.strigate.ferrot.helper

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.strigate.ferrot.R
import java.io.File

object InstallHelper {
    fun requestInstallApkIfExists(context: Context, filePath: String?): Boolean {
        if (filePath.isNullOrBlank()) {
            return false
        }
        val file = File(filePath)
        if (!file.exists() || file.length() <= 0L) {
            return false
        }
        val authority = "${context.packageName}.${context.getString(R.string.file_provider)}"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
