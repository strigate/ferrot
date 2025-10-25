package org.strigate.ferrot.helper

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.strigate.ferrot.R
import org.strigate.ferrot.extensions.guessMimeType
import java.io.File

object PlayHelper {
    fun playFileIfExists(context: Context, filePath: String?) {
        if (filePath.isNullOrBlank()) return
        val file = File(filePath)
        if (!file.exists() || file.length() <= 0L) return
        val authority = "${context.packageName}.${context.getString(R.string.file_provider)}"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, filePath.guessMimeType())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(context.contentResolver, file.name, uri)
        }
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
        }
    }
}
