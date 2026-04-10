package org.strigate.ferrot.app.actions

import org.junit.Assert.assertEquals
import org.junit.Test
import org.strigate.ferrot.app.Constants.Action.ACTION_NAVIGATE_DOWNLOAD
import org.strigate.ferrot.app.Constants.Extras.EXTRA_ACTION
import org.strigate.ferrot.app.Constants.Extras.EXTRA_DOWNLOAD_ID

class DownloadNotificationActionsTest {
    @Test
    fun downloadNotificationTag_formatsTag() {
        assertEquals("download:42", downloadNotificationTag(42L))
        assertEquals("active-download:42", activeDownloadNotificationTag(42L))
    }

    @Test
    fun downloadNotificationExtras_returnsNavigationPayload() {
        assertEquals(
            mapOf(
                EXTRA_ACTION to ACTION_NAVIGATE_DOWNLOAD,
                EXTRA_DOWNLOAD_ID to "7",
            ),
            downloadNotificationExtras(7L),
        )
    }
}
