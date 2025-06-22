package com.example.mpip.domain

data class DiaryStats(
    val totalEntries: Int = 0,
    val totalWords: Int = 0,
    val averageWordsPerEntry: Int = 0,
    val longestEntry: Int = 0,
    val currentStreak: Int = 0,
    val activeDays: Int = 0,
    val mostCommonMood: String = "",
    val mostCommonMoodCount: Int = 0,
    val topTags: List<Pair<String, Int>> = emptyList(),
    val entriesByMonth: Map<String, Int> = emptyMap()
)
