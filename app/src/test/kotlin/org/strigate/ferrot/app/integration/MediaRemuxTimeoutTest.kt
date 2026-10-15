package org.strigate.ferrot.app.integration

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

class MediaRemuxTimeoutTest {
    @Test
    fun successfulRemuxCompletes() = runTest {
        var completed = false
        withMediaRemuxTimeout { completed = true }
        assertTrue(completed)
    }

    @Test
    fun localTimeoutBecomesFailureAfterCleanup() = runTest {
        var cleanedUp = false
        val failure = runCatching {
            withMediaRemuxTimeout {
                try {
                    awaitCancellation()
                } finally {
                    cleanedUp = true
                }
            }
        }.exceptionOrNull()

        assertTrue(failure is IOException)
        assertEquals("Media processing timed out", failure?.message)
        assertTrue(cleanedUp)
    }

    @Test
    fun callerTimeoutRemainsCancellation() = runTest {
        val failure = runCatching {
            withTimeout(10L.milliseconds) {
                withMediaRemuxTimeout { awaitCancellation() }
            }
        }.exceptionOrNull()

        assertTrue(failure is TimeoutCancellationException)
    }

    @Test
    fun callerCancellationRemainsCancellation() = runTest {
        val started = CompletableDeferred<Unit>()
        val failure = CompletableDeferred<Throwable>()
        val remux = launch {
            try {
                withMediaRemuxTimeout {
                    started.complete(Unit)
                    awaitCancellation()
                }
            } catch (exception: CancellationException) {
                failure.complete(exception)
                throw exception
            }
        }
        started.await()
        remux.cancelAndJoin()

        assertTrue(failure.await() is CancellationException)
    }
}
