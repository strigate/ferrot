package org.strigate.ferrot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import org.strigate.ferrot.R
import org.strigate.ferrot.app.Constants.Extras.EXTRA_SHARE_FILE_PATH
import org.strigate.ferrot.extensions.toast
import org.strigate.ferrot.helper.ShareHelper

class ShareDownloadActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filePath = intent?.getStringExtra(EXTRA_SHARE_FILE_PATH)
        if (!ShareHelper.shareFileIfExists(this, filePath)) {
            toast(R.string.toast_share_failed, true)
        }
        finish()
    }
}
