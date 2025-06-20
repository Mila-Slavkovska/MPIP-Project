package com.example.mpip.domain

import com.example.mpip.domain.enums.EnergyState
import com.example.mpip.domain.enums.MoodState

data class PetClass(
    var type: String = "",
    var name: String = "",
    var happiness: Int = 100,
    var energy: Int = 100,
    var level: Int = 1,
    var experience: Int = 0,
    var lastFed: Long = System.currentTimeMillis(),
    var lastPlayed: Long = System.currentTimeMillis(),
    var items: List<String> = emptyList()
) {
    fun getMoodState(): MoodState {
        return when {
            happiness >= 80 -> MoodState.HAPPY
            happiness >= 50 -> MoodState.NEUTRAL
            happiness >= 20 -> MoodState.SAD
            else -> MoodState.VERY_SAD
        }
    }

    fun getEnergyState(): EnergyState {
        return when {
            energy >= 80 -> EnergyState.ENERGETIC
            energy >= 50 -> EnergyState.NORMAL
            energy >= 20 -> EnergyState.TIRED
            else -> EnergyState.EXHAUSTED
        }
    }

    fun needsCare(): Boolean {
        val currentTime = System.currentTimeMillis()
        val hoursSinceLastFed = (currentTime - lastFed) / (1000 * 60 * 60)
        val hoursSinceLastPlayed = (currentTime - lastPlayed) / (1000 * 60 * 60)

        return hoursSinceLastFed > 8 || hoursSinceLastPlayed > 12 || happiness < 50 || energy < 30
    }
}
