package org.strigate.ferrot.presentation.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
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
import org.strigate.ferrot.domain.usecase.apply.ConfigureAutomaticDuplicateDownloadDeletionSettingUseCase
import org.strigate.ferrot.domain.usecase.settings.GetAutomaticDuplicateDownloadDeletionSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.GetDownloadWifiOnlySettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.GetUseCookiesSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.GetLeftSwipeActionSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.GetRightSwipeActionSettingAsFlowUseCase
import org.strigate.ferrot.domain.usecase.settings.SaveAutomaticDuplicateDownloadDeletionSettingUseCase
import org.strigate.ferrot.domain.usecase.settings.SaveDownloadWifiOnlySettingUseCase
import org.strigate.ferrot.domain.usecase.settings.SaveUseCookiesSettingUseCase
import org.strigate.ferrot.domain.usecase.settings.SaveLeftSwipeActionSettingUseCase
import org.strigate.ferrot.domain.usecase.settings.SaveRightSwipeActionSettingUseCase
import org.strigate.ferrot.presentation.model.DownloadSwipeActionUiData
import org.strigate.ferrot.presentation.state.SettingsUiState
import org.strigate.ferrot.test.MainDispatcherRule
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
    private lateinit var getDownloadWifiOnlySettingAsFlowUseCase: GetDownloadWifiOnlySettingAsFlowUseCase

    @Mock
    private lateinit var getAutomaticDuplicateDownloadDeletionSettingAsFlowUseCase: GetAutomaticDuplicateDownloadDeletionSettingAsFlowUseCase

    @Mock
    private lateinit var saveDownloadWifiOnlySettingUseCase: SaveDownloadWifiOnlySettingUseCase

    @Mock
    private lateinit var saveAutomaticDuplicateDownloadDeletionSettingUseCase: SaveAutomaticDuplicateDownloadDeletionSettingUseCase

    @Mock
    private lateinit var applyWifiOnlyPolicyUseCase: ApplyWifiOnlyPolicyUseCase

    @Mock
    private lateinit var configureAutomaticDuplicateDownloadDeletionSettingUseCase: ConfigureAutomaticDuplicateDownloadDeletionSettingUseCase

    @Mock
    private lateinit var getUseCookiesSettingAsFlowUseCase: GetUseCookiesSettingAsFlowUseCase

    @Mock
    private lateinit var saveUseCookiesSettingUseCase: SaveUseCookiesSettingUseCase

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
        val downloadWifiOnlyFlow = MutableStateFlow(true)
        val automaticDeletionFlow = MutableStateFlow(false)
        val leftSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.ARCHIVE)
        val rightSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.DELETE)
        val viewModel = createViewModel(
            downloadWifiOnlyFlow = downloadWifiOnlyFlow,
            automaticDeletionFlow = automaticDeletionFlow,
            leftSwipeActionFlow = leftSwipeActionFlow,
            rightSwipeActionFlow = rightSwipeActionFlow,
        )

        val collector = backgroundScope.launch {
            viewModel.uiState.collect()
        }
        waitForUiState(viewModel) { it is SettingsUiState.Data }

        val state = viewModel.uiState.value as SettingsUiState.Data
        assertEquals(true, state.data.downloadWifiOnly)
        assertEquals(false, state.data.automaticDuplicateDownloadDeletion)
        assertEquals(true, state.data.useCookies)
        assertEquals(DownloadSwipeActionUiData.ARCHIVE, state.data.leftSwipeAction)
        assertEquals(DownloadSwipeActionUiData.DELETE, state.data.rightSwipeAction)

        collector.cancel()
    }

    @Test
    fun logShown_logsSettingsScreen() {
        val viewModel = createViewModel(
            downloadWifiOnlyFlow = MutableStateFlow(false),
            automaticDeletionFlow = MutableStateFlow(false),
            leftSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.ARCHIVE),
            rightSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.DELETE),
        )

        viewModel.logShown()

        verify(analyticsLogger)
            .logScreen(AnalyticsEvents.Screens.SETTINGS)
    }

    @Test
    fun setDownloadWifiOnly_savesSetting_andAppliesPolicy() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadWifiOnlyFlow = MutableStateFlow(false),
            automaticDeletionFlow = MutableStateFlow(false),
            leftSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.ARCHIVE),
            rightSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.DELETE),
        )

        viewModel.setDownloadWifiOnly(true)
        advanceUntilIdle()

        verify(saveDownloadWifiOnlySettingUseCase)
            .invoke(true)
        verify(applyWifiOnlyPolicyUseCase)
            .invoke(true)
    }

    @Test
    fun setAutomaticDuplicateDownloadDeletion_savesAndApplies() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadWifiOnlyFlow = MutableStateFlow(false),
            automaticDeletionFlow = MutableStateFlow(false),
            leftSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.ARCHIVE),
            rightSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.DELETE),
        )

        viewModel.setAutomaticDuplicateDownloadDeletion(true)
        advanceUntilIdle()

        verify(saveAutomaticDuplicateDownloadDeletionSettingUseCase)
            .invoke(true)
        verify(configureAutomaticDuplicateDownloadDeletionSettingUseCase)
            .invoke(true)
    }

    @Test
    fun setUseCookies_savesSetting() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadWifiOnlyFlow = MutableStateFlow(false),
            automaticDeletionFlow = MutableStateFlow(false),
            leftSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.ARCHIVE),
            rightSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.DELETE),
        )

        viewModel.setUseCookies(false)
        advanceUntilIdle()

        verify(saveUseCookiesSettingUseCase)
            .invoke(false)
    }

    @Test
    fun setLeftSwipeAction_savesSetting() = runTest(testDispatcher) {
        val viewModel = createViewModel(
            downloadWifiOnlyFlow = MutableStateFlow(false),
            automaticDeletionFlow = MutableStateFlow(false),
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
            downloadWifiOnlyFlow = MutableStateFlow(false),
            automaticDeletionFlow = MutableStateFlow(false),
            leftSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.ARCHIVE),
            rightSwipeActionFlow = MutableStateFlow(DownloadSwipeAction.DELETE),
        )

        viewModel.setRightSwipeAction(DownloadSwipeActionUiData.ARCHIVE)
        advanceUntilIdle()

        verify(saveRightSwipeActionSettingUseCase)
            .invoke(DownloadSwipeAction.ARCHIVE)
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private fun createViewModel(
        downloadWifiOnlyFlow: MutableStateFlow<Boolean>,
        automaticDeletionFlow: MutableStateFlow<Boolean>,
        useCookiesFlow: MutableStateFlow<Boolean> = MutableStateFlow(true),
        leftSwipeActionFlow: MutableStateFlow<DownloadSwipeAction>,
        rightSwipeActionFlow: MutableStateFlow<DownloadSwipeAction>,
    ): SettingsViewModel {
        `when`(getDownloadWifiOnlySettingAsFlowUseCase.invoke())
            .thenReturn(downloadWifiOnlyFlow)
        `when`(getAutomaticDuplicateDownloadDeletionSettingAsFlowUseCase.invoke())
            .thenReturn(automaticDeletionFlow)
        `when`(getUseCookiesSettingAsFlowUseCase.invoke())
            .thenReturn(useCookiesFlow)
        `when`(getLeftSwipeActionSettingAsFlowUseCase.invoke())
            .thenReturn(leftSwipeActionFlow)
        `when`(getRightSwipeActionSettingAsFlowUseCase.invoke())
            .thenReturn(rightSwipeActionFlow)
        `when`(settingsUseCase.getDownloadWifiOnlySettingAsFlowUseCase)
            .thenReturn(getDownloadWifiOnlySettingAsFlowUseCase)
        `when`(settingsUseCase.getAutomaticDuplicateDownloadDeletionSettingAsFlowUseCase)
            .thenReturn(getAutomaticDuplicateDownloadDeletionSettingAsFlowUseCase)
        `when`(settingsUseCase.getUseCookiesSettingAsFlowUseCase)
            .thenReturn(getUseCookiesSettingAsFlowUseCase)
        `when`(settingsUseCase.getLeftSwipeActionSettingAsFlowUseCase)
            .thenReturn(getLeftSwipeActionSettingAsFlowUseCase)
        `when`(settingsUseCase.getRightSwipeActionSettingAsFlowUseCase)
            .thenReturn(getRightSwipeActionSettingAsFlowUseCase)
        `when`(settingsUseCase.saveDownloadWifiOnlySettingUseCase)
            .thenReturn(saveDownloadWifiOnlySettingUseCase)
        `when`(settingsUseCase.saveAutomaticDuplicateDownloadDeletionSettingUseCase)
            .thenReturn(saveAutomaticDuplicateDownloadDeletionSettingUseCase)
        `when`(settingsUseCase.saveUseCookiesSettingUseCase)
            .thenReturn(saveUseCookiesSettingUseCase)
        `when`(settingsUseCase.saveLeftSwipeActionSettingUseCase)
            .thenReturn(saveLeftSwipeActionSettingUseCase)
        `when`(settingsUseCase.saveRightSwipeActionSettingUseCase)
            .thenReturn(saveRightSwipeActionSettingUseCase)

        return SettingsViewModel(
            analyticsLogger = analyticsLogger,
            settingsUseCase = settingsUseCase,
            applyWifiOnlyPolicyUseCase = applyWifiOnlyPolicyUseCase,
            configureAutomaticDuplicateDownloadDeletionSettingUseCase = configureAutomaticDuplicateDownloadDeletionSettingUseCase,
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
