package com.example.mpip.domain

import com.example.mpip.domain.enums.calendar.CalendarDayStatus

data class CalendarDay(
    val date: String,
    val hasLogin: Boolean = false,
    val pointsEarned: Int = 0,
    val tasksCompleted: Int = 0,
    val isToday: Boolean = false,
    val isInCurrentMonth: Boolean = true,
    val isSelected: Boolean = false
) {
    fun getDisplayStatus(): CalendarDayStatus {
        return when {
            isToday -> CalendarDayStatus.TODAY
            hasLogin && pointsEarned >= 50 -> CalendarDayStatus.HIGH_ACTIVITY
            hasLogin && pointsEarned >= 25 -> CalendarDayStatus.MEDIUM_ACTIVITY
            hasLogin -> CalendarDayStatus.LOW_ACTIVITY
            isInCurrentMonth -> CalendarDayStatus.NO_ACTIVITY
            else -> CalendarDayStatus.OTHER_MONTH
        }
    }

    fun getPointsIcon(): String {
        return when {
            pointsEarned >= 50 -> "🏆"
            pointsEarned >= 25 -> "⭐"
            pointsEarned > 0 -> "•"
            else -> ""
        }
    }

    fun getActivitySummary(): String {
        return when {
            !hasLogin -> "No activity"
            tasksCompleted > 0 -> "$pointsEarned points • $tasksCompleted tasks"
            else -> "$pointsEarned points earned"
        }
    }
}
