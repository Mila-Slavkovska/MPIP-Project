package com.example.mpip.domain

data class DailyProgress(
    val date: String,
    val hasLogin: Boolean = false,
    val pointsEarned: Int = 0,
    val tasksCompleted: Int = 0,
    val checkInCompleted: Boolean = false,
    val dailyTasksCompleted: Int = 0,
    val questionnaireTasksCompleted: Int = 0
)
