package org.strigate.ferrot.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy

fun isAppInForeground(): Boolean {
    return ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
}

fun OneTimeWorkRequest.Builder.setExpeditedIfAllowed(): OneTimeWorkRequest.Builder {
    if (isAtLeastS() && isAppInForeground()) {
        setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
    }
    return this
}
