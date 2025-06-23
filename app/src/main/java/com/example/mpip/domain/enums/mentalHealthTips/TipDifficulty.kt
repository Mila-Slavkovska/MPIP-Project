package com.example.mpip.domain.enums.mentalHealthTips

enum class TipDifficulty(val displayName: String, val timeEstimate: String, val emoji: String) {
    EASY("Easy", "1-2 minutes", "🟢"),
    MEDIUM("Medium", "5-10 minutes", "🟡"),
    HARD("Challenging", "15+ minutes", "🔴")
}