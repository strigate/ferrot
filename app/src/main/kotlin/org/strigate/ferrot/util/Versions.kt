package org.strigate.ferrot.util

import android.os.Build

fun isAtLeastS() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

fun isAtLeastTiramisu() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
