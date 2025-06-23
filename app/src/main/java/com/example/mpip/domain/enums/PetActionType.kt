package com.example.mpip.domain.enums

enum class PetActionType(
    val displayName: String,
    val pointsCost: Int,
    val happinessIncrease: Int,
    val energyIncrease: Int,
    val emoji: String
) {
    FEED("Feed", 20, 15, 5, "🍎"),
    PLAY("Play", 25, 10, 20, "🎮"),
    CARE("Care", 15, 10, 10, "❤️")
}
