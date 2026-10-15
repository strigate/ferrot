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
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

class MediaProbeTimeoutTest {
    @Test
    fun returnsSuccessfulProbeOutput() = runTest {
        assertEquals("probe output", withMediaProbeTimeout { "probe output" })
    }

    @Test
    fun localTimeoutBecomesProcessingFailure() = runTest {
        val failure = runCatching {
            withMediaProbeTimeout { awaitCancellation() }
        }.exceptionOrNull()

        assertTrue(failure is IOException)
        assertEquals("Media inspection timed out", failure?.message)
    }

    @Test
    fun callerTimeoutRemainsCancellation() = runTest {
        val failure = runCatching {
            withTimeout(10L.milliseconds) {
                withMediaProbeTimeout { awaitCancellation() }
            }
        }.exceptionOrNull()

        assertTrue(failure is TimeoutCancellationException)
    }

    @Test
    fun callerCancellationRemainsCancellation() = runTest {
        val started = CompletableDeferred<Unit>()
        val failure = CompletableDeferred<Throwable>()
        val probe = launch {
            try {
                withMediaProbeTimeout {
                    started.complete(Unit)
                    awaitCancellation()
                }
            } catch (exception: CancellationException) {
                failure.complete(exception)
                throw exception
            }
        }
        started.await()
        probe.cancelAndJoin()

        assertTrue(failure.await() is CancellationException)
    }

    @Test
    fun preservesOtherProcessingFailures() = runTest {
        val expected = IOException("Probe failed")
        val failure = runCatching {
            withMediaProbeTimeout { throw expected }
        }.exceptionOrNull()

        val rootCause = generateSequence(failure) { it.cause }.last()
        assertSame(expected, rootCause)
    }
}
