package com.example.mpip.domain

data class MonthlyProgress(
    val month: String,
    val totalPoints: Int = 0,
    val loginDays: Int = 0,
    val tasksCompleted: Int = 0,
    val streakDays: Int = 0,
    val dailyData: Map<String, DailyProgress> = emptyMap()
)
