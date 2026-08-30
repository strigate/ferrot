package org.strigate.ferrot.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import org.strigate.ferrot.R
import org.strigate.ferrot.app.Constants.Action.ACTION_START_DOWNLOAD_FROM_SHARE
import org.strigate.ferrot.app.Constants.Extras.EXTRA_ACTION
import org.strigate.ferrot.app.Constants.Extras.EXTRA_SHARED_URL
import org.strigate.ferrot.app.Constants.Extras.EXTRA_SHARED_URL_UID
import org.strigate.ferrot.extensions.toast
import org.strigate.ferrot.presentation.MainActivity
import org.strigate.ferrot.util.UidUtil

@AndroidEntryPoint
class ShareReceiveActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleIntent(intent)
        } else {
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(receivedIntent: Intent?) {
        val sharedUrl = ShareIntentParser.extractUrl(receivedIntent)
        if (sharedUrl == null) {
            toast(R.string.toast_share_no_url, true)
            finish()
            return
        }
        val handoffIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(EXTRA_ACTION, ACTION_START_DOWNLOAD_FROM_SHARE)
            putExtra(EXTRA_SHARED_URL_UID, UidUtil.generateUid())
            putExtra(EXTRA_SHARED_URL, sharedUrl)
        }
        startActivity(handoffIntent)
        finish()
    }
}
