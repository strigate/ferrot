package org.strigate.ferrot.app.integration

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLResponse
import javax.inject.Inject

open class YoutubeDlClient @Inject constructor() {
    open fun execute(
        request: YoutubeDLRequest,
        processId: String,
        redirectErrorStream: Boolean = false,
        callback: ((Float, Long, String) -> Unit)? = null,
    ): YoutubeDLResponse {
        return YoutubeDL.getInstance().execute(
            request = request,
            processId = processId,
            redirectErrorStream = redirectErrorStream,
            callback = callback,
        )
    }

    open fun destroyProcessById(processId: String): Boolean {
        return YoutubeDL.getInstance().destroyProcessById(processId)
    }
}
