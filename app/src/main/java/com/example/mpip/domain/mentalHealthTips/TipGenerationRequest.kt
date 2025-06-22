package com.example.mpip.domain.mentalHealthTips

data class TipGenerationRequest(
    val userId: String,
    val date: String,
    val userEmotions: List<String> = emptyList(),
    val recentTasks: List<String> = emptyList(),
    val userPreferences: TipPreferences = TipPreferences(),
    val previousTips: List<String> = emptyList()
)
