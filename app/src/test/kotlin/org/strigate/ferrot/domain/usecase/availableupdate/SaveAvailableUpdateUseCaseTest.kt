package org.strigate.ferrot.domain.usecase.availableupdate

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.strigate.ferrot.domain.model.AvailableUpdate
import org.strigate.ferrot.domain.repository.AvailableUpdateRepository

class SaveAvailableUpdateUseCaseTest {
    private lateinit var autoCloseable: AutoCloseable

    @Mock
    private lateinit var repository: AvailableUpdateRepository

    @Before
    fun setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun invoke_savesMappedUpdate() = runTest {
        SaveAvailableUpdateUseCase(repository)("v2", "/update.apk")
        verify(repository)
            .save(AvailableUpdate("v2", "/update.apk"))
    }

    @After
    fun tearDown() {
        autoCloseable.close()
    }
}
