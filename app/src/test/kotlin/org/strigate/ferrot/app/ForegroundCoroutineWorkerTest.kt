package org.strigate.ferrot.app

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.work.WorkerParameters
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.RETURNS_SELF
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.withSettings
import org.mockito.MockitoAnnotations
import java.util.UUID

class ForegroundCoroutineWorkerTest {
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var workerParameters: WorkerParameters

    @Mock
    private lateinit var notificationManager: NotificationManager

    @Mock
    private lateinit var activeNotification: StatusBarNotification

    @Mock
    private lateinit var notification: Notification

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)

        `when`(workerParameters.id)
            .thenReturn(UUID(0L, 42L))
        `when`(context.getSystemService(NotificationManager::class.java))
            .thenReturn(notificationManager)
    }

    @Test
    fun clearForegroundNotification_cancelsOnlyOwnedId() {
        createWorker().clearNotification()

        verify(notificationManager)
            .cancel(42)
        verifyNoMoreInteractions(notificationManager)
    }

    @Test
    fun updateExistingNotification_doesNotCreateNotification() {
        `when`(notificationManager.activeNotifications)
            .thenReturn(emptyArray())

        createWorker().updateExistingNotification()

        verify(notificationManager)
            .activeNotifications
        verifyNoMoreInteractions(notificationManager)
    }

    @Test
    fun updateExistingNotification_recoversWithoutServiceStart() {
        `when`(activeNotification.id)
            .thenReturn(42)
        `when`(activeNotification.notification)
            .thenReturn(notification)
        `when`(notificationManager.activeNotifications)
            .thenReturn(arrayOf(activeNotification))

        mockConstruction(Intent::class.java).use {
            mockStatic(PendingIntent::class.java).use {
                mockConstruction(
                    NotificationCompat.Builder::class.java,
                    withSettings().defaultAnswer(RETURNS_SELF),
                ) { builder, _ ->
                    `when`(builder.build())
                        .thenReturn(notification)
                }.use { builders ->
                    createWorker().updateExistingNotification()
                    verify(builders.constructed().single())
                        .setOngoing(false)
                    verify(builders.constructed().single())
                        .setAutoCancel(true)
                }
            }
        }

        verify(notificationManager)
            .notify(42, notification)
        verify(workerParameters, never())
            .foregroundUpdater
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }

    private fun createWorker() = object : ForegroundCoroutineWorker(context, workerParameters) {
        override suspend fun doWork(): Result = Result.success()

        fun clearNotification() = clearForegroundNotification()

        fun updateExistingNotification() = updateExistingForegroundNotification("failed")
    }
}
