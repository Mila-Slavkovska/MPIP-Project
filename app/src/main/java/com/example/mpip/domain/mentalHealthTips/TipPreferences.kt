package com.example.mpip.domain.mentalHealthTips

import com.example.mpip.domain.enums.mentalHealthTips.TipCategory
import com.example.mpip.domain.enums.mentalHealthTips.TipDifficulty

data class TipPreferences(
    val preferredCategories: List<TipCategory> = emptyList(),
    val preferredDifficulty: TipDifficulty = TipDifficulty.EASY,
    val personalizeBasedOnEmotions: Boolean = true,
    val personalizeBasedOnTasks: Boolean = true,
    val avoidCategories: List<TipCategory> = emptyList()
)
