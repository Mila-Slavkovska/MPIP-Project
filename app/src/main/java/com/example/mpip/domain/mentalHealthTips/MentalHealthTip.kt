package com.example.mpip.domain.mentalHealthTips

import android.annotation.SuppressLint
import com.example.mpip.domain.enums.mentalHealthTips.TipCategory
import com.example.mpip.domain.enums.mentalHealthTips.TipDifficulty
import com.google.firebase.database.PropertyName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MentalHealthTip(
    var id: String = "",
    var userId: String = "",
    var date: String = "",
    var title: String = "",
    var content: String = "",
    var category: TipCategory = TipCategory.GENERAL,
    var difficulty: TipDifficulty = TipDifficulty.EASY,

    @get:PropertyName("personalized")
    @set:PropertyName("personalized")
    var isPersonalized: Boolean = false,

    var basedOnEmotions: List<String> = emptyList(),
    var basedOnTasks: List<String> = emptyList(),
    var createdAt: Long = System.currentTimeMillis(),
    var viewedAt: Long = 0,

    @get:PropertyName("viewed")
    @set:PropertyName("viewed")
    var isViewed: Boolean = false,

    @get:PropertyName("favorited")
    @set:PropertyName("favorited")
    var isFavorited: Boolean = false,

    var pointsAwarded: Boolean = false,
    var aiModel: String = "gpt-3.5-turbo",
    var generationPrompt: String = "",

    var preview: String = "",
    var readingTimeMinutes: Int = 0,
    var displayDate: String = "",
    var today: Boolean = false
) {
    constructor() : this(
        id = "",
        userId = "",
        date = "",
        title = "",
        content = "",
        category = TipCategory.GENERAL,
        difficulty = TipDifficulty.EASY,
        isPersonalized = false,
        basedOnEmotions = emptyList(),
        basedOnTasks = emptyList(),
        createdAt = System.currentTimeMillis(),
        viewedAt = 0,
        isViewed = false,
        isFavorited = false,
        pointsAwarded = false,
        aiModel = "gpt-3.5-turbo",
        generationPrompt = "",
        preview = "",
        readingTimeMinutes = 0,
        displayDate = "",
        today = false
    )

    companion object {
        @SuppressLint("ConstantLocale")
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun getCurrentDateString(): String {
            return dateFormat.format(Date())
        }

        fun generateId(userId: String, date: String): String {
            return "${userId}_tip_${date}"
        }

        const val FIRST_VIEW_POINTS = 5
    }

    fun syncFields() {
        today = date == getCurrentDateString()

        if (preview.isEmpty() && content.isNotEmpty()) {
            preview = getTruncatedContentForPreview()
        }

        if (displayDate.isEmpty() && date.isNotEmpty()) {
            displayDate = try {
                val tipDate = dateFormat.parse(date)
                if (tipDate != null) {
                    when {
                        this.today -> "Today"
                        isYesterday(tipDate) -> "Yesterday"
                        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(tipDate)
                    }
                } else date
            } catch (e: Exception) {
                date
            }
        }

        if (readingTimeMinutes == 0 && content.isNotEmpty()) {
            val wordCount = content.split("\\s+".toRegex()).size
            readingTimeMinutes = maxOf(1, wordCount / 200)
        }
    }

    private fun getTruncatedContentForPreview(): String {
        return if (content.length <= 100) {
            content
        } else {
            content.take(100) + "..."
        }
    }

    private fun isYesterday(date: Date): Boolean {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -1)
        }.time

        val cal1 = Calendar.getInstance().apply { time = date }
        val cal2 = Calendar.getInstance().apply { time = yesterday }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
