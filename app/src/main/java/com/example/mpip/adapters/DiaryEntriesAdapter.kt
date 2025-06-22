package com.example.mpip.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mpip.R
import com.example.mpip.domain.DiaryEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DiaryEntriesAdapter(
    private val entries: List<DiaryEntry>,
    private val onEntryClick: (DiaryEntry) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val displayDateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val entryDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        private const val TYPE_MONTH_HEADER = 0
        private const val TYPE_ENTRY = 1
    }

    private val displayItems = mutableListOf<DisplayItem>()

    init {
        processEntriesWithHeaders()
    }

    override fun getItemViewType(position: Int): Int {
        return when (displayItems[position]) {
            is DisplayItem.MonthHeader -> TYPE_MONTH_HEADER
            is DisplayItem.Entry -> TYPE_ENTRY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_MONTH_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_diary_month_header, parent, false)
                MonthHeaderViewHolder(view)
            }

            TYPE_ENTRY -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_diary_entry, parent, false)
                EntryViewHolder(view)
            }

            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayItems[position]) {
            is DisplayItem.MonthHeader -> {
                (holder as MonthHeaderViewHolder).bind(item.monthYear, item.entryCount)
            }

            is DisplayItem.Entry -> {
                (holder as EntryViewHolder).bind(item.entry)
            }
        }
    }

    override fun getItemCount() = displayItems.size

    private fun processEntriesWithHeaders() {
        displayItems.clear()

        if (entries.isEmpty()) return

        val entriesByMonth = entries.groupBy { entry ->
            try {
                val date = entryDateFormat.parse(entry.date)
                if (date != null) monthYearFormat.format(date) else "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }
        }

        val sortedMonths = entriesByMonth.keys.sortedByDescending { monthYear ->
            try {
                monthYearFormat.parse(monthYear)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }

        sortedMonths.forEach { monthYear ->
            val monthEntries = entriesByMonth[monthYear] ?: emptyList()

            displayItems.add(DisplayItem.MonthHeader(monthYear, monthEntries.size))

            val sortedEntries = monthEntries.sortedByDescending { it.date }
            sortedEntries.forEach { entry ->
                displayItems.add(DisplayItem.Entry(entry))
            }
        }
    }

    inner class MonthHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val monthText: TextView = itemView.findViewById(R.id.month_text)
        private val entryCountText: TextView = itemView.findViewById(R.id.entry_count_text)

        fun bind(monthYear: String, entryCount: Int) {
            monthText.text = monthYear
            entryCountText.text = "$entryCount ${if (entryCount == 1) "entry" else "entries"}"
        }
    }

    inner class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateText: TextView = itemView.findViewById(R.id.entry_date_text)
        private val titleText: TextView = itemView.findViewById(R.id.entry_title_text)
        private val previewText: TextView = itemView.findViewById(R.id.entry_preview_text)
        private val moodEmoji: TextView = itemView.findViewById(R.id.entry_mood_emoji)
        private val wordCountText: TextView = itemView.findViewById(R.id.entry_word_count_text)
        private val timeText: TextView = itemView.findViewById(R.id.entry_time_text)

        fun bind(entry: DiaryEntry) {
            try {
                val date = entryDateFormat.parse(entry.date)
                dateText.text = if (date != null) {
                    when {
                        entry.isToday() -> "Today"
                        isYesterday(date) -> "Yesterday"
                        entry.isThisWeek() -> SimpleDateFormat("EEEE", Locale.getDefault()).format(
                            date
                        )

                        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
                    }
                } else {
                    entry.date
                }
            } catch (e: Exception) {
                dateText.text = entry.date
            }

            titleText.text = entry.title.ifEmpty { "Untitled" }
            previewText.text = entry.getPreview()

            if (entry.moodEmoji.isNotEmpty()) {
                moodEmoji.text = entry.moodEmoji
                moodEmoji.visibility = View.VISIBLE
            } else {
                moodEmoji.visibility = View.GONE
            }

            wordCountText.text = "${entry.wordCount} words"
            timeText.text = entry.getCreationTime()

            itemView.setOnClickListener {
                onEntryClick(entry)
            }

            if (entry.isToday()) {
                itemView.alpha = 1.0f
                itemView.elevation = 4f
            } else {
                itemView.alpha = 0.9f
                itemView.elevation = 2f
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

    sealed class DisplayItem {
        data class MonthHeader(val monthYear: String, val entryCount: Int) : DisplayItem()
        data class Entry(val entry: DiaryEntry) : DisplayItem()
    }
}