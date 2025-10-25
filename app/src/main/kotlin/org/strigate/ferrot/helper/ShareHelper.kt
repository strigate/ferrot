package org.strigate.ferrot.helper

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.strigate.ferrot.R
import org.strigate.ferrot.extensions.guessMimeType
import java.io.File

object ShareHelper {
    fun shareFileIfExists(context: Context, filePath: String?): Boolean {
        if (filePath.isNullOrBlank()) {
            return false
        }
        val file = File(filePath)
        if (!file.exists() || file.length() <= 0L) {
            return false
        }
        val authority = "${context.packageName}.${context.getString(R.string.file_provider)}"
        val uri: Uri = FileProvider.getUriForFile(context, authority, file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = filePath.guessMimeType()
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(context.contentResolver, file.name, uri)
        }
        val chooser = Intent.createChooser(send, context.getString(R.string.share_to))
        if (context !is Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(chooser)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
