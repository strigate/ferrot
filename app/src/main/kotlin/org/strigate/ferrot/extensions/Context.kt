package org.strigate.ferrot.extensions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.strigate.ferrot.R

fun Context.toast(
    @StringRes textRes: Int,
    isLong: Boolean = false,
) = toast(getString(textRes), isLong)

fun Context.toast(
    text: String,
    isLong: Boolean = false,
) = CoroutineScope(Dispatchers.Main).launch {
    Toast.makeText(this@toast, text, if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

fun Context.copyToClipboard(text: String, label: String = "value") {
    val clipboardManager = ContextCompat.getSystemService(this, ClipboardManager::class.java)
    clipboardManager?.setPrimaryClip(ClipData.newPlainText(label, text))
    CoroutineScope(Dispatchers.Main).launch {
        toast(R.string.toast_copied_to_clipboard, true)
    }
}
