package com.example.mpip.domain

data class UserProgress(
    val userId: String = "",
    val totalPoints: Int = 0,
    val availablePoints: Int = 0,
    val tasksCompleted: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: String = "",
    val level: Int = 1,
    val monthlyStats: Map<String, MonthlyStats> = emptyMap(),
    val firstLoginDate: String = "",
    val hasClaimedDailyTip: Boolean = false,
    val lastTipDate: String = "",
    val totalPointsSpent: Int = 0,
    val petInteractions: Int = 0
)
