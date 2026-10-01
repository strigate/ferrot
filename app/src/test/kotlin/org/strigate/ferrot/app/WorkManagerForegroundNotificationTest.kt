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
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`
import java.lang.reflect.Proxy
import java.util.UUID

class WorkManagerForegroundNotificationTest {
    @Test
    fun lateServiceCommand_postsNotificationAfterWorkHasFinished() {
        mockStatic(Log::class.java).use {
            val context = mock(Context::class.java)
            val workManager = mock(WorkManagerImpl::class.java)
            val processor = mock(Processor::class.java)
            val taskExecutor = mock(TaskExecutor::class.java)
            val tracker = mock(WorkConstraintsTracker::class.java)

            `when`(workManager.processor).thenReturn(processor)
            `when`(workManager.workTaskExecutor).thenReturn(taskExecutor)

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
            val downloading = mock(Notification::class.java)
            val command = mock(Intent::class.java)

            `when`(command.action).thenReturn("ACTION_NOTIFY")
            `when`(command.getIntExtra("KEY_NOTIFICATION_ID", 0)).thenReturn(42)
            `when`(command.getStringExtra("KEY_WORKSPEC_ID")).thenReturn(workId)
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
    }
}
