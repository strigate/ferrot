package org.strigate.ferrot.domain.usecase.combined

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.apply.ConfigureAutomaticAppUpdatesSettingUseCase
import org.strigate.ferrot.domain.usecase.apply.ConfigureAutomaticDependencyUpdatesSettingUseCase
import org.strigate.ferrot.domain.usecase.apply.ConfigureAutomaticDuplicateDownloadDeletionSettingUseCase
import org.strigate.ferrot.domain.usecase.orphancleanup.EnqueueOrphanDownloadFilesCleanupUseCase
import javax.inject.Inject

class ConfigureBackgroundWorkUseCase @Inject constructor(
    private val settingsUseCase: SettingsUseCase,
    private val configureAutomaticAppUpdatesSettingUseCase: ConfigureAutomaticAppUpdatesSettingUseCase,
    private val configureAutomaticDependencyUpdatesSettingUseCase: ConfigureAutomaticDependencyUpdatesSettingUseCase,
    private val configureAutomaticDuplicateDownloadDeletionSettingUseCase: ConfigureAutomaticDuplicateDownloadDeletionSettingUseCase,
    private val enqueueOrphanDownloadFilesCleanupUseCase: EnqueueOrphanDownloadFilesCleanupUseCase,
) {
    suspend operator fun invoke() {
        val settings = combine(
            settingsUseCase.getAutomaticUpdatesSettingAsFlowUseCase(),
            settingsUseCase.getAutomaticDependencyUpdatesSettingAsFlowUseCase(),
            settingsUseCase.getAutomaticDuplicateDownloadDeletionSettingAsFlowUseCase(),
        ) { automaticUpdates, automaticDependencyUpdates, automaticDuplicateDownloadDeletion ->
            BackgroundWorkSettings(
                automaticUpdates = automaticUpdates,
                automaticDependencyUpdates = automaticDependencyUpdates,
                automaticDuplicateDownloadDeletion = automaticDuplicateDownloadDeletion,
            )
        }.first()

        configureAutomaticAppUpdatesSettingUseCase(
            automaticAppUpdates = settings.automaticUpdates,
        )
        configureAutomaticDependencyUpdatesSettingUseCase(
            automaticDependencyUpdates = settings.automaticDependencyUpdates,
        )
        configureAutomaticDuplicateDownloadDeletionSettingUseCase(
            automaticDuplicateDownloadDeletion = settings.automaticDuplicateDownloadDeletion,
        )
        enqueueOrphanDownloadFilesCleanupUseCase()
    }

    private data class BackgroundWorkSettings(
        val automaticUpdates: Boolean,
        val automaticDependencyUpdates: Boolean,
        val automaticDuplicateDownloadDeletion: Boolean,
    )
}
