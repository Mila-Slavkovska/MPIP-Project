package com.example.mpip.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DiaryEntry(
    val id: String = "",
    val userId: String = "",
    val date: String = "",
    val title: String = "",
    val content: String = "",
    val mood: String = "",
    val moodEmoji: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val wordCount: Int = 0,
    val tags: List<String> = emptyList(),
    val isPrivate: Boolean = true,
    val hasImages: Boolean = false
) {
    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val displayDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun getCurrentDateString(): String {
            return dateFormat.format(Date())
        }

        fun generateId(userId: String, date: String): String {
            return "${userId}_diary_${date}"
        }
    }

    fun getDisplayDate(): String {
        return try {
            val date = dateFormat.parse(date)
            if (date != null) displayDateFormat.format(date) else this.date
        } catch (e: Exception) {
            this.date
        }
    }

    fun getCreationTime(): String {
        return timeFormat.format(Date(createdAt))
    }

    fun getLastUpdatedTime(): String {
        return timeFormat.format(Date(updatedAt))
    }

    fun calculateWordCount(): Int {
        return if (content.isBlank()) {
            0
        } else {
            content.trim().split("\\s+".toRegex()).size
        }
    }

    fun getPreview(): String {
        return if (content.length <= 100) {
            content
        } else {
            content.take(100) + "..."
        }
    }

    fun isToday(): Boolean {
        return date == getCurrentDateString()
    }

    fun isThisWeek(): Boolean {
        return try {
            val entryDate = dateFormat.parse(date)
            val today = Date()
            val calendar = Calendar.getInstance()

            if (entryDate != null) {
                calendar.time = today
                val currentWeekStart = calendar.apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                }.time

                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                val nextWeekStart = calendar.time

                entryDate.after(currentWeekStart) && entryDate.before(nextWeekStart)
            } else false
        } catch (e: Exception) {
            false
        }
    }

    fun getReadingTimeMinutes(): Int {
        return maxOf(1, wordCount / 200)
    }
}
