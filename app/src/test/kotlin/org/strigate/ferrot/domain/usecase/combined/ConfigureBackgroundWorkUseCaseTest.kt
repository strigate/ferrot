package org.strigate.ferrot.domain.usecase.combined

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.app.integration.CookieFileStore
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.apply.ConfigureAutomaticAppUpdateWorkUseCase
import org.strigate.ferrot.domain.usecase.apply.ConfigureAutomaticDependencyUpdateWorkUseCase
import org.strigate.ferrot.domain.usecase.apply.ConfigureAutomaticDuplicateDownloadDeletionWorkUseCase
import org.strigate.ferrot.domain.usecase.cookieset.DeleteCookieSetsWithMissingFilesUseCase
import org.strigate.ferrot.domain.usecase.orphancleanup.EnqueueOrphanDownloadFilesCleanupUseCase
import org.strigate.ferrot.domain.usecase.settings.GetAutomaticAppUpdatesEnabledSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.GetAutomaticDependencyUpdatesEnabledSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.GetAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase
import org.strigate.ferrot.test.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigureBackgroundWorkUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val testDispatcher: TestDispatcher = mainDispatcherRule.testDispatcher
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var settingsUseCase: SettingsUseCase

    @Mock
    private lateinit var configureAutomaticAppUpdateWorkUseCase: ConfigureAutomaticAppUpdateWorkUseCase

    @Mock
    private lateinit var configureAutomaticDependencyUpdateWorkUseCase: ConfigureAutomaticDependencyUpdateWorkUseCase

    @Mock
    private lateinit var configureAutomaticDuplicateDownloadDeletionWorkUseCase: ConfigureAutomaticDuplicateDownloadDeletionWorkUseCase

    @Mock
    private lateinit var cookieFileStore: CookieFileStore

    @Mock
    private lateinit var deleteCookieSetsWithMissingFilesUseCase: DeleteCookieSetsWithMissingFilesUseCase

    @Mock
    private lateinit var enqueueOrphanDownloadFilesCleanupUseCase: EnqueueOrphanDownloadFilesCleanupUseCase

    @Mock
    private lateinit var getAutomaticAppUpdatesEnabledSettingAsFlowUseCase: GetAutomaticAppUpdatesEnabledSettingAsFlowUseCase

    @Mock
    private lateinit var getAutomaticDependencyUpdatesEnabledSettingAsFlowUseCase: GetAutomaticDependencyUpdatesEnabledSettingAsFlowUseCase

    @Mock
    private lateinit var getAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase: GetAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun invoke_appliesAllStartupWorkSettings() = runTest(testDispatcher) {
        val automaticAppUpdatesEnabledFlow = MutableStateFlow(true)
        val automaticDependencyUpdatesEnabledFlow = MutableStateFlow(false)
        val automaticDuplicateDownloadDeletionEnabledFlow = MutableStateFlow(true)
        createUseCase(
            automaticAppUpdatesEnabledFlow = automaticAppUpdatesEnabledFlow,
            automaticDependencyUpdatesEnabledFlow = automaticDependencyUpdatesEnabledFlow,
            automaticDuplicateDownloadDeletionEnabledFlow = automaticDuplicateDownloadDeletionEnabledFlow,
        ).invoke()

        verify(configureAutomaticAppUpdateWorkUseCase)
            .invoke(true)
        verify(configureAutomaticDependencyUpdateWorkUseCase)
            .invoke(false)
        verify(configureAutomaticDuplicateDownloadDeletionWorkUseCase)
            .invoke(true)
        verify(cookieFileStore)
            .deleteStaleTempCookies()
        verify(deleteCookieSetsWithMissingFilesUseCase)
            .invoke()
        verify(enqueueOrphanDownloadFilesCleanupUseCase)
            .invoke()
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private fun createUseCase(
        automaticAppUpdatesEnabledFlow: MutableStateFlow<Boolean>,
        automaticDependencyUpdatesEnabledFlow: MutableStateFlow<Boolean>,
        automaticDuplicateDownloadDeletionEnabledFlow: MutableStateFlow<Boolean>,
    ): ConfigureBackgroundWorkUseCase {
        `when`(getAutomaticAppUpdatesEnabledSettingAsFlowUseCase.invoke())
            .thenReturn(automaticAppUpdatesEnabledFlow)
        `when`(getAutomaticDependencyUpdatesEnabledSettingAsFlowUseCase.invoke())
            .thenReturn(automaticDependencyUpdatesEnabledFlow)
        `when`(getAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase.invoke())
            .thenReturn(automaticDuplicateDownloadDeletionEnabledFlow)
        `when`(settingsUseCase.getAutomaticAppUpdatesEnabledSettingAsFlowUseCase)
            .thenReturn(getAutomaticAppUpdatesEnabledSettingAsFlowUseCase)
        `when`(settingsUseCase.getAutomaticDependencyUpdatesEnabledSettingAsFlowUseCase)
            .thenReturn(getAutomaticDependencyUpdatesEnabledSettingAsFlowUseCase)
        `when`(settingsUseCase.getAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase)
            .thenReturn(getAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase)

        return ConfigureBackgroundWorkUseCase(
            settingsUseCase = settingsUseCase,
            configureAutomaticAppUpdateWorkUseCase = configureAutomaticAppUpdateWorkUseCase,
            configureAutomaticDependencyUpdateWorkUseCase = configureAutomaticDependencyUpdateWorkUseCase,
            configureAutomaticDuplicateDownloadDeletionWorkUseCase = configureAutomaticDuplicateDownloadDeletionWorkUseCase,
            cookieFileStore = cookieFileStore,
            deleteCookieSetsWithMissingFilesUseCase = deleteCookieSetsWithMissingFilesUseCase,
            enqueueOrphanDownloadFilesCleanupUseCase = enqueueOrphanDownloadFilesCleanupUseCase,
        )
    }
}
