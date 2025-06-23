package com.example.mpip

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mpip.adapters.CalendarAdapter
import com.example.mpip.domain.CalendarDay
import com.example.mpip.domain.UserProgress
import com.example.mpip.repository.Repository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarFragment : Fragment() {
    private lateinit var repository: Repository
    private val auth = FirebaseAuth.getInstance()

    private lateinit var backButton: Button
    private lateinit var monthYearText: TextView
    private lateinit var prevMonthButton: Button
    private lateinit var nextMonthButton: Button
    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var statsLayout: LinearLayout
    private lateinit var currentStreakText: TextView
    private lateinit var longestStreakText: TextView
    private lateinit var monthPointsText: TextView
    private lateinit var loginDaysText: TextView
    private lateinit var selectedDateInfo: LinearLayout
    private lateinit var selectedDateText: TextView
    private lateinit var selectedDatePointsText: TextView
    private lateinit var selectedDateTasksText: TextView

    private lateinit var calendarAdapter: CalendarAdapter
    private var currentCalendar = Calendar.getInstance()
    private var currentUserProgress = UserProgress()
    private var monthlyData = mutableMapOf<String, CalendarDay>()

    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = Repository()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupCalendar()
        loadUserProgress()
        loadMonthData()
    }

    private fun initializeViews(view: View) {
        backButton = view.findViewById(R.id.back_button)
        monthYearText = view.findViewById(R.id.month_year_text)
        prevMonthButton = view.findViewById(R.id.prev_month_button)
        nextMonthButton = view.findViewById(R.id.next_month_button)
        calendarRecyclerView = view.findViewById(R.id.calendar_recycler)
        statsLayout = view.findViewById(R.id.stats_layout)
        currentStreakText = view.findViewById(R.id.current_streak_text)
        longestStreakText = view.findViewById(R.id.longest_streak_text)
        monthPointsText = view.findViewById(R.id.month_points_text)
        loginDaysText = view.findViewById(R.id.login_days_text)
        selectedDateInfo = view.findViewById(R.id.selected_date_info)
        selectedDateText = view.findViewById(R.id.selected_date_text)
        selectedDatePointsText = view.findViewById(R.id.selected_date_points_text)
        selectedDateTasksText = view.findViewById(R.id.selected_date_tasks_text)

        backButton.setOnClickListener {
            (activity as? MainActivity)?.hideDailyTasks()
        }

        prevMonthButton.setOnClickListener {
            navigateMonth(-1)
        }

        nextMonthButton.setOnClickListener {
            navigateMonth(1)
        }
    }

    private fun setupCalendar() {
        calendarAdapter = CalendarAdapter { calendarDay ->
            onDateSelected(calendarDay)
        }

        calendarRecyclerView.apply {
            layoutManager = GridLayoutManager(context, 7) // 7 days per week
            adapter = calendarAdapter
        }

        updateMonthDisplay()
    }

    private fun loadUserProgress() {
        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                currentUserProgress = repository.getUserProgress(userId)!!
                updateStatsDisplay()

                Log.d(
                    "CalendarFragment",
                    "User progress loaded: ${currentUserProgress.currentStreak} streak"
                )

            } catch (e: Exception) {
                Log.e("CalendarFragment", "Error loading user progress: ${e.message}")
                Toast.makeText(context, "Error loading progress", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadMonthData() {
        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                val monthKey =
                    SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentCalendar.time)

                val monthlyProgress = repository.getMonthlyProgress(userId, monthKey)

                monthlyData.clear()
                monthlyProgress.forEach { (dateKey, progress) ->
                    monthlyData[dateKey] = CalendarDay(
                        date = dateKey,
                        hasLogin = progress.hasLogin,
                        pointsEarned = progress.pointsEarned,
                        tasksCompleted = progress.tasksCompleted,
                        isToday = dateKey == dateKeyFormat.format(Date()),
                        isInCurrentMonth = dateKey.startsWith(monthKey)
                    )
                }

                updateCalendarDisplay()

                Log.d("CalendarFragment", "Monthly data loaded: ${monthlyData.size} days")

            } catch (e: Exception) {
                Log.e("CalendarFragment", "Error loading monthly data: ${e.message}")
                Toast.makeText(context, "Error loading calendar data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateMonthDisplay() {
        monthYearText.text = monthYearFormat.format(currentCalendar.time)

        val today = Calendar.getInstance()
        val nextMonth = Calendar.getInstance().apply {
            time = currentCalendar.time
            add(Calendar.MONTH, 1)
        }
        nextMonthButton.isEnabled = !nextMonth.after(today)
        nextMonthButton.alpha = if (nextMonthButton.isEnabled) 1.0f else 0.5f
    }

    private fun updateCalendarDisplay() {
        val calendarDays = generateCalendarDays()
        calendarAdapter.updateDays(calendarDays)
    }

    private fun generateCalendarDays(): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()

        val firstDayOfMonth = Calendar.getInstance().apply {
            time = currentCalendar.time
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val firstDayOfWeek = Calendar.getInstance().apply {
            time = firstDayOfMonth.time
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            add(Calendar.DAY_OF_MONTH, -(dayOfWeek - Calendar.SUNDAY))
        }

        val currentDay = Calendar.getInstance().apply { time = firstDayOfWeek.time }

        repeat(42) {
            val dateKey = dateKeyFormat.format(currentDay.time)
            val calendarDay = monthlyData[dateKey] ?: CalendarDay(
                date = dateKey,
                hasLogin = false,
                pointsEarned = 0,
                tasksCompleted = 0,
                isToday = dateKey == dateKeyFormat.format(Date()),
                isInCurrentMonth = isSameMonth(currentDay, currentCalendar)
            )

            days.add(calendarDay)
            currentDay.add(Calendar.DAY_OF_MONTH, 1)
        }

        return days
    }

    private fun isSameMonth(day: Calendar, month: Calendar): Boolean {
        return day.get(Calendar.YEAR) == month.get(Calendar.YEAR) &&
                day.get(Calendar.MONTH) == month.get(Calendar.MONTH)
    }

    private fun updateStatsDisplay() {
        currentStreakText.text = currentUserProgress.currentStreak.toString()
        longestStreakText.text = currentUserProgress.longestStreak.toString()

        val monthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentCalendar.time)
        val monthPoints = monthlyData.values
            .filter { it.isInCurrentMonth }
            .sumOf { it.pointsEarned }
        val loginDays = monthlyData.values
            .filter { it.isInCurrentMonth && it.hasLogin }
            .size

        monthPointsText.text = monthPoints.toString()
        loginDaysText.text = loginDays.toString()
    }

    private fun navigateMonth(direction: Int) {
        currentCalendar.add(Calendar.MONTH, direction)
        updateMonthDisplay()
        loadMonthData()

        selectedDateInfo.visibility = View.GONE
    }

    private fun onDateSelected(calendarDay: CalendarDay) {
        if (!calendarDay.isInCurrentMonth) {
            val selectedDate = dateKeyFormat.parse(calendarDay.date)
            if (selectedDate != null) {
                currentCalendar.time = selectedDate
                updateMonthDisplay()
                loadMonthData()
            }
            return
        }

        selectedDateInfo.visibility = View.VISIBLE

        val selectedDate = dateKeyFormat.parse(calendarDay.date)
        if (selectedDate != null) {
            selectedDateText.text = displayDateFormat.format(selectedDate)
        }

        if (calendarDay.hasLogin) {
            selectedDatePointsText.text = "🏆 ${calendarDay.pointsEarned} points earned"
            selectedDateTasksText.text = "✅ ${calendarDay.tasksCompleted} tasks completed"
            selectedDatePointsText.visibility = View.VISIBLE
            selectedDateTasksText.visibility = View.VISIBLE
        } else {
            selectedDatePointsText.text = "No activity this day"
            selectedDatePointsText.visibility = View.VISIBLE
            selectedDateTasksText.visibility = View.GONE
        }

        Log.d(
            "CalendarFragment",
            "Selected date: ${calendarDay.date}, Points: ${calendarDay.pointsEarned}"
        )
    }

    companion object {
        fun newInstance(): CalendarFragment {
            return CalendarFragment()
        }
    }
}