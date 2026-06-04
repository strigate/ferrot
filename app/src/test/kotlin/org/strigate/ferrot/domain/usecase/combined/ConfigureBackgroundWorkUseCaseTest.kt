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
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.apply.ConfigureAutomaticAppUpdatesSettingUseCase
import org.strigate.ferrot.domain.usecase.apply.ConfigureAutomaticDependencyUpdatesSettingUseCase
import org.strigate.ferrot.domain.usecase.apply.ConfigureAutomaticDuplicateDownloadDeletionSettingUseCase
import org.strigate.ferrot.domain.usecase.orphancleanup.EnqueueOrphanDownloadFilesCleanupUseCase
import org.strigate.ferrot.domain.usecase.settings.GetAutomaticDependencyUpdatesSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.GetAutomaticDuplicateDownloadDeletionSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.GetAutomaticUpdatesSettingAsFlowUseCase
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
    private lateinit var configureAutomaticAppUpdatesSettingUseCase: ConfigureAutomaticAppUpdatesSettingUseCase

    @Mock
    private lateinit var configureAutomaticDependencyUpdatesSettingUseCase: ConfigureAutomaticDependencyUpdatesSettingUseCase

    @Mock
    private lateinit var configureAutomaticDuplicateDownloadDeletionSettingUseCase: ConfigureAutomaticDuplicateDownloadDeletionSettingUseCase

    @Mock
    private lateinit var enqueueOrphanDownloadFilesCleanupUseCase: EnqueueOrphanDownloadFilesCleanupUseCase

    @Mock
    private lateinit var getAutomaticUpdatesSettingAsFlowUseCase: GetAutomaticUpdatesSettingAsFlowUseCase

    @Mock
    private lateinit var getAutomaticDependencyUpdatesSettingAsFlowUseCase: GetAutomaticDependencyUpdatesSettingAsFlowUseCase

    @Mock
    private lateinit var getAutomaticDuplicateDownloadDeletionSettingAsFlowUseCase: GetAutomaticDuplicateDownloadDeletionSettingAsFlowUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun invoke_appliesAllStartupWorkSettings() = runTest(testDispatcher) {
        val automaticUpdatesFlow = MutableStateFlow(true)
        val automaticDependencyUpdatesFlow = MutableStateFlow(false)
        val automaticDuplicateDownloadDeletionFlow = MutableStateFlow(true)
        createUseCase(
            automaticUpdatesFlow = automaticUpdatesFlow,
            automaticDependencyUpdatesFlow = automaticDependencyUpdatesFlow,
            automaticDuplicateDownloadDeletionFlow = automaticDuplicateDownloadDeletionFlow,
        ).invoke()

        verify(configureAutomaticAppUpdatesSettingUseCase)
            .invoke(true)
        verify(configureAutomaticDependencyUpdatesSettingUseCase)
            .invoke(false)
        verify(configureAutomaticDuplicateDownloadDeletionSettingUseCase)
            .invoke(true)
        verify(enqueueOrphanDownloadFilesCleanupUseCase)
            .invoke()
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private fun createUseCase(
        automaticUpdatesFlow: MutableStateFlow<Boolean>,
        automaticDependencyUpdatesFlow: MutableStateFlow<Boolean>,
        automaticDuplicateDownloadDeletionFlow: MutableStateFlow<Boolean>,
    ): ConfigureBackgroundWorkUseCase {
        `when`(getAutomaticUpdatesSettingAsFlowUseCase.invoke())
            .thenReturn(automaticUpdatesFlow)
        `when`(getAutomaticDependencyUpdatesSettingAsFlowUseCase.invoke())
            .thenReturn(automaticDependencyUpdatesFlow)
        `when`(getAutomaticDuplicateDownloadDeletionSettingAsFlowUseCase.invoke())
            .thenReturn(automaticDuplicateDownloadDeletionFlow)
        `when`(settingsUseCase.getAutomaticUpdatesSettingAsFlowUseCase)
            .thenReturn(getAutomaticUpdatesSettingAsFlowUseCase)
        `when`(settingsUseCase.getAutomaticDependencyUpdatesSettingAsFlowUseCase)
            .thenReturn(getAutomaticDependencyUpdatesSettingAsFlowUseCase)
        `when`(settingsUseCase.getAutomaticDuplicateDownloadDeletionSettingAsFlowUseCase)
            .thenReturn(getAutomaticDuplicateDownloadDeletionSettingAsFlowUseCase)

        return ConfigureBackgroundWorkUseCase(
            settingsUseCase = settingsUseCase,
            configureAutomaticAppUpdatesSettingUseCase = configureAutomaticAppUpdatesSettingUseCase,
            configureAutomaticDependencyUpdatesSettingUseCase = configureAutomaticDependencyUpdatesSettingUseCase,
            configureAutomaticDuplicateDownloadDeletionSettingUseCase = configureAutomaticDuplicateDownloadDeletionSettingUseCase,
            enqueueOrphanDownloadFilesCleanupUseCase = enqueueOrphanDownloadFilesCleanupUseCase,
        )
    }
}
