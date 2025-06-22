package com.example.mpip.domain

import com.example.mpip.domain.enums.diary.DiarySortOption

data class DiaryFilter(
    val startDate: String = "",
    val endDate: String = "",
    val mood: String = "",
    val searchText: String = "",
    val tags: List<String> = emptyList(),
    val sortBy: DiarySortOption = DiarySortOption.DATE_DESC
)
