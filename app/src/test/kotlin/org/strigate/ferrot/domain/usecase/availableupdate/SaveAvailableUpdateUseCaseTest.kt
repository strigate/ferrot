package org.strigate.ferrot.domain.usecase.availableupdate

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.strigate.ferrot.domain.model.AvailableUpdate
import org.strigate.ferrot.domain.repository.AvailableUpdateRepository

class SaveAvailableUpdateUseCaseTest {
    @Test
    fun invoke_savesMappedUpdate() = runTest {
        val repository = mock(AvailableUpdateRepository::class.java)
        SaveAvailableUpdateUseCase(repository)("v2", "/update.apk")
        verify(repository).save(AvailableUpdate("v2", "/update.apk"))
    }
}
