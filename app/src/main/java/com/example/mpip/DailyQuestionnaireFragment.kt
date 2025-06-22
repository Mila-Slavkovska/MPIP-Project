package com.example.mpip

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mpip.adapters.EmotionSelectionAdapter
import com.example.mpip.adapters.RelationSelectionAdapter
import com.example.mpip.domain.DailyQuestionnaire
import com.example.mpip.domain.Emotion
import com.example.mpip.domain.EmotionRelation
import com.example.mpip.repository.Repository
import com.example.mpip.service.TaskGenerationService
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DailyQuestionnaireFragment : Fragment() {
    private lateinit var repository: Repository
    private lateinit var taskService: TaskGenerationService
    private val auth = FirebaseAuth.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private lateinit var progressBar: ProgressBar
    private lateinit var stepIndicator: TextView
    private lateinit var questionTitle: TextView
    private lateinit var questionSubtitle: TextView
    private lateinit var contentContainer: LinearLayout
    private lateinit var nextButton: Button
    private lateinit var backButton: Button
    private lateinit var closeButton: ImageButton

    private lateinit var emotionsRecyclerView: RecyclerView
    private lateinit var relationsRecyclerView: RecyclerView
    private lateinit var memoInput: TextInputEditText
    private lateinit var memoLayout: TextInputLayout

    private var currentStep = 1
    private val totalSteps = 3
    private var selectedEmotions = mutableListOf<String>()
    private var selectedRelations = mutableListOf<String>()
    private var memoText = ""

    private var emotions = listOf<Emotion>()
    private var relations = listOf<EmotionRelation>()

    private lateinit var emotionsAdapter: EmotionSelectionAdapter
    private lateinit var relationsAdapter: RelationSelectionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = Repository()
        taskService = TaskGenerationService(repository)
        loadData()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_daily_questionnaire, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupClickListeners()
        if (emotions.isNotEmpty()) {
            setupStep1()
        }
    }

    private fun initializeViews(view: View) {
        progressBar = view.findViewById(R.id.step_progress)
        stepIndicator = view.findViewById(R.id.step_indicator)
        questionTitle = view.findViewById(R.id.question_title)
        questionSubtitle = view.findViewById(R.id.question_subtitle)
        closeButton = view.findViewById(R.id.close_button)

        contentContainer = view.findViewById(R.id.content_container)

        nextButton = view.findViewById(R.id.next_button)
        backButton = view.findViewById(R.id.back_button)

        progressBar.max = totalSteps
        progressBar.progress = currentStep
        updateStepIndicator()
    }

    private fun setupClickListeners() {
        closeButton.setOnClickListener {
            dismissQuestionnaire()
        }

        nextButton.setOnClickListener {
            handleNextStep()
        }

        backButton.setOnClickListener {
            handleBackStep()
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                Log.d("QuestionnaireDebug", "=== STARTING DATA LOAD ===")

                val userId = auth.currentUser?.uid
                if (userId != null) {
                    val hasCompleted = repository.hasCompletedTodayQuestionnaire(userId)
                    if (hasCompleted) {
                        Log.w("QuestionnaireDebug", "Questionnaire already completed today!")
                        Toast.makeText(
                            context,
                            "You've already completed today's check-in!",
                            Toast.LENGTH_LONG
                        ).show()
                        dismissQuestionnaire()
                        return@launch
                    }
                }

                emotions = repository.getActiveEmotions()
                relations = repository.getEmotionRelations()

                Log.d("QuestionnaireDebug", "Emotions loaded: ${emotions.size}")
                Log.d("QuestionnaireDebug", "Relations loaded: ${relations.size}")

                if (emotions.isNotEmpty()) {
                    Log.d(
                        "QuestionnaireDebug",
                        "✅ Setting up step 1 with ${emotions.size} emotions"
                    )
                    setupStep1()
                } else {
                    Log.e("QuestionnaireDebug", "❌ NO ACTIVE EMOTIONS FOUND!")
                    Toast.makeText(
                        context,
                        "No emotions available - check Firebase data",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("QuestionnaireDebug", "❌ ERROR loading data: ${e.message}", e)
                Toast.makeText(context, "Error loading data: ${e.message}", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun setupStep1() {
        Log.d("QuestionnaireDebug", "Setting up step 1 with ${emotions.size} emotions")

        currentStep = 1
        updateStepIndicator()

        questionTitle.text = "How are you feeling today?"
        questionSubtitle.text = "Select all emotions that describe how you feel right now"

        contentContainer.removeAllViews()

        emotionsRecyclerView = RecyclerView(requireContext()).apply {
            layoutManager = GridLayoutManager(context, 2)
            setPadding(16, 16, 16, 16)

            isNestedScrollingEnabled = false

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        Log.d("QuestionnaireDebug", "Creating adapter with ${emotions.size} emotions")
        emotionsAdapter = EmotionSelectionAdapter(emotions) { emotion, isSelected ->
            Log.d("QuestionnaireDebug", "Emotion clicked: ${emotion.name}, selected: $isSelected")
            if (isSelected) {
                selectedEmotions.add(emotion.id)
            } else {
                selectedEmotions.remove(emotion.id)
            }
            updateNextButton()
        }

        emotionsRecyclerView.adapter = emotionsAdapter
        contentContainer.addView(emotionsRecyclerView)

        Log.d("QuestionnaireDebug", "RecyclerView added to content container")

        backButton.visibility = View.GONE
        nextButton.text = "Next"
        updateNextButton()

        Log.d("QuestionnaireDebug", "Button visibility: ${nextButton.visibility}")
        Log.d("QuestionnaireDebug", "Button enabled: ${nextButton.isEnabled}")
        Log.d("QuestionnaireDebug", "Button text: ${nextButton.text}")
    }

    private fun setupStep2() {
        currentStep = 2
        updateStepIndicator()

        questionTitle.text = "What are these feelings related to?"
        questionSubtitle.text = "Choose what's affecting your emotions today"

        contentContainer.removeAllViews()

        relationsRecyclerView = RecyclerView(requireContext()).apply {
            layoutManager = GridLayoutManager(context, 1)
            setPadding(16, 16, 16, 16)
        }

        relationsAdapter = RelationSelectionAdapter(relations) { relation, isSelected ->
            if (isSelected) {
                selectedRelations.add(relation.id)
            } else {
                selectedRelations.remove(relation.id)
            }
            updateNextButton()
        }

        relationsRecyclerView.adapter = relationsAdapter
        contentContainer.addView(relationsRecyclerView)

        backButton.visibility = View.VISIBLE
        nextButton.text = "Next"
        updateNextButton()
    }

    private fun setupStep3() {
        currentStep = 3
        updateStepIndicator()

        questionTitle.text = "Anything else on your mind?"
        questionSubtitle.text = "Share your thoughts or leave it blank (optional)"

        contentContainer.removeAllViews()

        val padding = 32

        memoLayout = TextInputLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(padding, padding, padding, padding / 2)
            }
            hint = "Write your thoughts here..."
            boxStrokeColor = resources.getColor(android.R.color.holo_blue_bright, null)
        }

        memoInput = TextInputEditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            minLines = 3
            maxLines = 6
            setText(memoText)
        }

        memoLayout.addView(memoInput)
        contentContainer.addView(memoLayout)

        backButton.visibility = View.VISIBLE
        nextButton.text = "Complete Check-in"
        nextButton.isEnabled = true
    }

    private fun handleNextStep() {
        when (currentStep) {
            1 -> {
                if (selectedEmotions.isNotEmpty()) {
                    setupStep2()
                }
            }

            2 -> {
                if (selectedRelations.isNotEmpty()) {
                    setupStep3()
                }
            }

            3 -> {
                memoText = memoInput.text.toString().trim()
                completeQuestionnaire()
            }
        }
    }

    private fun handleBackStep() {
        when (currentStep) {
            2 -> setupStep1()
            3 -> setupStep2()
        }
    }

    private fun updateStepIndicator() {
        stepIndicator.text = "Step $currentStep of $totalSteps"
        progressBar.progress = currentStep
    }

    private fun updateNextButton() {
        val shouldEnable = when (currentStep) {
            1 -> selectedEmotions.isNotEmpty()
            2 -> selectedRelations.isNotEmpty()
            3 -> true // Memo is optional
            else -> false
        }

        Log.d(
            "QuestionnaireDebug",
            "updateNextButton: step=$currentStep, selectedEmotions=${selectedEmotions.size}, shouldEnable=$shouldEnable"
        )

        nextButton.isEnabled = shouldEnable

        when (currentStep) {
            1 -> {
                nextButton.text = if (shouldEnable) "Next" else "Next (Select emotions first)"
            }

            2 -> {
                nextButton.text = if (shouldEnable) "Next" else "Next (Select relations first)"
            }

            3 -> {
                nextButton.text = "Complete Check-in"
            }
        }

        Log.d(
            "QuestionnaireDebug",
            "Button enabled: ${nextButton.isEnabled}, text: ${nextButton.text}"
        )
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun completeQuestionnaire() {
        val userId = auth.currentUser?.uid ?: return
        val today = dateFormat.format(Date())

        lifecycleScope.launch {
            try {
                nextButton.isEnabled = false
                nextButton.text = "Saving..."

                val selectedEmotionNames = emotions.filter { emotion ->
                    selectedEmotions.contains(emotion.id)
                }.map { it.name }

                val selectedRelationNames = relations.filter { relation ->
                    selectedRelations.contains(relation.id)
                }.map { it.name }

                val questionnaire = DailyQuestionnaire(
                    userId = userId,
                    date = today,
                    timestamp = System.currentTimeMillis(),
                    selectedEmotions = selectedEmotions,
                    emotionRelations = selectedRelations,
                    memo = memoText,
                    completed = true,
                    selectedEmotionNames = selectedEmotionNames,
                    selectedRelationNames = selectedRelationNames,
                    completedAt = System.currentTimeMillis()
                )

                Log.d("QuestionnaireDebug", "=== SAVING QUESTIONNAIRE ===")
                Log.d("QuestionnaireDebug", "Emotions: $selectedEmotionNames")
                Log.d("QuestionnaireDebug", "Relations: $selectedRelationNames")
                Log.d("QuestionnaireDebug", "Memo: $memoText")

                val saved = repository.saveDailyQuestionnaire(questionnaire)
                Log.d("QuestionnaireDebug", "Questionnaire saved: $saved")

                if (saved) {
                    Log.d("QuestionnaireDebug", "=== GENERATING QUESTIONNAIRE TASKS ===")
                    val questionnaireTasks = taskService.generateDailyTasks(userId, questionnaire)

                    Log.d(
                        "QuestionnaireDebug",
                        "Generated ${questionnaireTasks.size} questionnaire tasks:"
                    )
                    questionnaireTasks.forEach { task ->
                        Log.d("QuestionnaireDebug", "- ${task.title} (${task.category})")
                    }

                    val tasksSaved = repository.saveDailyTasks(questionnaireTasks)
                    Log.d("QuestionnaireDebug", "Tasks saved: $tasksSaved")

                    if (tasksSaved) {
                        Log.d(
                            "QuestionnaireDebug",
                            "✅ Generated ${questionnaireTasks.size} questionnaire tasks"
                        )
                        showCompletionMessage(questionnaireTasks.size)
                    } else {
                        Log.e("QuestionnaireDebug", "❌ Failed to save questionnaire tasks")
                        Toast.makeText(
                            context,
                            "Failed to generate personalized tasks",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.e("QuestionnaireDebug", "❌ Failed to save questionnaire")
                    Toast.makeText(context, "Failed to save questionnaire", Toast.LENGTH_SHORT)
                        .show()
                }

            } catch (e: Exception) {
                Log.e("QuestionnaireDebug", "❌ Error completing questionnaire: ${e.message}", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                nextButton.isEnabled = true
                nextButton.text = "Complete Check-in"
            }
        }
    }

    private fun showCompletionMessage(taskCount: Int) {
        val message = "✅ Daily check-in complete!\n\n" +
                "I've prepared $taskCount personalized tasks based on how you're feeling today. " +
                "These will be added to your daily routine tasks.\n\n" +
                "Let's take care of yourself and your pet! 🐾"

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Check-in Complete!")
            .setMessage(message)
            .setPositiveButton("View All Tasks") { _, _ ->
                dismissAndShowTasks()
            }
            .setNegativeButton("Later") { _, _ ->
                dismissQuestionnaire()
            }
            .setCancelable(false)
            .create()

        dialog.show()
    }

    private fun dismissAndShowTasks() {
        // TODO: Show tasks UI or navigate to tasks screen
        dismissQuestionnaire()
    }

    private fun dismissQuestionnaire() {
        parentFragmentManager.beginTransaction()
            .remove(this)
            .commit()

        (activity as? MainActivity)?.onQuestionnaireCompleted()
    }

    companion object {
        fun newInstance(): DailyQuestionnaireFragment {
            return DailyQuestionnaireFragment()
        }
    }
}