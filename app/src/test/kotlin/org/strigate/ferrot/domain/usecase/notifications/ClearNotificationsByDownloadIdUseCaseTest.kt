package org.strigate.ferrot.domain.usecase.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.test.MainDispatcherRule
import org.strigate.ferrot.app.Constants.Action.ACTION_NAVIGATE_DOWNLOAD
import org.strigate.ferrot.app.Constants.Extras.EXTRA_ACTION
import org.strigate.ferrot.app.Constants.Extras.EXTRA_DOWNLOAD_ID
import org.strigate.ferrot.app.Constants.Notifications.Channels.CHANNEL_ID_DOWNLOADED

@OptIn(ExperimentalCoroutinesApi::class)
class ClearNotificationsByDownloadIdUseCaseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val testDispatcher: TestDispatcher = mainDispatcherRule.testDispatcher
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var appContext: Context

    @Mock
    private lateinit var notificationManager: NotificationManager

    @Mock
    private lateinit var statusBarNotification: StatusBarNotification

    @Mock
    private lateinit var extras: Bundle

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)

        `when`(appContext.getSystemService(NotificationManager::class.java))
            .thenReturn(notificationManager)
        `when`(notificationManager.activeNotifications)
            .thenReturn(arrayOf(statusBarNotification))
        `when`(statusBarNotification.id)
            .thenReturn(55)
        `when`(statusBarNotification.tag)
            .thenReturn("download-tag")

        doReturn(ACTION_NAVIGATE_DOWNLOAD)
            .`when`(extras).getString(EXTRA_ACTION)
        doReturn("77")
            .`when`(extras).getString(EXTRA_DOWNLOAD_ID)

        val notification = object : Notification() {
            override fun getChannelId(): String = CHANNEL_ID_DOWNLOADED
        }.apply {
            this.extras = this@ClearNotificationsByDownloadIdUseCaseTest.extras
        }
        doReturn(notification)
            .`when`(statusBarNotification).notification
    }

    @Test
    fun invoke_clearsDownloadedNotifications_matchingDownloadId() {
        ClearNotificationsByDownloadIdUseCase(appContext).invoke(77L)

        verify(notificationManager)
            .cancel("download-tag", 55)
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }
}
