package com.example.mpip.domain

data class DailyQuestionnaire(
    val id: String = "",
    val userId: String = "",
    val date: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val selectedEmotions: List<String> = emptyList(),
    val emotionRelations: List<String> = emptyList(),
    val memo: String = "",
    val completed: Boolean = false,
    val selectedEmotionNames: List<String> = emptyList(),
    val selectedRelationNames: List<String> = emptyList(),
    val completedAt: Long = 0
)

