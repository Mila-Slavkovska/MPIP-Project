package com.example.mpip

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.mpip.domain.UserProgress
import com.example.mpip.domain.enums.PetActionType
import com.example.mpip.domain.mentalHealthTips.MentalHealthTip
import com.example.mpip.repository.Repository
import com.example.mpip.service.TaskGenerationService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var auth: FirebaseAuth

    private lateinit var petNameText: TextView
    private lateinit var petImage: ImageView
    private lateinit var happinessBar: ProgressBar
    private lateinit var energyBar: ProgressBar
    private lateinit var happinessText: TextView
    private lateinit var energyText: TextView
    private lateinit var levelText: TextView
    private lateinit var feedButton: Button
    private lateinit var playButton: Button
    private lateinit var careButton: Button
    private lateinit var repository: Repository

    private var tasksFragment: DailyTasksFragment? = null
    private lateinit var taskService: TaskGenerationService
    private lateinit var taskMenuComponent: TaskMenuComponent

    private lateinit var pointsDisplay: TextView
    private lateinit var streakDisplay: TextView
    private var currentUserProgress: UserProgress? = null

    private var taskHistoryFragment: TaskHistoryFragment? = null

    private var calendarFragment: CalendarFragment? = null

    private var diaryFragment: DiaryFragment? = null

    private lateinit var mentalHealthTipsCard: LinearLayout
    private lateinit var tipStatusText: TextView
    private lateinit var tipPointsBadge: TextView
    private lateinit var tipPreviewText: TextView
    private lateinit var tipCategoryText: TextView
    private lateinit var viewTipButton: Button
    private var currentMentalHealthTip: MentalHealthTip? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivityFlow", "=== MainActivity onCreate() started ===")

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Log.e("MainActivityFlow", "No authenticated user found")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        Log.d("MainActivityFlow", "User authenticated: ${currentUser.email}")

        //checkAndInitializeFirebaseData()

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val userSpecificKey = "PetPrefs_${currentUser.uid}"
        sharedPrefs = getSharedPreferences(userSpecificKey, MODE_PRIVATE)
        Log.d("MainActivityFlow", "Using preferences: $userSpecificKey")

        val hasPet = sharedPrefs.getBoolean("pet_selected", false)
        Log.d("MainActivityFlow", "User has pet: $hasPet")

        initializeViews()
        repository = Repository()
        taskService = TaskGenerationService(repository)
        performDailyPetReset()
        checkDailyCheckInStatus()
        initializeDailyTasksAndStatus()
        setupClickListeners()
        setupTaskMenu() // ADD THIS LINE
        loadTodaysMentalHealthTip()

        if (hasPet) {
            Log.d("MainActivityFlow", "Loading existing pet data")
            loadPetData()

            val newPetCreated = intent.getBooleanExtra("new_pet_created", false)
            if (newPetCreated) {
                Log.d("MainActivityFlow", "New pet created, showing welcome message")
                showNewPetWelcome()
            }
        } else {
            Log.d("MainActivityFlow", "No pet found - redirecting to pet selection")
            redirectToPetSelection()
        }

        Log.d("MainActivityFlow", "=== MainActivity setup complete ===")
    }

    private fun initializeViews() {
        Log.d("MainActivityFlow", "Initializing views...")

        try {
            val userProfileBtn: ImageButton = findViewById(R.id.user_profile)

            petNameText = findViewById(R.id.pet_name)
            petImage = findViewById(R.id.pet_image)
            happinessBar = findViewById(R.id.happiness_bar)
            energyBar = findViewById(R.id.energy_bar)
            happinessText = findViewById(R.id.happiness_text)
            energyText = findViewById(R.id.energy_text)
            levelText = findViewById(R.id.level_text)

            feedButton = findViewById(R.id.feed_button)
            playButton = findViewById(R.id.play_button)
            careButton = findViewById(R.id.care_button)

            pointsDisplay = findViewById(R.id.points_display)
            streakDisplay = findViewById(R.id.streak_display)

            mentalHealthTipsCard = findViewById(R.id.mental_health_tips_card)
            tipStatusText = findViewById(R.id.tip_status_text)
            tipPointsBadge = findViewById(R.id.tip_points_badge)
            tipPreviewText = findViewById(R.id.tip_preview_text)
            tipCategoryText = findViewById(R.id.tip_category_text)
            viewTipButton = findViewById(R.id.view_tip_button)

            Log.d("MainActivityFlow", "All views found successfully")

        } catch (e: Exception) {
            Log.e("MainActivityFlow", "Failed to initialize views", e)
            Toast.makeText(this, "View error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeDailyTasksAndStatus() {
        val currentUser = auth.currentUser ?: return

        lifecycleScope.launch {
            try {
                val hasDailyTasks = repository.hasDailyRoutineTasksForToday(currentUser.uid)
                if (!hasDailyTasks) {
                    Log.d("MainActivityFlow", "Creating daily routine tasks for today")
                    val dailyTasks = taskService.generateDailyRoutineTasks(currentUser.uid)
                    repository.saveDailyRoutineTasks(dailyTasks)
                }

                val hasCompletedQuestionnaire =
                    repository.hasCompletedTodayQuestionnaire(currentUser.uid)
                updateTaskMenuStatus()


            } catch (e: Exception) {
                Log.e("MainActivityFlow", "Error initializing tasks: ${e.message}")
            }
        }
    }

    private fun setupClickListeners() {
        Log.d("MainActivityFlow", "Setting up click listeners...")

        try {
            val userProfileBtn: ImageButton = findViewById(R.id.user_profile)
            userProfileBtn.setOnClickListener {
                Log.d("MainActivityFlow", "Profile button clicked")
                val intent = Intent(applicationContext, UserProfileActivity::class.java)
                startActivity(intent)
            }

            feedButton.setOnClickListener { feedPet() }
            playButton.setOnClickListener { playWithPet() }
            careButton.setOnClickListener { carePet() }
            petImage.setOnClickListener { petTapped() }

            pointsDisplay.setOnClickListener {
                showUserStatsDialog()
            }

            streakDisplay.setOnLongClickListener {
                showTaskHistory()
                true
            }

            viewTipButton.setOnClickListener {
                showMentalHealthTipDialog()
            }

        } catch (e: Exception) {
            Log.e("MainActivityFlow", "Failed to setup click listeners", e)
        }
    }

    private fun loadTodaysMentalHealthTip() {
        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                Log.d("MainActivityFlow", "Loading today's mental health tip...")

                var todaysTip = repository.getTodaysMentalHealthTip(userId)

                if (todaysTip == null) {
                    Log.d("MainActivityFlow", "No tip found for today, generating new one...")
                    todaysTip = repository.generateTodaysMentalHealthTip(userId)
                }

                if (todaysTip != null) {
                    currentMentalHealthTip = todaysTip
                    updateMentalHealthTipUI(todaysTip)
                    Log.d("MainActivityFlow", "Mental health tip loaded: ${todaysTip.title}")
                } else {
                    Log.e("MainActivityFlow", "Failed to load or generate mental health tip")
                    hideMentalHealthTipCard()
                }

            } catch (e: Exception) {
                Log.e("MainActivityFlow", "Error loading mental health tip: ${e.message}")
                hideMentalHealthTipCard()
            }
        }
    }

    private fun updateMentalHealthTipUI(tip: MentalHealthTip) {
        Log.d("MainActivityFlow", "=== TIP UI UPDATE ===")
        Log.d("MainActivityFlow", "Tip ID: ${tip.id}")
        Log.d("MainActivityFlow", "Is Viewed: ${tip.isViewed}")
        Log.d("MainActivityFlow", "Points Awarded: ${tip.pointsAwarded}")
        Log.d("MainActivityFlow", "Viewed At: ${tip.viewedAt}")

        tip.syncFields()

        tipPreviewText.text = tip.preview
        tipCategoryText.text =
            "${tip.category.emoji} ${tip.category.displayName} • ${tip.difficulty.emoji} ${tip.difficulty.displayName}"

        val hasBeenViewed = tip.isViewed

        if (hasBeenViewed) {
            Log.d("MainActivityFlow", "Setting UI to 'Read' state")
            tipStatusText.text = "Read today ✓"
            tipPointsBadge.text = "Read"
            tipPointsBadge.setBackgroundColor(0xFFE8F5E8.toInt())
            tipPointsBadge.setTextColor(0xFF00B894.toInt())
            viewTipButton.text = "Read Again"
        } else {
            Log.d("MainActivityFlow", "Setting UI to 'New' state")
            if (tip.isPersonalized) {
                tipStatusText.text = "Personalized for you"
            } else {
                tipStatusText.text = "Today's wellness tip"
            }
            tipPointsBadge.text = "+${MentalHealthTip.FIRST_VIEW_POINTS} pts"
            tipPointsBadge.setBackgroundColor(0xFFFFF4E6.toInt())
            tipPointsBadge.setTextColor(0xFFE17055.toInt())
            viewTipButton.text = "View Tip"
        }

        mentalHealthTipsCard.visibility = View.VISIBLE
    }

    private fun hideMentalHealthTipCard() {
        mentalHealthTipsCard.visibility = View.GONE
    }

    private fun showMentalHealthTipDialog() {
        val tip = currentMentalHealthTip ?: return

        val message = buildString {
            append("${tip.content}\n\n")

            if (tip.isPersonalized && tip.basedOnEmotions.isNotEmpty()) {
                append(
                    "💡 This tip was personalized based on your recent emotions: ${
                        tip.basedOnEmotions.joinToString(
                            ", "
                        )
                    }\n\n"
                )
            }

            append("Category: ${tip.category.emoji} ${tip.category.displayName}\n")
            append("Difficulty: ${tip.difficulty.emoji} ${tip.difficulty.displayName}\n")
            append("Estimated time: ${tip.difficulty.timeEstimate}\n")

            if (tip.isPersonalized) {
                append("\n🤖 This tip was generated specifically for you based on your recent activity.")
            }
        }

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("${tip.category.emoji} ${tip.title}")
            .setMessage(message)
            .setPositiveButton("Got it!") { _, _ ->
                markTipAsViewed(tip)
            }
            .setNeutralButton("Save for Later") { _, _ ->
                Toast.makeText(this, "Tip saved! 💾", Toast.LENGTH_SHORT).show()
            }
            .create()

        dialog.show()
    }

    private fun markTipAsViewed(tip: MentalHealthTip) {
        val userId = auth.currentUser?.uid ?: return

        if (tip.isViewed) {
            Log.d("MainActivityFlow", "Tip already viewed, no points awarded: ${tip.id}")
            Toast.makeText(this, "You've already read this tip today! 📖", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("MainActivityFlow", "Marking tip as viewed: ${tip.id}")

        lifecycleScope.launch {
            try {
                val success = repository.markTipAsViewed(tip.id)

                if (success) {
                    repository.updateUserProgressWithPoints(
                        userId,
                        MentalHealthTip.FIRST_VIEW_POINTS,
                        false
                    )

                    currentMentalHealthTip = tip.copy(
                        isViewed = true,
                        pointsAwarded = true,
                        viewedAt = System.currentTimeMillis()
                    ).also { updatedTip ->
                        updatedTip.syncFields()
                    }

                    updateMentalHealthTipUI(currentMentalHealthTip!!)

                    Toast.makeText(
                        this@MainActivity,
                        "Great job reading your tip! +${MentalHealthTip.FIRST_VIEW_POINTS} points 🧠",
                        Toast.LENGTH_LONG
                    ).show()

                    Log.d(
                        "MainActivityFlow",
                        "Tip marked as viewed and ${MentalHealthTip.FIRST_VIEW_POINTS} points awarded"
                    )
                    loadUserProgress()

                    val updatedTip = repository.getTodaysMentalHealthTip(userId)
                    if (updatedTip != null) {
                        currentMentalHealthTip = updatedTip
                        Log.d(
                            "MainActivityFlow",
                            "Reloaded tip from Firebase - isViewed: ${updatedTip.isViewed}}"
                        )
                    }

                } else {
                    Log.e("MainActivityFlow", "Failed to mark tip as viewed")
                    Toast.makeText(
                        this@MainActivity,
                        "Error saving progress. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("MainActivityFlow", "Error marking tip as viewed: ${e.message}")
                Toast.makeText(
                    this@MainActivity,
                    "Error saving progress. Please try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun setupTaskMenu() {
        val taskMenuView = findViewById<View>(R.id.task_menu_header).parent as View
        taskMenuComponent = TaskMenuComponent(taskMenuView)

        taskMenuComponent.onDailyCheckinClick = {
            showDailyQuestionnaire()
        }

        taskMenuComponent.onDailyTasksClick = {
            showDailyTasks()
        }

        taskMenuComponent.onTaskHistoryClick = {
            showTaskHistory()
        }

        taskMenuComponent.onProgressCalendarClick = {
            showProgressCalendar()
        }

        taskMenuComponent.onPersonalDiaryClick = {
            showPersonalDiary()
        }
    }

    private fun showPersonalDiary() {
        Log.d("MainActivityFlow", "Showing personal diary")

        if (diaryFragment == null) {
            diaryFragment = DiaryFragment.newInstance()
        }

        findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main_content_container).visibility =
            View.GONE
        findViewById<FrameLayout>(R.id.tasks_container).visibility = View.VISIBLE

        supportFragmentManager.beginTransaction()
            .replace(R.id.tasks_container, diaryFragment!!)
            .addToBackStack("diary")
            .commit()
    }

    private fun showProgressCalendar() {
        Log.d("MainActivityFlow", "Showing progress calendar")

        if (calendarFragment == null) {
            calendarFragment = CalendarFragment.newInstance()
        }

        findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main_content_container).visibility =
            View.GONE
        findViewById<FrameLayout>(R.id.tasks_container).visibility = View.VISIBLE

        supportFragmentManager.beginTransaction()
            .replace(R.id.tasks_container, calendarFragment!!)
            .addToBackStack("calendar")
            .commit()
    }

    private fun showTaskHistory() {
        taskMenuComponent.forceCollapse()
        Log.d("MainActivityFlow", "Showing task history")

        if (taskHistoryFragment == null) {
            taskHistoryFragment = TaskHistoryFragment.newInstance()
        }

        findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main_content_container).visibility =
            View.GONE
        findViewById<FrameLayout>(R.id.tasks_container).visibility = View.VISIBLE

        supportFragmentManager.beginTransaction()
            .replace(R.id.tasks_container, taskHistoryFragment!!)
            .addToBackStack("task_history")
            .commit()
    }

    // NEW: Show user stats dialog
    private fun showUserStatsDialog() {
        val progress = currentUserProgress ?: return

        val message = buildString {
            append("💰 Available Points: ${progress.availablePoints}\n")
            append("🏆 Total Points Earned: ${progress.totalPoints}\n")
            append("💸 Total Points Spent: ${progress.totalPointsSpent}\n")
            append("🔥 Current Streak: ${progress.currentStreak} days\n")
            append("📈 Longest Streak: ${progress.longestStreak} days\n")
            append("✅ Tasks Completed: ${progress.tasksCompleted}\n")
            append("🎮 Pet Interactions: ${progress.petInteractions}\n")
            append("📊 Level: ${progress.level}")
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("Your Progress")
            .setMessage(message)
            .setPositiveButton("Task History") { _, _ ->
                showTaskHistory()
            }
            .setNegativeButton("OK", null)
            .show()
    }

    private fun showDailyTasks() {
        taskMenuComponent.forceCollapse()
        if (tasksFragment == null) {
            tasksFragment = DailyTasksFragment.newInstance()
        }

        findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main_content_container).visibility =
            View.GONE
        findViewById<FrameLayout>(R.id.tasks_container).visibility = View.VISIBLE

        supportFragmentManager.beginTransaction()
            .replace(R.id.tasks_container, tasksFragment!!)
            .addToBackStack("tasks")
            .commit()
    }

    fun hideDailyTasks() {

        findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main_content_container).visibility =
            View.VISIBLE
        findViewById<FrameLayout>(R.id.tasks_container).visibility = View.GONE

        val currentFragment = supportFragmentManager.findFragmentById(R.id.tasks_container)
        if (currentFragment != null) {
            supportFragmentManager.beginTransaction()
                .remove(currentFragment)
                .commit()
        }

        tasksFragment = null
        taskHistoryFragment = null
        calendarFragment = null
        diaryFragment = null

        lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                updateTaskMenuStatus()
                Log.d("MainActivityFlow", "Returned from tasks/history/calendar - refreshed UI")
            } catch (e: Exception) {
                Log.e("MainActivityFlow", "Error refreshing UI: ${e.message}")
            }
        }
    }

    fun onTaskCompleted(pointsEarned: Int) {
        val currentUser = auth.currentUser ?: return

        lifecycleScope.launch {
            repository.updateUserProgressWithPoints(currentUser.uid, pointsEarned, true)
        }

        val currentHappiness = sharedPrefs.getInt("pet_happiness", 100)
        val currentEnergy = sharedPrefs.getInt("pet_energy", 100)

        val happinessBonus = when {
            pointsEarned >= 25 -> 15
            pointsEarned >= 15 -> 10
            else -> 5
        }

        val energyBonus = happinessBonus / 2

        val newHappiness = minOf(100, currentHappiness + happinessBonus)
        val newEnergy = minOf(100, currentEnergy + energyBonus)

        with(sharedPrefs.edit()) {
            putInt("pet_happiness", newHappiness)
            putInt("pet_energy", newEnergy)
            apply()
        }

        val petName = sharedPrefs.getString("pet_name", "Your pet") ?: "Your pet"
        Toast.makeText(
            this,
            "$petName is proud! 🌟 +$pointsEarned points earned!",
            Toast.LENGTH_LONG
        ).show()


        loadUserProgress()

    }

    private fun performDailyPetReset() {
        val currentEnergy = sharedPrefs.getInt("pet_energy", 100)

        if (currentEnergy < 20) {
            with(sharedPrefs.edit()) {
                putInt("pet_energy", 20)
                apply()
            }

            energyBar.progress = 20
            energyText.text = "20%"

            Log.d("MainActivityFlow", "Daily pet energy reset to 20%")
        }
    }

    private fun checkDailyCheckInStatus() {
        val currentUser = auth.currentUser ?: return

        lifecycleScope.launch {
            try {
                updateTaskMenuStatus()

            } catch (e: Exception) {
                Log.e("MainActivityFlow", "Error checking status: ${e.message}")
            }
        }
    }

    private fun updateTaskMenuStatus() {
        lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val hasCheckinToday = repository.hasCompletedTodayQuestionnaire(currentUser.uid)
                val todayTasks = repository.getTodayTasks(currentUser.uid)
                val pendingTasks = todayTasks.filter { !it.completed }
                val completedTasks = todayTasks.filter { it.completed }
                val hasDiaryEntryToday = repository.hasDiaryEntryToday(currentUser.uid)


                taskMenuComponent.updateMenuStatus(
                    userProgress = currentUserProgress ?: UserProgress(),
                    hasCheckinToday = hasCheckinToday,
                    pendingTasksCount = pendingTasks.size,
                    completedTasksToday = completedTasks.size,
                    hasDiaryEntryToday = hasDiaryEntryToday
                )
            } catch (e: Exception) {
                Log.e("MainActivity", "Error updating task menu: ${e.message}")
            }
        }
    }

    private fun showDailyQuestionnaire() {
        taskMenuComponent.forceCollapse()
        val currentUser = auth.currentUser ?: return

        lifecycleScope.launch {
            try {
                Log.d("MainActivityFlow", "Checking questionnaire status...")

                val hasCompleted = repository.hasCompletedTodayQuestionnaire(currentUser.uid)
                Log.d("MainActivityFlow", "Questionnaire completed: $hasCompleted")

                if (hasCompleted) {
                    Log.d("MainActivityFlow", "Showing already completed dialog")

                    android.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("Already Complete!")
                        .setMessage("You've already completed your daily check-in today! ✅\n\nCome back tomorrow for a fresh questionnaire.")
                        .setPositiveButton("View My Tasks") { _, _ ->
                            showDailyTasks()
                        }
                        .setNegativeButton("OK", null)
                        .show()
                    return@launch
                }
                Log.d("MainActivityFlow", "Showing questionnaire fragment")
                val fragment = DailyQuestionnaireFragment.newInstance()
                supportFragmentManager.beginTransaction()
                    .add(android.R.id.content, fragment)
                    .addToBackStack("questionnaire")
                    .commit()

            } catch (e: Exception) {
                Log.e("MainActivityFlow", "Error checking questionnaire status: ${e.message}")
                Toast.makeText(this@MainActivity, "Error loading questionnaire", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun redirectToPetSelection() {
        Log.d("MainActivityFlow", "Redirecting to pet selection")
        Toast.makeText(this, "Let's choose your pet companion! 🐾", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, PetSelectionActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (findViewById<FrameLayout>(R.id.tasks_container).visibility == View.VISIBLE) {
            hideDailyTasks()
        } else {
            super.onBackPressed()
        }
    }

    fun onQuestionnaireCompleted() {
        updateTaskMenuStatus()

        val currentHappiness = sharedPrefs.getInt("pet_happiness", 100)
        val currentEnergy = sharedPrefs.getInt("pet_energy", 100)

        val newHappiness = minOf(100, currentHappiness + 20)
        val newEnergy = minOf(100, currentEnergy + 15)

        with(sharedPrefs.edit()) {
            putInt("pet_happiness", newHappiness)
            putInt("pet_energy", newEnergy)
            apply()
        }

        happinessBar.progress = newHappiness
        energyBar.progress = newEnergy
        happinessText.text = "$newHappiness%"
        energyText.text = "$newEnergy%"

        val petName = sharedPrefs.getString("pet_name", "Your pet") ?: "Your pet"

        lifecycleScope.launch {

        }

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Check-in Complete! 🎉")
            .setMessage("$petName is excited about your self-care journey!\n\nYour personalized tasks are ready, plus your daily routine tasks. Complete them to earn more points and keep your pet happy!")
            .setPositiveButton("View All Tasks") { _, _ ->
                showDailyTasks()
            }
            .setNegativeButton("Later") { _, _ ->
                Toast.makeText(this, "Tap 'View Tasks' when you're ready! 📋", Toast.LENGTH_SHORT)
                    .show()
            }
            .setCancelable(false)
            .create()

        dialog.show()
        animatePet()
    }

    private fun showNewPetWelcome() {
        val petName = sharedPrefs.getString("pet_name", "Your pet") ?: "Your pet"
        val petType = sharedPrefs.getString("selected_pet_type", "cat") ?: "cat"

        val welcomeMessage = when (petType) {
            "cat" -> "🐱 $petName is ready to start this journey with you!"
            "turtle" -> "🐢 $petName is excited to be your companion!"
            else -> "🐾 $petName is here to support your self-care journey!"
        }

        val dialog = android.app.AlertDialog.Builder(this)
        dialog.setTitle("Welcome to Your Pet Journey!")
        dialog.setMessage("$welcomeMessage\n\nTake care of your pet by feeding, playing, and showing love. Your pet's happiness reflects your own self-care progress!")
        dialog.setPositiveButton("Let's Start!") { _, _ ->
            Toast.makeText(this, "Tap your pet to interact! 🎉", Toast.LENGTH_LONG).show()
        }
        dialog.setCancelable(true)
        dialog.show()
    }

    private fun loadPetData() {
        Log.d("MainActivityFlow", "Loading pet data...")

        try {
            val petName = sharedPrefs.getString("pet_name", "Unknown") ?: "Unknown"
            val petType = sharedPrefs.getString("selected_pet_type", "cat") ?: "cat"

            val isNewPet = !sharedPrefs.contains("pet_initialized")

            val happiness = if (isNewPet) {
                50
            } else {
                sharedPrefs.getInt("pet_happiness", 100)
            }

            val energy = if (isNewPet) {
                50
            } else {
                sharedPrefs.getInt("pet_energy", 100)
            }

            val level = sharedPrefs.getInt("pet_level", 1)

            petNameText.text = petName
            levelText.text = "Level $level"

            try {
                val imageResource = when (petType) {
                    "cat" -> R.drawable.pet_cat
                    "turtle" -> R.drawable.pet_turtle
                    else -> android.R.drawable.sym_def_app_icon
                }
                petImage.setImageResource(imageResource)
            } catch (e: Exception) {
                Log.e("MainActivityFlow", "Failed to set pet image, using fallback", e)
                petNameText.text = "$petName ${if (petType == "cat") "🐱" else "🐢"}"
            }

            happinessBar.progress = happiness
            energyBar.progress = energy
            happinessText.text = "$happiness%"
            energyText.text = "$energy%"

            if (isNewPet) {
                with(sharedPrefs.edit()) {
                    putInt("pet_happiness", happiness)
                    putInt("pet_energy", energy)
                    putBoolean("pet_initialized", true)
                    apply()
                }
                Log.d("MainActivityFlow", "New pet initialized with 50% stats")
            }

            loadUserProgress()

            happinessBar.visibility = android.view.View.VISIBLE
            energyBar.visibility = android.view.View.VISIBLE

        } catch (e: Exception) {
            Log.e("MainActivityFlow", "Error loading pet data", e)
            Toast.makeText(this, "Error loading pet: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadUserProgress() {
        val currentUser = auth.currentUser ?: return

        lifecycleScope.launch {
            try {
                val progress = repository.getUserProgress(currentUser.uid)
                currentUserProgress = progress

                pointsDisplay.text = "💰 ${progress?.availablePoints ?: 0}"
                streakDisplay.text = "🔥 ${progress?.currentStreak ?: 0}"

                updatePetActionButtons()

                Log.d(
                    "MainActivityFlow",
                    "User progress loaded: ${progress?.availablePoints} points, ${progress?.currentStreak} streak"
                )

            } catch (e: Exception) {
                Log.e("MainActivityFlow", "Error loading user progress: ${e.message}")
            }
        }
    }

    private fun updatePetActionButtons() {
        val availablePoints = currentUserProgress?.availablePoints ?: 0

        feedButton.text =
            "${PetActionType.FEED.emoji} ${PetActionType.FEED.displayName} (${PetActionType.FEED.pointsCost})"
        feedButton.isEnabled = availablePoints >= PetActionType.FEED.pointsCost
        feedButton.alpha = if (feedButton.isEnabled) 1.0f else 0.5f

        playButton.text =
            "${PetActionType.PLAY.emoji} ${PetActionType.PLAY.displayName} (${PetActionType.PLAY.pointsCost})"
        playButton.isEnabled = availablePoints >= PetActionType.PLAY.pointsCost
        playButton.alpha = if (playButton.isEnabled) 1.0f else 0.5f

        careButton.text =
            "${PetActionType.CARE.emoji} ${PetActionType.CARE.displayName} (${PetActionType.CARE.pointsCost})"
        careButton.isEnabled = availablePoints >= PetActionType.CARE.pointsCost
        careButton.alpha = if (careButton.isEnabled) 1.0f else 0.5f
    }

    private fun feedPet() {
        performPetAction(PetActionType.FEED)
    }

    private fun playWithPet() {
        performPetAction(PetActionType.PLAY)
    }

    private fun carePet() {
        performPetAction(PetActionType.CARE)
    }

    private fun performPetAction(action: PetActionType) {
        val currentUser = auth.currentUser ?: return
        val availablePoints = currentUserProgress?.availablePoints ?: 0

        if (availablePoints < action.pointsCost) {
            Toast.makeText(
                this,
                "Not enough points! You need ${action.pointsCost} points for ${action.displayName}",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("${action.emoji} ${action.displayName}")
            .setMessage(
                "Spend ${action.pointsCost} points to ${action.displayName.lowercase()} your pet?\n\n" +
                        "Effects:\n" +
                        "• +${action.happinessIncrease}% happiness\n" +
                        "• +${action.energyIncrease}% energy"
            )
            .setPositiveButton("Yes") { _, _ ->
                executePetAction(action)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun executePetAction(action: PetActionType) {
        val currentUser = auth.currentUser ?: return

        lifecycleScope.launch {
            try {
                val success = repository.spendPointsOnPetAction(currentUser.uid, action)

                if (success) {
                    val currentHappiness = sharedPrefs.getInt("pet_happiness", 100)
                    val currentEnergy = sharedPrefs.getInt("pet_energy", 100)

                    val newHappiness = minOf(100, currentHappiness + action.happinessIncrease)
                    val newEnergy = minOf(100, currentEnergy + action.energyIncrease)

                    with(sharedPrefs.edit()) {
                        putInt("pet_happiness", newHappiness)
                        putInt("pet_energy", newEnergy)
                        apply()
                    }

                    happinessBar.progress = newHappiness
                    energyBar.progress = newEnergy
                    happinessText.text = "$newHappiness%"
                    energyText.text = "$newEnergy%"

                    val petName = sharedPrefs.getString("pet_name", "Your pet") ?: "Your pet"

                    Toast.makeText(
                        this@MainActivity,
                        "$petName loves it! ${action.emoji} (-${action.pointsCost} points)",
                        Toast.LENGTH_LONG
                    ).show()

                    loadUserProgress()
                    animatePet()

                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to spend points. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("MainActivityFlow", "Error performing pet action: ${e.message}")
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun petTapped() {
        val petName = sharedPrefs.getString("pet_name", "Your pet") ?: "Your pet"
        val petType = sharedPrefs.getString("selected_pet_type", "cat") ?: "cat"

        val message = when (petType) {
            "cat" -> "$petName purrs contentedly! 🐱"
            "turtle" -> "$petName slowly blinks at you! 🐢"
            else -> "$petName is happy to see you!"
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        animatePet()
    }

    private fun animatePet() {
        try {
            petImage.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(150)
                .withEndAction {
                    petImage.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(150)
                        .start()
                }
                .start()
        } catch (e: Exception) {
            Log.e("MainActivityFlow", "Animation failed", e)
        }
    }
}
