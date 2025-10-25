package org.strigate.ferrot.util

import com.github.f4b6a3.uuid.UuidCreator

object UidUtil {
    fun generateUid(): String {
        val randomBased = UuidCreator.getRandomBased().toString()
        val timeOrderedEpoch = UuidCreator.getTimeOrderedEpoch().toString()
        return "$randomBased$timeOrderedEpoch"
    }
}
