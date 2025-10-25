package org.strigate.ferrot.helper

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import org.strigate.ferrot.R
import org.strigate.ferrot.app.Constants
import org.strigate.ferrot.app.Constants.LOG_TAG
import org.strigate.ferrot.extensions.guessMimeType
import org.strigate.ferrot.extensions.toast
import java.io.File

object SaveHelper {
    fun saveToDownloads(context: Context, filePath: String?): Boolean {
        if (filePath.isNullOrBlank()) {
            return false
        }
        val sourceFile = File(filePath)
        if (!sourceFile.exists() || sourceFile.length() <= 0L) {
            return false
        }
        val relativePath = Environment.DIRECTORY_DOWNLOADS + "/${Constants.NAME}"
        val relativePathWithSlash = ensureTrailingSlash(relativePath)
        val relativePathWithNoSlash = relativePathWithSlash.removeSuffix("/")
        val outputMimeType = filePath.guessMimeType()
        val displayName = sourceFile.name

        val existsInDownloads = existsInDownloads(
            context = context,
            relativePathWithSlash = relativePathWithSlash,
            relativePathWithNoSlash = relativePathWithNoSlash,
            displayName = displayName,
        )
        if (existsInDownloads) {
            val message = context.getString(R.string.toast_file_exists, relativePathWithSlash)
            context.toast(message)
            return false
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, outputMimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePathWithSlash)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val contentResolver = context.contentResolver
        val uri = contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            values
        ) ?: return false

        return try {
            contentResolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { it.copyTo(out) }
            } ?: error("openOutputStream null")

            ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }.also {
                contentResolver.update(uri, it, null, null)
            }
            val message = context.getString(
                R.string.toast_saved_to,
                "$relativePathWithSlash$displayName",
            )
            context.toast(message)
            true
        } catch (throwable: Throwable) {
            runCatching {
                contentResolver.delete(uri, null, null)
            }
            Log.w(LOG_TAG, "Failed to save: ${throwable.message}", throwable)
            false
        }
    }

    private fun existsInDownloads(
        context: Context,
        relativePathWithSlash: String,
        relativePathWithNoSlash: String,
        displayName: String,
    ): Boolean {
        val contentResolver = context.contentResolver
        val relativePathColumn = MediaStore.MediaColumns.RELATIVE_PATH
        val displayNameColumn = MediaStore.MediaColumns.DISPLAY_NAME
        val selection = buildString {
            append("(")
            append("$relativePathColumn = ? OR $relativePathColumn = ?")
            append(") AND ")
            append("$displayNameColumn = ?")
        }
        val selectionArgs = arrayOf(relativePathWithSlash, relativePathWithNoSlash, displayName)
        return contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns._ID),
            selection,
            selectionArgs,
            null,
        ).use { cursor ->
            cursor != null && cursor.moveToFirst()
        }
    }

    private fun ensureTrailingSlash(path: String): String {
        return if (path.endsWith("/")) path else "$path/"
    }
}
