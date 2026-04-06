package org.strigate.ferrot.app.actions

import org.junit.Assert.assertEquals
import org.junit.Test
import org.strigate.ferrot.app.Constants.Action.ACTION_NAVIGATE_DOWNLOADS
import org.strigate.ferrot.app.Constants.Extras.EXTRA_ACTION

class AvailableUpdateNotificationActionsTest {
    @Test
    fun availableUpdateNotificationHelpers_returnExpectedStaticValues() {
        assertEquals("update_available", availableUpdateNotificationTag())
        assertEquals(
            mapOf(EXTRA_ACTION to ACTION_NAVIGATE_DOWNLOADS),
            availableUpdateNotificationExtras(),
        )
    }
}
