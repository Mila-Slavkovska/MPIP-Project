package com.example.mpip.domain.mentalHealthTips

import com.example.mpip.domain.enums.mentalHealthTips.TipCategory

data class TipStats(
    val totalTipsViewed: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val favoriteCount: Int = 0,
    val totalPointsEarned: Int = 0,
    val categoryCounts: Map<TipCategory, Int> = emptyMap(),
    val lastViewedDate: String = ""
)
