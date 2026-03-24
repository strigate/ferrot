package org.strigate.ferrot.util

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Test

class VersionsTest {
    @Test
    fun sdkChecks_matchPlatformComparisons() {
        assertEquals(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S, isAtLeastS())
        assertEquals(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU, isAtLeastTiramisu())
    }
}
