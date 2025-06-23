package com.example.mpip.domain

data class TaskCompletion(
    val id: String = "",
    val userId: String = "",
    val taskId: String = "",
    val completedAt: Long = System.currentTimeMillis(),
    val pointsEarned: Int = 0,
    val userResponse: String = "",
    val photoPath: String = "",
    val associatedQuestionnaire: String = ""
)
