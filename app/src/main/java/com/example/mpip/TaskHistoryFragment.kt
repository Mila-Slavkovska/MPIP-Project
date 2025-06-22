package com.example.mpip

import android.app.DatePickerDialog
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mpip.adapters.TaskHistoryAdapter
import com.example.mpip.domain.TaskHistoryFilter
import com.example.mpip.domain.TaskHistoryItem
import com.example.mpip.domain.TaskHistoryStats
import com.example.mpip.repository.Repository
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TaskHistoryFragment : Fragment() {
    private lateinit var repository: Repository
    private val auth = FirebaseAuth.getInstance()

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var loadingLayout: LinearLayout
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var statsLayout: LinearLayout
    private lateinit var filterButton: Button
    private lateinit var backButton: Button

    private lateinit var totalTasksText: TextView
    private lateinit var totalPointsText: TextView
    private lateinit var activeDaysText: TextView
    private lateinit var topEmotionsText: TextView

    private var currentFilter = TaskHistoryFilter()
    private var historyItems = mutableListOf<TaskHistoryItem>()
    private lateinit var historyAdapter: TaskHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = Repository()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_task_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupRecyclerView()
        loadTaskHistory()
    }

    private fun initializeViews(view: View) {
        historyRecyclerView = view.findViewById(R.id.history_recycler)
        loadingLayout = view.findViewById(R.id.loading_layout)
        emptyStateLayout = view.findViewById(R.id.empty_state_layout)
        statsLayout = view.findViewById(R.id.stats_layout)
        filterButton = view.findViewById(R.id.filter_button)
        backButton = view.findViewById(R.id.back_button)

        totalTasksText = view.findViewById(R.id.total_tasks_text)
        totalPointsText = view.findViewById(R.id.total_points_text)
        activeDaysText = view.findViewById(R.id.active_days_text)
        topEmotionsText = view.findViewById(R.id.top_emotions_text)

        filterButton.setOnClickListener {
            showFilterDialog()
        }

        backButton.setOnClickListener {
            (activity as? MainActivity)?.hideDailyTasks()
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = TaskHistoryAdapter(historyItems) { historyItem ->
            showTaskHistoryDetails(historyItem)
        }

        historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }
    }

    private fun loadTaskHistory() {
        val userId = auth.currentUser?.uid ?: return

        showLoading(true)

        lifecycleScope.launch {
            try {
                val items = repository.getQuestionnaireTaskHistory(userId, currentFilter)

                historyItems.clear()
                historyItems.addAll(items)
                historyAdapter.notifyDataSetChanged()

                if (items.isNotEmpty()) {
                    showHistoryList()
                    loadMonthlyStats()
                } else {
                    showEmptyState()
                }

                Log.d("TaskHistoryFragment", "Loaded ${items.size} task history items")

            } catch (e: Exception) {
                Log.e("TaskHistoryFragment", "Error loading task history: ${e.message}")
                Toast.makeText(context, "Error loading history: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                showEmptyState()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun loadMonthlyStats() {
        val userId = auth.currentUser?.uid ?: return
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        lifecycleScope.launch {
            try {
                val stats = repository.getTaskHistoryStats(userId, currentMonth)
                updateStatsDisplay(stats)
            } catch (e: Exception) {
                Log.e("TaskHistoryFragment", "Error loading stats: ${e.message}")
            }
        }
    }

    private fun updateStatsDisplay(stats: TaskHistoryStats) {
        totalTasksText.text = stats.questionnaireTasksCompleted.toString()
        totalPointsText.text = stats.totalPoints.toString()
        activeDaysText.text = stats.activeDays.toString()

        if (stats.topEmotions.isNotEmpty()) {
            val emotionsText =
                stats.topEmotions.take(3).joinToString(", ") { "${it.first} (${it.second})" }
            topEmotionsText.text = "🎭 Top emotions: $emotionsText"
            topEmotionsText.visibility = View.VISIBLE
        } else {
            topEmotionsText.visibility = View.GONE
        }

        Log.d(
            "TaskHistoryFragment",
            "Updated stats: ${stats.questionnaireTasksCompleted} tasks, ${stats.totalPoints} points"
        )
    }

    private fun showTaskHistoryDetails(historyItem: TaskHistoryItem) {
        val task = historyItem.task

        val message = buildString {
            append("${task.title}\n")
            append("${task.description}\n\n")

            append("📅 Completed: ${historyItem.completionDate} at ${historyItem.completionTime}\n")
            append("🏆 Points Earned: ${historyItem.pointsEarned}\n")
            append(
                "🎯 Difficulty: ${
                    task.difficulty.name.lowercase().replaceFirstChar { it.uppercase() }
                }\n\n"
            )

            if (task.triggeringEmotionNames.isNotEmpty()) {
                append("🎭 Emotions: ${task.triggeringEmotionNames.joinToString(", ")}\n")
            }

            if (task.questionnaireRelations.isNotEmpty()) {
                append("🔗 Related to: ${task.questionnaireRelations.joinToString(", ")}\n")
            }

            if (task.questionnaireMemo.isNotEmpty()) {
                append("📝 Your note: \"${task.questionnaireMemo}\"\n")
            }

            if (task.userResponse.isNotEmpty()) {
                append("\n💬 Your response:\n\"${task.userResponse}\"")
            }
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Task Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_history_filter, null)

        val startDateButton = dialogView.findViewById<Button>(R.id.start_date_button)
        val endDateButton = dialogView.findViewById<Button>(R.id.end_date_button)
        val thisWeekButton = dialogView.findViewById<Button>(R.id.this_week_button)
        val thisMonthButton = dialogView.findViewById<Button>(R.id.this_month_button)
        val allTimeButton = dialogView.findViewById<Button>(R.id.all_time_button)
        val minPointsInput = dialogView.findViewById<TextInputEditText>(R.id.min_points_input)
        val maxPointsInput = dialogView.findViewById<TextInputEditText>(R.id.max_points_input)
        val clearFiltersButton = dialogView.findViewById<Button>(R.id.clear_filters_button)
        val emotionChips = dialogView.findViewById<ChipGroup>(R.id.emotion_chips)

        if (currentFilter.startDate.isNotEmpty()) {
            startDateButton.text = currentFilter.startDate
        }
        if (currentFilter.endDate.isNotEmpty()) {
            endDateButton.text = currentFilter.endDate
        }
        if (currentFilter.minPoints > 0) {
            minPointsInput.setText(currentFilter.minPoints.toString())
        }
        if (currentFilter.maxPoints < Int.MAX_VALUE) {
            maxPointsInput.setText(currentFilter.maxPoints.toString())
        }

        thisWeekButton.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
            val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            startDateButton.text = startDate
            endDateButton.text = endDate
        }

        thisMonthButton.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
            val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            startDateButton.text = startDate
            endDateButton.text = endDate
        }

        allTimeButton.setOnClickListener {
            startDateButton.text = "Start Date"
            endDateButton.text = "End Date"
        }

        clearFiltersButton.setOnClickListener {
            startDateButton.text = "Start Date"
            endDateButton.text = "End Date"
            minPointsInput.setText("")
            maxPointsInput.setText("")
            emotionChips.removeAllViews()
        }

        startDateButton.setOnClickListener {
            showDatePicker { date ->
                startDateButton.text = date
            }
        }

        endDateButton.setOnClickListener {
            showDatePicker { date ->
                endDateButton.text = date
            }
        }

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Filter Task History")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                applyFilter(dialogView)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onDateSelected(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun applyFilter(dialogView: View) {
        val startDateButton = dialogView.findViewById<Button>(R.id.start_date_button)
        val endDateButton = dialogView.findViewById<Button>(R.id.end_date_button)
        val minPointsInput = dialogView.findViewById<TextInputEditText>(R.id.min_points_input)
        val maxPointsInput = dialogView.findViewById<TextInputEditText>(R.id.max_points_input)

        val startDate = if (startDateButton.text.toString() != "Start Date") {
            startDateButton.text.toString()
        } else ""

        val endDate = if (endDateButton.text.toString() != "End Date") {
            endDateButton.text.toString()
        } else ""

        val minPoints = minPointsInput.text.toString().toIntOrNull() ?: 0
        val maxPoints = maxPointsInput.text.toString().toIntOrNull() ?: Int.MAX_VALUE

        currentFilter = TaskHistoryFilter(
            startDate = startDate,
            endDate = endDate,
            minPoints = minPoints,
            maxPoints = maxPoints
        )

        Log.d("TaskHistoryFragment", "Applied filter: $currentFilter")
        loadTaskHistory()
    }

    private fun showLoading(show: Boolean) {
        loadingLayout.visibility = if (show) View.VISIBLE else View.GONE
        historyRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        statsLayout.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showHistoryList() {
        emptyStateLayout.visibility = View.GONE
        historyRecyclerView.visibility = View.VISIBLE
        statsLayout.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        emptyStateLayout.visibility = View.VISIBLE
        historyRecyclerView.visibility = View.GONE
        statsLayout.visibility = View.GONE
        showLoading(false)
    }

    companion object {
        fun newInstance(): TaskHistoryFragment {
            return TaskHistoryFragment()
        }
    }
}