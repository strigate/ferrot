package org.strigate.ferrot.domain.usecase

import org.strigate.ferrot.domain.usecase.apply.ApplyAutomaticDuplicateDownloadDeletionSettingUseCase
import org.strigate.ferrot.domain.usecase.apply.ApplyWifiOnlyPolicyUseCase
import javax.inject.Inject

class ApplyUseCase @Inject constructor(
    val applyWifiOnlyPolicyUseCase: ApplyWifiOnlyPolicyUseCase,
    val applyAutomaticDuplicateDownloadDeletionSettingUseCase: ApplyAutomaticDuplicateDownloadDeletionSettingUseCase,
)
