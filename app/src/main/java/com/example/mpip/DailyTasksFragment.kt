package com.example.mpip

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mpip.adapters.DailyTasksAdapter
import com.example.mpip.domain.DailyTask
import com.example.mpip.domain.enums.tasks.TaskAction
import com.example.mpip.domain.enums.tasks.TaskCategory
import com.example.mpip.domain.enums.tasks.TaskType
import com.example.mpip.repository.Repository
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class DailyTasksFragment : Fragment() {
    private lateinit var repository: Repository
    private val auth = FirebaseAuth.getInstance()

    private lateinit var headerLayout: LinearLayout
    private lateinit var todayDateText: TextView
    private lateinit var progressText: TextView
    private lateinit var pointsText: TextView
    private lateinit var tasksRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var loadingLayout: LinearLayout

    private var currentTasks = mutableListOf<DailyTask>()
    private lateinit var tasksAdapter: DailyTasksAdapter
    private var currentPhotoTaskId: String? = null
    private var photoUri: Uri? = null

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(
                context,
                "Camera permission is needed for photo tasks",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoTaskId != null) {
            completePhotoTask(currentPhotoTaskId!!, photoUri.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = Repository()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_daily_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupRecyclerView()
        loadTodayTasks()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            (activity as? MainActivity)?.hideDailyTasks()
        }
    }

    private fun initializeViews(view: View) {
        try {
            val backButton: ImageButton = view.findViewById(R.id.back_button)
            backButton.setOnClickListener {
                (activity as? MainActivity)?.hideDailyTasks()
            }
        } catch (e: Exception) {
            Log.d(
                "DailyTasksFragment",
                "Back button not found in layout, will use alternative navigation"
            )
        }
        headerLayout = view.findViewById(R.id.header_layout)
        todayDateText = view.findViewById(R.id.today_date)
        progressText = view.findViewById(R.id.progress_text)
        pointsText = view.findViewById(R.id.points_text)
        tasksRecyclerView = view.findViewById(R.id.tasks_recycler)
        emptyStateLayout = view.findViewById(R.id.empty_state_layout)
        loadingLayout = view.findViewById(R.id.loading_layout)

        val today = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
        todayDateText.text = "Today - $today"
    }

    private fun setupRecyclerView() {
        tasksAdapter = DailyTasksAdapter(currentTasks) { task, action ->
            handleTaskAction(task, action)
        }

        tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tasksAdapter
        }
    }

    private fun loadTodayTasks() {
        val userId = auth.currentUser?.uid ?: return

        showLoading(true)

        lifecycleScope.launch {
            try {
                val allTasks = repository.getTodayTasks(userId)
                val dailyRoutineTasks =
                    allTasks.filter { it.category == TaskCategory.DAILY_ROUTINE }
                val questionnaireTasks =
                    allTasks.filter { it.category == TaskCategory.QUESTIONNAIRE_BASED }

                Log.d("DailyTasksFragment", "=== TASK BREAKDOWN ===")
                Log.d("DailyTasksFragment", "Daily routine tasks: ${dailyRoutineTasks.size}")
                Log.d("DailyTasksFragment", "Questionnaire tasks: ${questionnaireTasks.size}")
                Log.d("DailyTasksFragment", "Total tasks: ${allTasks.size}")

                if (allTasks.isNotEmpty()) {
                    currentTasks.clear()

                    val sortedTasks = dailyRoutineTasks.sortedBy { it.points } +
                            questionnaireTasks.sortedByDescending { it.points }

                    currentTasks.addAll(sortedTasks)
                    tasksAdapter.notifyDataSetChanged()

                    updateProgressDisplay()
                    showTasksList()
                } else {
                    showEmptyState()
                }

            } catch (e: Exception) {
                Log.e("DailyTasksFragment", "Error loading tasks: ${e.message}", e)
                Toast.makeText(context, "Error loading tasks: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                showEmptyState()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun handleTaskAction(task: DailyTask, action: TaskAction) {
        when (action) {
            TaskAction.COMPLETE_SIMPLE -> completeSimpleTask(task)
            TaskAction.COMPLETE_TEXT -> showTextCompletionDialog(task)
            TaskAction.COMPLETE_PHOTO -> startPhotoTask(task)
            TaskAction.VIEW_DETAILS -> showTaskDetails(task)
        }
    }

    private fun completeSimpleTask(task: DailyTask) {
        lifecycleScope.launch {
            val success = repository.completeTask(task.id)

            if (success) {
                markTaskCompleted(task)
                showCompletionFeedback(task)
                updatePetProgress(task.points)
            } else {
                Toast.makeText(context, "Failed to complete task", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTextCompletionDialog(task: DailyTask) {
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.dialog_text_task_completion, null)

        val titleText = dialogView.findViewById<TextView>(R.id.task_title)
        val descriptionText = dialogView.findViewById<TextView>(R.id.task_description)
        val textInput = dialogView.findViewById<TextInputEditText>(R.id.response_input)
        val wordCountText = dialogView.findViewById<TextView>(R.id.word_count)

        titleText.text = task.title

        val enhancedDescription = buildString {
            append(task.description)

            if (task.category == TaskCategory.QUESTIONNAIRE_BASED && task.triggeringEmotionNames.isNotEmpty()) {
                append(
                    "\n\n💡 This task is personalized for your emotions today: ${
                        task.triggeringEmotionNames.joinToString(
                            ", "
                        )
                    }"
                )
            }
        }
        descriptionText.text = enhancedDescription

        textInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val text = s.toString().trim()
                val wordCount = if (text.isEmpty()) 0 else text.split("\\s+".toRegex()).size
                wordCountText.text = "$wordCount words"
            }
        })

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Complete Task") { _, _ ->
                val response = textInput.text.toString().trim()
                if (response.isNotEmpty()) {
                    completeTextTask(task, response)
                } else {
                    Toast.makeText(context, "Please write something first", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
        textInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled =
                    s.toString().trim().isNotEmpty()
            }
        })
    }

    private fun completeTextTask(task: DailyTask, response: String) {
        lifecycleScope.launch {
            val success = repository.completeTask(task.id, response)

            if (success) {
                markTaskCompleted(task)
                showCompletionFeedback(task)
                updatePetProgress(task.points)
            } else {
                Toast.makeText(context, "Failed to complete task", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startPhotoTask(task: DailyTask) {
        currentPhotoTaskId = task.id
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }

            else -> {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        try {
            val photoFile = File(
                requireContext().getExternalFilesDir(null),
                "task_photo_${System.currentTimeMillis()}.jpg"
            )

            photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )

            takePicture.launch(photoUri)

        } catch (e: Exception) {
            Toast.makeText(context, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun completePhotoTask(taskId: String, photoPath: String) {
        lifecycleScope.launch {
            val success = repository.completeTask(taskId, "", photoPath)

            if (success) {
                val task = currentTasks.find { it.id == taskId }
                if (task != null) {
                    markTaskCompleted(task)
                    showCompletionFeedback(task)
                    updatePetProgress(task.points)
                }
            } else {
                Toast.makeText(context, "Failed to complete task", Toast.LENGTH_SHORT).show()
            }
        }

        currentPhotoTaskId = null
        photoUri = null
    }

    private fun showTaskDetails(task: DailyTask) {
        val message = buildString {
            append("${task.description}\n\n")
            append("Points: ${task.points}\n")
            append("Category: ${if (task.category == TaskCategory.QUESTIONNAIRE_BASED) "Personal Task" else "Daily Routine"}\n")
            append(
                "Difficulty: ${
                    task.difficulty.name.lowercase().replaceFirstChar { it.uppercase() }
                }\n"
            )

            if (task.category == TaskCategory.QUESTIONNAIRE_BASED) {
                append("\n--- Personal Context ---\n")

                if (task.triggeringEmotionNames.isNotEmpty()) {
                    append("Based on emotions: ${task.triggeringEmotionNames.joinToString(", ")}\n")
                }

                if (task.questionnaireRelations.isNotEmpty()) {
                    append("Related to: ${task.questionnaireRelations.joinToString(", ")}\n")
                }

                if (task.questionnaireMemo.isNotEmpty()) {
                    append("Your note: \"${task.questionnaireMemo}\"\n")
                }

                append("\nThis task was created based on your daily check-in.")
            } else {
                append("\nThis is a daily routine task that helps maintain healthy habits.")
            }

            if (task.completed && task.completedAt > 0) {
                val completedTime =
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(task.completedAt))
                append("\n\n✅ Completed at $completedTime")

                if (task.userResponse.isNotEmpty()) {
                    append("\nYour response: \"${task.userResponse}\"")
                }
            }
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle(task.title)
            .setMessage(message)
            .setPositiveButton("Got it", null)
            .show()
    }

    private fun markTaskCompleted(task: DailyTask) {
        val index = currentTasks.indexOf(task)
        if (index != -1) {
            currentTasks[index] = task.copy(
                completed = true,
                completedAt = System.currentTimeMillis()
            )
            tasksAdapter.notifyItemChanged(index)
            updateProgressDisplay()

            Log.d("DailyTasksFragment", "Task marked as completed locally: ${task.title}")
        }
    }

    private fun showCompletionFeedback(task: DailyTask) {
        val message = when (task.type) {
            TaskType.TEXT -> "Great job writing! ✍️"
            TaskType.PHOTO -> "Beautiful photo captured! 📸"
            TaskType.SIMPLE_ACTION -> if (task.category == TaskCategory.DAILY_ROUTINE) {
                "Daily habit completed! ⭐"
            } else {
                "Personal task completed! 🌟"
            }
        }

        val categoryBonus = if (task.category == TaskCategory.QUESTIONNAIRE_BASED) {
            " (Personal task bonus!)"
        } else {
            ""
        }

        Toast.makeText(
            context,
            "$message +${task.points} points$categoryBonus",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun updatePetProgress(pointsEarned: Int) {
        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            repository.updateUserProgress(userId, pointsEarned)
        }
        (activity as? MainActivity)?.onTaskCompleted(pointsEarned)
    }

    private fun updateProgressDisplay() {
        val dailyTasks = currentTasks.filter { it.category == TaskCategory.DAILY_ROUTINE }
        val questionnaireTasks =
            currentTasks.filter { it.category == TaskCategory.QUESTIONNAIRE_BASED }

        val completedDaily = dailyTasks.count { it.completed }
        val completedQuestionnaire = questionnaireTasks.count { it.completed }
        val totalCompleted = completedDaily + completedQuestionnaire
        val totalTasks = currentTasks.size

        val totalPoints = currentTasks.filter { it.completed }.sumOf { it.points }

        progressText.text = if (questionnaireTasks.isNotEmpty()) {
            "$totalCompleted of $totalTasks tasks completed (Daily: $completedDaily/${dailyTasks.size}, Personal: $completedQuestionnaire/${questionnaireTasks.size})"
        } else {
            "$completedDaily of ${dailyTasks.size} daily tasks completed"
        }

        pointsText.text = "$totalPoints points earned today"

        Log.d("DailyTasksFragment", "Progress updated: $totalCompleted/$totalTasks completed")
    }

    private fun showLoading(show: Boolean) {
        loadingLayout.visibility = if (show) View.VISIBLE else View.GONE
        tasksRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        headerLayout.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showTasksList() {
        emptyStateLayout.visibility = View.GONE
        tasksRecyclerView.visibility = View.VISIBLE
        headerLayout.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        emptyStateLayout.visibility = View.VISIBLE
        tasksRecyclerView.visibility = View.GONE
        headerLayout.visibility = View.GONE
        showLoading(false)
    }

    companion object {
        fun newInstance(): DailyTasksFragment {
            return DailyTasksFragment()
        }
    }
}