package org.strigate.ferrot.domain.usecase.combined

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.strigate.ferrot.app.integration.CookieFileStore
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.apply.ConfigureAutomaticAppUpdateWorkUseCase
import org.strigate.ferrot.domain.usecase.apply.ConfigureAutomaticDependencyUpdateWorkUseCase
import org.strigate.ferrot.domain.usecase.apply.ConfigureAutomaticDuplicateDownloadDeletionWorkUseCase
import org.strigate.ferrot.domain.usecase.cookieset.DeleteCookieSetsWithMissingFilesUseCase
import org.strigate.ferrot.domain.usecase.orphancleanup.EnqueueOrphanDownloadFilesCleanupUseCase
import javax.inject.Inject

class ConfigureBackgroundWorkUseCase @Inject constructor(
    private val settingsUseCase: SettingsUseCase,
    private val configureAutomaticAppUpdateWorkUseCase: ConfigureAutomaticAppUpdateWorkUseCase,
    private val configureAutomaticDependencyUpdateWorkUseCase: ConfigureAutomaticDependencyUpdateWorkUseCase,
    private val configureAutomaticDuplicateDownloadDeletionWorkUseCase: ConfigureAutomaticDuplicateDownloadDeletionWorkUseCase,
    private val cookieFileStore: CookieFileStore,
    private val deleteCookieSetsWithMissingFilesUseCase: DeleteCookieSetsWithMissingFilesUseCase,
    private val enqueueOrphanDownloadFilesCleanupUseCase: EnqueueOrphanDownloadFilesCleanupUseCase,
) {
    suspend operator fun invoke() {
        val settings = combine(
            settingsUseCase.getAutomaticAppUpdatesEnabledSettingAsFlowUseCase(),
            settingsUseCase.getAutomaticDependencyUpdatesEnabledSettingAsFlowUseCase(),
            settingsUseCase.getAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase(),
        ) { automaticAppUpdatesEnabled, automaticDependencyUpdatesEnabled, automaticDuplicateDownloadDeletionEnabled ->
            BackgroundWorkSettings(
                automaticAppUpdatesEnabled = automaticAppUpdatesEnabled,
                automaticDependencyUpdatesEnabled = automaticDependencyUpdatesEnabled,
                automaticDuplicateDownloadDeletionEnabled = automaticDuplicateDownloadDeletionEnabled,
            )
        }.first()

        configureAutomaticAppUpdateWorkUseCase(
            automaticAppUpdatesEnabled = settings.automaticAppUpdatesEnabled,
        )
        configureAutomaticDependencyUpdateWorkUseCase(
            automaticDependencyUpdatesEnabled = settings.automaticDependencyUpdatesEnabled,
        )
        configureAutomaticDuplicateDownloadDeletionWorkUseCase(
            automaticDuplicateDownloadDeletionEnabled = settings.automaticDuplicateDownloadDeletionEnabled,
        )
        cookieFileStore.deleteStaleTempCookies()
        deleteCookieSetsWithMissingFilesUseCase()
        enqueueOrphanDownloadFilesCleanupUseCase()
    }

    private data class BackgroundWorkSettings(
        val automaticAppUpdatesEnabled: Boolean,
        val automaticDependencyUpdatesEnabled: Boolean,
        val automaticDuplicateDownloadDeletionEnabled: Boolean,
    )
}
