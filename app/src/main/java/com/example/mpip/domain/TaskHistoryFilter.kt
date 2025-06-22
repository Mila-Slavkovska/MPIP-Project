package com.example.mpip.domain

data class TaskHistoryFilter(
    val startDate: String = "",
    val endDate: String = "",
    val emotions: List<String> = emptyList(),
    val minPoints: Int = 0,
    val maxPoints: Int = Int.MAX_VALUE
)
