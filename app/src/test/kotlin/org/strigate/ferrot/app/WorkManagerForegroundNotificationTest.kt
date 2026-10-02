package org.strigate.ferrot.app

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.impl.Processor
import androidx.work.impl.WorkManagerImpl
import androidx.work.impl.constraints.WorkConstraintsTracker
import androidx.work.impl.foreground.SystemForegroundDispatcher
import androidx.work.impl.model.WorkGenerationalId
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mockStatic
import org.mockito.MockitoAnnotations
import java.lang.reflect.Proxy
import java.util.UUID

class WorkManagerForegroundNotificationTest {
    private lateinit var autoCloseable: AutoCloseable

    private lateinit var logMock: MockedStatic<Log>

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var workManager: WorkManagerImpl

    @Mock
    private lateinit var processor: Processor

    @Mock
    private lateinit var taskExecutor: TaskExecutor

    @Mock
    private lateinit var tracker: WorkConstraintsTracker

    @Mock
    private lateinit var downloading: Notification

    @Mock
    private lateinit var command: Intent

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
        logMock = mockStatic(Log::class.java)
    }

    @Test
    fun lateServiceCommand_postsNotificationAfterWorkHasFinished() {
        `when`(workManager.processor)
            .thenReturn(processor)
        `when`(workManager.workTaskExecutor)
            .thenReturn(taskExecutor)

        val constructor = SystemForegroundDispatcher::class.java.getDeclaredConstructor(
            Context::class.java,
            WorkManagerImpl::class.java,
            WorkConstraintsTracker::class.java,
        ).apply { isAccessible = true }

        val dispatcher = constructor.newInstance(context, workManager, tracker)
        val callbackClass = Class.forName(
            "androidx.work.impl.foreground.SystemForegroundDispatcher\$Callback",
        )
        val postedNotifications = mutableListOf<Notification>()
        val callback = Proxy.newProxyInstance(
            callbackClass.classLoader,
            arrayOf(callbackClass),
        ) { _, method, arguments ->
            if (method.name == "startForeground") {
                postedNotifications += arguments!![2] as Notification
            }
            null
        }
        SystemForegroundDispatcher::class.java.getDeclaredMethod("setCallback", callbackClass)
            .apply { isAccessible = true }
            .invoke(dispatcher, callback)

        val workId = UUID.randomUUID().toString()

        `when`(command.action)
            .thenReturn("ACTION_NOTIFY")
        `when`(command.getIntExtra("KEY_NOTIFICATION_ID", 0))
            .thenReturn(42)
        `when`(command.getStringExtra("KEY_WORKSPEC_ID"))
            .thenReturn(workId)
        @Suppress("DEPRECATION")
        `when`(command.getParcelableExtra<Notification>("KEY_NOTIFICATION"))
            .thenReturn(downloading)

        dispatcher.onExecuted(WorkGenerationalId(workId, 0), false)
        SystemForegroundDispatcher::class.java
            .getDeclaredMethod(
                "onStartCommand",
                Intent::class.java,
                Int::class.javaPrimitiveType
            )
            .apply { isAccessible = true }
            .invoke(dispatcher, command, 1)

        assertEquals(listOf(downloading), postedNotifications)
    }

    @After
    fun tearDown() {
        logMock.close()
        autoCloseable.close()
    }
}
