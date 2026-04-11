package org.strigate.ferrot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import org.strigate.ferrot.app.Constants.Extras.EXTRA_SHARE_FILE_PATH
import org.strigate.ferrot.helper.ShareHelper

class ShareDownloadActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filePath = intent?.getStringExtra(EXTRA_SHARE_FILE_PATH)
        ShareHelper.shareFileIfExists(this, filePath)
        finish()
    }
}
