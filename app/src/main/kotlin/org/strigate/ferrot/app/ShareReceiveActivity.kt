package org.strigate.ferrot.app

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import org.strigate.ferrot.app.Constants.Action.ACTION_START_DOWNLOAD_FROM_SHARE
import org.strigate.ferrot.app.Constants.Extras.EXTRA_ACTION
import org.strigate.ferrot.app.Constants.Extras.EXTRA_SHARED_URL
import org.strigate.ferrot.app.Constants.Extras.EXTRA_SHARED_URL_UID
import org.strigate.ferrot.presentation.MainActivity
import org.strigate.ferrot.util.UidUtil

@AndroidEntryPoint
class ShareReceiveActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val action = intent?.action
        val sharedUrl = when (action) {
            Intent.ACTION_SEND -> extractUrlFromActionSend(intent)
            Intent.ACTION_SEND_MULTIPLE -> extractUrlFromActionSendMultiple(intent)
            else -> null
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_ACTION, ACTION_START_DOWNLOAD_FROM_SHARE)
            putExtra(EXTRA_SHARED_URL_UID, UidUtil.generateUid())
            putExtra(EXTRA_SHARED_URL, sharedUrl)
        }
        startActivity(intent)
        finish()
    }

    private fun extractUrlFromActionSend(intent: Intent): String? {
        val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (!extraText.isNullOrEmpty()) {
            val urlFromText = findFirstHttpUrl(extraText)
            if (!urlFromText.isNullOrEmpty()) {
                return urlFromText
            }
        }
        val clip: ClipData? = intent.clipData
        if (clip != null) {
            val count = clip.itemCount
            for (index in 0 until count) {
                val item = clip.getItemAt(index)
                val itemText: CharSequence? = item.text
                if (itemText != null) {
                    val urlFromItemText: String? = findFirstHttpUrl(itemText.toString())
                    if (!urlFromItemText.isNullOrEmpty()) {
                        return urlFromItemText
                    }
                }
                val itemUri = item.uri
                if (itemUri != null) {
                    val uriString = itemUri.toString()
                    if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                        return uriString
                    }
                }
            }
        }
        val dataUri = intent.data
        if (dataUri != null) {
            val asString = dataUri.toString()
            if (asString.startsWith("http://") || asString.startsWith("https://")) {
                return asString
            }
        }
        return null
    }

    private fun extractUrlFromActionSendMultiple(intent: Intent): String? {
        val clip: ClipData? = intent.clipData
        if (clip != null) {
            val count = clip.itemCount
            for (index in 0 until count) {
                val item = clip.getItemAt(index)
                val itemText: CharSequence? = item.text
                if (itemText != null) {
                    val urlFromItemText: String? = findFirstHttpUrl(itemText.toString())
                    if (!urlFromItemText.isNullOrEmpty()) {
                        return urlFromItemText
                    }
                }
                val itemUri = item.uri
                if (itemUri != null) {
                    val uriString = itemUri.toString()
                    if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                        return uriString
                    }
                }
            }
        }
        val extraText: String? = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (!extraText.isNullOrEmpty()) {
            val urlFromText: String? = findFirstHttpUrl(extraText)
            if (!urlFromText.isNullOrEmpty()) {
                return urlFromText
            }
        }
        return null
    }

    companion object {
        private val URL_REGEX: Regex = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

        fun findFirstHttpUrl(text: String): String? {
            if (text.isEmpty()) return null
            val match = URL_REGEX.find(text)
            return match?.value
        }
    }
}
