package com.example.mpip

import com.example.mpip.domain.Emotion
import com.example.mpip.domain.EmotionRelation
import com.example.mpip.domain.TaskTemplate
import com.example.mpip.domain.enums.tasks.TaskCategory
import com.example.mpip.domain.enums.tasks.TaskDifficulty
import com.example.mpip.domain.enums.tasks.TaskType
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FirebaseDataInitializer {
    private val database =
        FirebaseDatabase.getInstance("https://mpip-project-ea779-default-rtdb.europe-west1.firebasedatabase.app")

    suspend fun initializeAllData(): Boolean {
        try {
            initializeEmotions()
            initializeEmotionRelations()
            kotlinx.coroutines.delay(2000)
            initializeTaskTemplates()
            return true
            println("✅ All Firebase Realtime Database data initialized successfully!")
        } catch (e: Exception) {
            return false
            println("❌ Error initializing Firebase data: ${e.message}")
        }
    }

    private suspend fun initializeEmotions(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val emotions = mapOf(
                "happy" to Emotion("happy", "Happy", "😊", "positive", true),
                "excited" to Emotion("excited", "Excited", "🤩", "positive", true),
                "grateful" to Emotion("grateful", "Grateful", "🙏", "positive", true),
                "peaceful" to Emotion("peaceful", "Peaceful", "😌", "positive", true),
                "confident" to Emotion("confident", "Confident", "💪", "positive", true),
                "loved" to Emotion("loved", "Loved", "🥰", "positive", true),
                "proud" to Emotion("proud", "Proud", "😎", "positive", true),
                "energetic" to Emotion("energetic", "Energetic", "⚡", "positive", true),

                // Negative emotions
                "sad" to Emotion("sad", "Sad", "😢", "negative", true),
                "anxious" to Emotion("anxious", "Anxious", "😰", "negative", true),
                "angry" to Emotion("angry", "Angry", "😠", "negative", true),
                "frustrated" to Emotion("frustrated", "Frustrated", "😤", "negative", true),
                "overwhelmed" to Emotion("overwhelmed", "Overwhelmed", "😵", "negative", true),
                "lonely" to Emotion("lonely", "Lonely", "😔", "negative", true),
                "stressed" to Emotion("stressed", "Stressed", "😫", "negative", true),
                "tired" to Emotion("tired", "Tired", "😴", "negative", true),

                // Neutral emotions
                "calm" to Emotion("calm", "Calm", "😐", "neutral", true),
                "confused" to Emotion("confused", "Confused", "🤔", "neutral", true),
                "curious" to Emotion("curious", "Curious", "🧐", "neutral", true),
                "focused" to Emotion("focused", "Focused", "🎯", "neutral", true)
            )

            database.getReference("emotions").setValue(emotions)
                .addOnSuccessListener {
                    println("✅ Emotions initialized")
                    continuation.resume(true)
                }
                .addOnFailureListener { error ->
                    println("❌ Failed to initialize emotions: ${error.message}")
                    continuation.resume(false)
                }
        }
    }

    private suspend fun initializeEmotionRelations(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val relations = mapOf(
                "work_school" to EmotionRelation(
                    "work_school",
                    "Work/School",
                    "Related to job, studies, or academic pressure",
                    true
                ),
                "relationships" to EmotionRelation(
                    "relationships",
                    "Relationships",
                    "Friends, family, romantic relationships",
                    true
                ),
                "health" to EmotionRelation(
                    "health",
                    "Health",
                    "Physical or mental health concerns",
                    true
                ),
                "finance" to EmotionRelation(
                    "finance",
                    "Finance",
                    "Money, bills, or financial security",
                    true
                ),
                "future" to EmotionRelation(
                    "future",
                    "Future",
                    "Uncertainty about what's coming next",
                    true
                ),
                "personal_growth" to EmotionRelation(
                    "personal_growth",
                    "Personal Growth",
                    "Self-improvement and personal development",
                    true
                ),
                "daily_life" to EmotionRelation(
                    "daily_life",
                    "Daily Life",
                    "Routine activities and daily experiences",
                    true
                ),
                "social_situations" to EmotionRelation(
                    "social_situations",
                    "Social Situations",
                    "Interactions with others, social anxiety",
                    true
                ),
                "achievements" to EmotionRelation(
                    "achievements",
                    "Achievements",
                    "Success, accomplishments, or lack thereof",
                    true
                ),
                "other" to EmotionRelation(
                    "other",
                    "Other",
                    "Something else not listed above",
                    true
                )
            )

            database.getReference("emotion_relations").setValue(relations)
                .addOnSuccessListener {
                    println("✅ Emotion relations initialized")
                    continuation.resume(true)
                }
                .addOnFailureListener { error ->
                    println("❌ Failed to initialize emotion relations: ${error.message}")
                    continuation.resume(false)
                }
        }
    }

    private suspend fun initializeTaskTemplates(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val questionnaireTasks = mapOf(
                "gratitude_photo" to TaskTemplate(
                    "gratitude_photo",
                    "Gratitude Photo",
                    "Take a picture of something you're grateful for today",
                    TaskType.PHOTO,
                    TaskCategory.QUESTIONNAIRE_BASED,
                    20,
                    true,
                    listOf("happy", "excited", "grateful"),
                    TaskDifficulty.EASY
                ),
                "share_joy" to TaskTemplate(
                    "share_joy",
                    "Share Your Joy",
                    "Write about what made you feel good today and why it was meaningful",
                    TaskType.TEXT,
                    TaskCategory.QUESTIONNAIRE_BASED,
                    25,
                    true,
                    listOf("happy", "excited", "grateful", "proud"),
                    TaskDifficulty.MEDIUM
                ),
                "spread_kindness" to TaskTemplate(
                    "spread_kindness",
                    "Spread Kindness",
                    "Write a kind message you could send to someone you care about",
                    TaskType.TEXT,
                    TaskCategory.QUESTIONNAIRE_BASED,
                    30,
                    true,
                    listOf("happy", "grateful", "loved", "peaceful"),
                    TaskDifficulty.MEDIUM
                ),

                "breathing_exercise" to TaskTemplate(
                    "breathing_exercise",
                    "Breathing Exercise",
                    "Take 5 deep breaths and write how you feel afterward",
                    TaskType.TEXT,
                    TaskCategory.QUESTIONNAIRE_BASED,
                    20,
                    true,
                    listOf("sad", "anxious", "angry", "frustrated", "stressed"),
                    TaskDifficulty.EASY
                ),
                "emotion_processing" to TaskTemplate(
                    "emotion_processing",
                    "Emotion Processing",
                    "Write about what you're feeling and what might help you feel better",
                    TaskType.TEXT,
                    TaskCategory.QUESTIONNAIRE_BASED,
                    35,
                    true,
                    listOf("sad", "anxious", "angry", "frustrated", "overwhelmed", "lonely"),
                    TaskDifficulty.HARD
                ),
                "self_compassion" to TaskTemplate(
                    "self_compassion",
                    "Self-Compassion",
                    "Write something kind and understanding to yourself, as you would to a good friend",
                    TaskType.TEXT,
                    TaskCategory.QUESTIONNAIRE_BASED,
                    30,
                    true,
                    listOf("sad", "anxious", "stressed", "overwhelmed", "tired", "lonely"),
                    TaskDifficulty.MEDIUM
                ),
                "comfort_item" to TaskTemplate(
                    "comfort_item",
                    "Comfort Item",
                    "Take a photo of something that brings you comfort or peace",
                    TaskType.PHOTO,
                    TaskCategory.QUESTIONNAIRE_BASED,
                    15,
                    true,
                    listOf(
                        "sad",
                        "anxious",
                        "stressed",
                        "overwhelmed",
                        "tired",
                        "lonely",
                        "frustrated",
                        "angry"
                    ),
                    TaskDifficulty.EASY
                ),

                "mindful_observation" to TaskTemplate(
                    "mindful_observation",
                    "Mindful Observation",
                    "Take a photo of something beautiful you notice around you right now",
                    TaskType.PHOTO,
                    TaskCategory.QUESTIONNAIRE_BASED,
                    20,
                    true,
                    emptyList(),
                    TaskDifficulty.EASY
                ),
                "three_good_things" to TaskTemplate(
                    "three_good_things",
                    "Three Good Things",
                    "Write down three good things that happened today, no matter how small",
                    TaskType.TEXT,
                    TaskCategory.QUESTIONNAIRE_BASED,
                    25,
                    true,
                    emptyList(),
                    TaskDifficulty.MEDIUM
                ),
                "future_self_letter" to TaskTemplate(
                    "future_self_letter",
                    "Future Self Letter",
                    "Write a short encouraging message to yourself for tomorrow",
                    TaskType.TEXT,
                    TaskCategory.QUESTIONNAIRE_BASED,
                    30,
                    true,
                    emptyList(),
                    TaskDifficulty.MEDIUM
                )
            )

            val routineTasks = mapOf(
                "hydrate" to TaskTemplate(
                    "hydrate",
                    "Hydrate",
                    "Drink a glass of water mindfully",
                    TaskType.SIMPLE_ACTION,
                    TaskCategory.DAILY_ROUTINE,
                    5,
                    true,
                    emptyList(),
                    TaskDifficulty.EASY
                ),
                "step_outside" to TaskTemplate(
                    "step_outside",
                    "Step Outside",
                    "Go outside for at least 2 minutes, even if just to your balcony",
                    TaskType.SIMPLE_ACTION,
                    TaskCategory.DAILY_ROUTINE,
                    10,
                    true,
                    emptyList(),
                    TaskDifficulty.EASY
                ),
                "stretch_break" to TaskTemplate(
                    "stretch_break",
                    "Stretch Break",
                    "Do some gentle stretches or move your body for 1 minute",
                    TaskType.SIMPLE_ACTION,
                    TaskCategory.DAILY_ROUTINE,
                    10,
                    true,
                    emptyList(),
                    TaskDifficulty.EASY
                ),
                "digital_break" to TaskTemplate(
                    "digital_break",
                    "Digital Break",
                    "Put your phone away for 10 minutes and focus on your surroundings",
                    TaskType.SIMPLE_ACTION,
                    TaskCategory.DAILY_ROUTINE,
                    15,
                    true,
                    emptyList(),
                    TaskDifficulty.MEDIUM
                ),
                "tidy_space" to TaskTemplate(
                    "tidy_space",
                    "Tidy Space",
                    "Clean or organize one small area around you",
                    TaskType.SIMPLE_ACTION,
                    TaskCategory.DAILY_ROUTINE,
                    10,
                    true,
                    emptyList(),
                    TaskDifficulty.EASY
                )
            )

            val allTasks = questionnaireTasks + routineTasks

            database.getReference("task_templates").setValue(allTasks)
                .addOnSuccessListener {
                    println("✅ Task templates initialized (${allTasks.size} tasks)")
                    continuation.resume(true)
                }
                .addOnFailureListener { error ->
                    println("❌ Failed to initialize task templates: ${error.message}")
                    continuation.resume(false)
                }
        }
    }
}