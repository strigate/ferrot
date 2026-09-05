package org.strigate.ferrot.presentation.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.analytics.AnalyticsEvents
import org.strigate.ferrot.analytics.AnalyticsLogger
import org.strigate.ferrot.domain.model.DownloadSwipeAction
import org.strigate.ferrot.domain.usecase.SettingsUseCase
import org.strigate.ferrot.domain.usecase.apply.ApplyWifiOnlyPolicyUseCase
import org.strigate.ferrot.domain.usecase.apply.ConfigureAutomaticDuplicateDownloadDeletionWorkUseCase
import org.strigate.ferrot.domain.usecase.settings.GetAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.GetCookiesEnabledSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.GetLeftSwipeActionSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.GetRightSwipeActionSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.GetWifiOnlyDownloadsEnabledSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.SaveAutomaticDuplicateDownloadDeletionEnabledSettingUseCase
import org.strigate.ferrot.domain.usecase.settings.SaveCookiesEnabledSettingUseCase
import org.strigate.ferrot.domain.usecase.settings.SaveLeftSwipeActionSettingUseCase
import org.strigate.ferrot.domain.usecase.settings.SaveRightSwipeActionSettingUseCase
import org.strigate.ferrot.domain.usecase.settings.SaveWifiOnlyDownloadsEnabledSettingUseCase
import org.strigate.ferrot.presentation.model.DownloadSwipeActionUiData
import org.strigate.ferrot.presentation.state.SettingsUiState
import org.strigate.ferrot.test.MainDispatcherRule
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val testDispatcher: TestDispatcher = mainDispatcherRule.testDispatcher
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var analyticsLogger: AnalyticsLogger

    @Mock
    private lateinit var settingsUseCase: SettingsUseCase

    @Mock
    private lateinit var getWifiOnlyDownloadsEnabledSettingAsFlowUseCase: GetWifiOnlyDownloadsEnabledSettingAsFlowUseCase

    @Mock
    private lateinit var getAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase: GetAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase

    @Mock
    private lateinit var saveWifiOnlyDownloadsEnabledSettingUseCase: SaveWifiOnlyDownloadsEnabledSettingUseCase

    @Mock
    private lateinit var saveAutomaticDuplicateDownloadDeletionEnabledSettingUseCase: SaveAutomaticDuplicateDownloadDeletionEnabledSettingUseCase

    @Mock
    private lateinit var applyWifiOnlyPolicyUseCase: ApplyWifiOnlyPolicyUseCase

    @Mock
    private lateinit var configureAutomaticDuplicateDownloadDeletionWorkUseCase: ConfigureAutomaticDuplicateDownloadDeletionWorkUseCase

    @Mock
    private lateinit var getCookiesEnabledSettingAsFlowUseCase: GetCookiesEnabledSettingAsFlowUseCase

    @Mock
    private lateinit var saveCookiesEnabledSettingUseCase: SaveCookiesEnabledSettingUseCase

    @Mock
    private lateinit var getLeftSwipeActionSettingAsFlowUseCase: GetLeftSwipeActionSettingAsFlowUseCase

    @Mock
    private lateinit var getRightSwipeActionSettingAsFlowUseCase: GetRightSwipeActionSettingAsFlowUseCase

    @Mock
    private lateinit var saveLeftSwipeActionSettingUseCase: SaveLeftSwipeActionSettingUseCase

    @Mock
    private lateinit var saveRightSwipeActionSettingUseCase: SaveRightSwipeActionSettingUseCase

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun uiState_exposesMappedSettings() = runTest(testDispatcher) {
        val wifiOnlyDownloadsEnabledFlow = MutableStateFlow(true)
        val automaticDuplicateDownloadDeletionEnabledFlow = MutableStateFlow(false)
        val leftSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.ARCHIVE)
        val rightSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.DELETE)
        val viewModel = createViewModel(
            wifiOnlyDownloadsEnabledFlow = wifiOnlyDownloadsEnabledFlow,
            automaticDuplicateDownloadDeletionEnabledFlow =
                automaticDuplicateDownloadDeletionEnabledFlow,
            leftSwipeActionFlow = leftSwipeActionFlow,
            rightSwipeActionFlow = rightSwipeActionFlow,
        )

        val collector = backgroundScope.launch {
            viewModel.uiState.collect()
        }
        waitForUiState(viewModel) { it is SettingsUiState.Data }

        val state = viewModel.uiState.value as SettingsUiState.Data
        assertEquals(true, state.data.wifiOnlyDownloadsEnabled)
        assertEquals(false, state.data.automaticDuplicateDownloadDeletionEnabled)
        assertEquals(true, state.data.cookiesEnabled)
        assertEquals(DownloadSwipeActionUiData.ARCHIVE, state.data.leftSwipeAction)
        assertEquals(DownloadSwipeActionUiData.DELETE, state.data.rightSwipeAction)

        collector.cancel()
    }

    @Test
    fun logShown_logsSettingsScreen() {
        val viewModel = createViewModel(
            wifiOnlyDownloadsEnabledFlow = MutableStateFlow(false),
            automaticDuplicateDownloadDeletionEnabledFlow = MutableStateFlow(false),
            leftSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.ARCHIVE),
            rightSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.DELETE),
        )

        viewModel.logShown()

        verify(analyticsLogger)
            .logScreen(AnalyticsEvents.Screens.SETTINGS)
    }

    @Test
    fun setWifiOnlyDownloadsEnabled_savesSetting_andAppliesPolicy() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            wifiOnlyDownloadsEnabledFlow = MutableStateFlow(false),
            automaticDuplicateDownloadDeletionEnabledFlow = MutableStateFlow(false),
            leftSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.ARCHIVE),
            rightSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.DELETE),
        )

        viewModel.setWifiOnlyDownloadsEnabled(true)
        advanceUntilIdle()

        verify(saveWifiOnlyDownloadsEnabledSettingUseCase)
            .invoke(true)
        verify(applyWifiOnlyPolicyUseCase)
            .invoke(true)
    }

    @Test
    fun setAutomaticDuplicateDownloadDeletionEnabled_savesAndApplies() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            wifiOnlyDownloadsEnabledFlow = MutableStateFlow(false),
            automaticDuplicateDownloadDeletionEnabledFlow = MutableStateFlow(false),
            leftSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.ARCHIVE),
            rightSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.DELETE),
        )

        viewModel.setAutomaticDuplicateDownloadDeletionEnabled(true)
        advanceUntilIdle()

        verify(saveAutomaticDuplicateDownloadDeletionEnabledSettingUseCase)
            .invoke(true)
        verify(configureAutomaticDuplicateDownloadDeletionWorkUseCase)
            .invoke(true)
    }

    @Test
    fun setCookiesEnabled_savesSetting() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            wifiOnlyDownloadsEnabledFlow = MutableStateFlow(false),
            automaticDuplicateDownloadDeletionEnabledFlow = MutableStateFlow(false),
            leftSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.ARCHIVE),
            rightSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.DELETE),
        )

        viewModel.setCookiesEnabled(false)
        advanceUntilIdle()

        verify(saveCookiesEnabledSettingUseCase)
            .invoke(false)
    }

    @Test
    fun setLeftSwipeAction_savesSetting() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            wifiOnlyDownloadsEnabledFlow = MutableStateFlow(false),
            automaticDuplicateDownloadDeletionEnabledFlow = MutableStateFlow(false),
            leftSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.ARCHIVE),
            rightSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.DELETE),
        )

        viewModel.setLeftSwipeAction(DownloadSwipeActionUiData.NONE)
        advanceUntilIdle()

        verify(saveLeftSwipeActionSettingUseCase)
            .invoke(DownloadSwipeAction.NONE)
    }

    @Test
    fun setRightSwipeAction_savesSetting() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            wifiOnlyDownloadsEnabledFlow = MutableStateFlow(false),
            automaticDuplicateDownloadDeletionEnabledFlow = MutableStateFlow(false),
            leftSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.ARCHIVE),
            rightSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.DELETE),
        )

        viewModel.setRightSwipeAction(DownloadSwipeActionUiData.ARCHIVE)
        advanceUntilIdle()

        verify(saveRightSwipeActionSettingUseCase)
            .invoke(DownloadSwipeAction.ARCHIVE)
    }

    @Test
    fun uiState_exposesErrorWhenSettingsCannotBeRead() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            wifiOnlyDownloadsEnabledFlow = flow { throw IOException("unavailable") },
            automaticDuplicateDownloadDeletionEnabledFlow = MutableStateFlow(false),
            leftSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.NONE),
            rightSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.NONE),
        )
        val collector = backgroundScope.launch { viewModel.uiState.collect() }

        waitForUiState(viewModel) { it is SettingsUiState.Error }
        assertEquals(SettingsUiState.Error, viewModel.uiState.value)

        collector.cancel()
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private fun createViewModel(
        wifiOnlyDownloadsEnabledFlow: Flow<Boolean>,
        automaticDuplicateDownloadDeletionEnabledFlow: MutableStateFlow<Boolean>,
        cookiesEnabledFlow: MutableStateFlow<Boolean> = MutableStateFlow(true),
        leftSwipeActionFlow: MutableStateFlow<DownloadSwipeAction>,
        rightSwipeActionFlow: MutableStateFlow<DownloadSwipeAction>,
    ): SettingsViewModel {
        `when`(getWifiOnlyDownloadsEnabledSettingAsFlowUseCase.invoke())
            .thenReturn(wifiOnlyDownloadsEnabledFlow)
        `when`(getAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase.invoke())
            .thenReturn(automaticDuplicateDownloadDeletionEnabledFlow)
        `when`(getCookiesEnabledSettingAsFlowUseCase.invoke())
            .thenReturn(cookiesEnabledFlow)
        `when`(getLeftSwipeActionSettingAsFlowUseCase.invoke())
            .thenReturn(leftSwipeActionFlow)
        `when`(getRightSwipeActionSettingAsFlowUseCase.invoke())
            .thenReturn(rightSwipeActionFlow)
        `when`(settingsUseCase.getWifiOnlyDownloadsEnabledSettingAsFlowUseCase)
            .thenReturn(getWifiOnlyDownloadsEnabledSettingAsFlowUseCase)
        `when`(settingsUseCase.getAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase)
            .thenReturn(getAutomaticDuplicateDownloadDeletionEnabledSettingAsFlowUseCase)
        `when`(settingsUseCase.getCookiesEnabledSettingAsFlowUseCase)
            .thenReturn(getCookiesEnabledSettingAsFlowUseCase)
        `when`(settingsUseCase.getLeftSwipeActionSettingAsFlowUseCase)
            .thenReturn(getLeftSwipeActionSettingAsFlowUseCase)
        `when`(settingsUseCase.getRightSwipeActionSettingAsFlowUseCase)
            .thenReturn(getRightSwipeActionSettingAsFlowUseCase)
        `when`(settingsUseCase.saveWifiOnlyDownloadsEnabledSettingUseCase)
            .thenReturn(saveWifiOnlyDownloadsEnabledSettingUseCase)
        `when`(settingsUseCase.saveAutomaticDuplicateDownloadDeletionEnabledSettingUseCase)
            .thenReturn(saveAutomaticDuplicateDownloadDeletionEnabledSettingUseCase)
        `when`(settingsUseCase.saveCookiesEnabledSettingUseCase)
            .thenReturn(saveCookiesEnabledSettingUseCase)
        `when`(settingsUseCase.saveLeftSwipeActionSettingUseCase)
            .thenReturn(saveLeftSwipeActionSettingUseCase)
        `when`(settingsUseCase.saveRightSwipeActionSettingUseCase)
            .thenReturn(saveRightSwipeActionSettingUseCase)

        return SettingsViewModel(
            analyticsLogger = analyticsLogger,
            settingsUseCase = settingsUseCase,
            applyWifiOnlyPolicyUseCase = applyWifiOnlyPolicyUseCase,
            configureAutomaticDuplicateDownloadDeletionWorkUseCase = configureAutomaticDuplicateDownloadDeletionWorkUseCase,
        )
    }

    private suspend fun waitForUiState(
        viewModel: SettingsViewModel,
        predicate: (SettingsUiState) -> Boolean,
    ) {
        withTimeout(2.seconds) {
            while (!predicate(viewModel.uiState.value)) {
                kotlinx.coroutines.yield()
            }
        }
    }
}
