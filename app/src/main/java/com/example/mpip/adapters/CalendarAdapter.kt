package com.example.mpip.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mpip.R
import com.example.mpip.domain.CalendarDay
import java.text.SimpleDateFormat
import java.util.Locale

class CalendarAdapter(
    private val onDayClick: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.DayViewHolder>() {
    private var days = listOf<CalendarDay>()
    private val dayFormat = SimpleDateFormat("d", Locale.getDefault())

    fun updateDays(newDays: List<CalendarDay>) {
        days = newDays
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount() = days.size

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayNumber: TextView = itemView.findViewById(R.id.day_number)
        private val pointsIndicator: TextView = itemView.findViewById(R.id.points_indicator)
        private val streakIndicator: View = itemView.findViewById(R.id.streak_indicator)

        fun bind(calendarDay: CalendarDay) {
            val context = itemView.context

            val dayNum = try {
                val date =
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(calendarDay.date)
                if (date != null) dayFormat.format(date) else "?"
            } catch (e: Exception) {
                "?"
            }

            dayNumber.text = dayNum

            when {
                calendarDay.isToday -> {
                    dayNumber.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                    itemView.setBackgroundResource(R.drawable.bg_calendar_today)
                    dayNumber.textSize = 16f
                }

                calendarDay.hasLogin -> {
                    dayNumber.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.calendar_active_text
                        )
                    )
                    itemView.setBackgroundResource(R.drawable.bg_calendar_active_day)
                    dayNumber.textSize = 14f
                }

                calendarDay.isInCurrentMonth -> {
                    dayNumber.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.calendar_current_month_text
                        )
                    )
                    itemView.setBackgroundResource(R.drawable.bg_calendar_normal_day)
                    dayNumber.textSize = 14f
                }

                else -> {
                    dayNumber.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.calendar_other_month_text
                        )
                    )
                    itemView.setBackgroundResource(R.drawable.bg_calendar_other_month_day)
                    dayNumber.textSize = 12f
                }
            }

            if (calendarDay.pointsEarned > 0) {
                pointsIndicator.visibility = View.VISIBLE
                pointsIndicator.text = when {
                    calendarDay.pointsEarned >= 50 -> "🏆"
                    calendarDay.pointsEarned >= 25 -> "⭐"
                    else -> "•"
                }
            } else {
                pointsIndicator.visibility = View.GONE
            }

            streakIndicator.visibility = if (calendarDay.hasLogin) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                onDayClick(calendarDay)
            }
        }
    }
}