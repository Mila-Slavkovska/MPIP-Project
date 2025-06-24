package com.example.mpip.service

import android.util.Log
import com.example.mpip.domain.enums.mentalHealthTips.TipCategory
import com.example.mpip.domain.enums.mentalHealthTips.TipDifficulty
import com.example.mpip.domain.mentalHealthTips.MentalHealthTip
import com.example.mpip.domain.mentalHealthTips.TipGenerationRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class OpenAIService {
    companion object {
        private const val OPENAI_API_URL = "https://api.openai.com/v1/chat/completions"
        private const val MODEL = "gpt-3.5-turbo"
        private const val MAX_TOKENS = 200
        private const val TEMPERATURE = 1.0f

        private const val API_KEY =
            "YOUR_API_KEY"
    }

    suspend fun generateMentalHealthTip(request: TipGenerationRequest): MentalHealthTip? {
        return withContext(Dispatchers.IO) {
            try {
                if (API_KEY == "YOUR_OPENAI_API_KEY_HERE") {
                    Log.w("OpenAIService", "API key not configured, using fallback tip")
                    return@withContext generateFallbackTip(request)
                }

                val prompt = buildPersonalizedPrompt(request)
                val response = callOpenAIAPI(prompt)

                if (response != null) {
                    parseTipResponse(response, request)
                } else {
                    Log.w("OpenAIService", "API call failed, using fallback tip")
                    generateFallbackTip(request)
                }

            } catch (e: Exception) {
                Log.e("OpenAIService", "Error generating tip: ${e.message}", e)
                generateFallbackTip(request)
            }
        }
    }

    private fun buildPersonalizedPrompt(request: TipGenerationRequest): String {
        val basePrompt = """
            Generate a brief, actionable mental health tip (max 150 words) that is:
            - Practical and easy to implement
            - Positive and encouraging
            - Suitable for daily practice
            - Evidence-based
            
            Format the response as JSON with these fields:
            {
                "title": "Short tip title",
                "content": "Main tip content",
                "category": "one of: general, anxiety, stress, mood, sleep, mindfulness, relationships, productivity, self_care, gratitude, exercise, nutrition",
                "difficulty": "easy, medium, or hard"
            }
        """.trimIndent()

        val personalizedContext = buildString {
            if (request.userEmotions.isNotEmpty()) {
                append("\n\nUser's recent emotions: ${request.userEmotions.joinToString(", ")}")
                append("\nPlease tailor the tip to help with these emotional states.")
            }

            if (request.recentTasks.isNotEmpty()) {
                append("\n\nUser has been working on: ${request.recentTasks.joinToString(", ")}")
                append("\nConsider their current self-care activities.")
            }

            if (request.userPreferences.preferredCategories.isNotEmpty()) {
                val categories =
                    request.userPreferences.preferredCategories.joinToString(", ") { it.displayName }
                append("\n\nUser prefers tips about: $categories")
            }

            if (request.previousTips.isNotEmpty()) {
                append(
                    "\n\nAvoid repeating these recent topics: ${
                        request.previousTips.take(3).joinToString(", ")
                    }"
                )
            }

            append("\n\nMake the tip feel personal and relevant to their current situation.")
        }

        return basePrompt + personalizedContext
    }

    private suspend fun callOpenAIAPI(prompt: String): String? {
        return try {
            val url = URL(OPENAI_API_URL)
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $API_KEY")
            connection.doOutput = true

            val requestBody = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", MAX_TOKENS)
                put("temperature", TEMPERATURE)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(requestBody.toString())
            writer.flush()
            writer.close()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                Log.d("OpenAIService", "API call successful")
                response
            } else {
                Log.e("OpenAIService", "API call failed with code: $responseCode")
                null
            }

        } catch (e: Exception) {
            Log.e("OpenAIService", "API call error: ${e.message}", e)
            null
        }
    }

    private fun parseTipResponse(
        response: String,
        request: TipGenerationRequest
    ): MentalHealthTip? {
        return try {
            val jsonResponse = JSONObject(response)
            val choices = jsonResponse.getJSONArray("choices")
            val content = choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            val tipJson = JSONObject(content)

            val title = tipJson.getString("title")
            val tipContent = tipJson.getString("content")
            val categoryName = tipJson.getString("category")
            val difficultyName = tipJson.getString("difficulty")

            val category = TipCategory.values().find {
                it.name.lowercase() == categoryName.lowercase()
            } ?: TipCategory.GENERAL

            val difficulty = when (difficultyName.lowercase()) {
                "easy" -> TipDifficulty.EASY
                "medium" -> TipDifficulty.MEDIUM
                "hard" -> TipDifficulty.HARD
                else -> TipDifficulty.EASY
            }

            MentalHealthTip(
                id = MentalHealthTip.generateId(request.userId, request.date),
                userId = request.userId,
                date = request.date,
                title = title,
                content = tipContent,
                category = category,
                difficulty = difficulty,
                isPersonalized = request.userEmotions.isNotEmpty() || request.recentTasks.isNotEmpty(),
                basedOnEmotions = request.userEmotions,
                basedOnTasks = request.recentTasks,
                aiModel = MODEL,
                generationPrompt = buildPersonalizedPrompt(request).take(500)
            )

        } catch (e: Exception) {
            Log.e("OpenAIService", "Error parsing tip response: ${e.message}", e)
            null
        }
    }


    private fun generateFallbackTip(request: TipGenerationRequest): MentalHealthTip {
        val fallbackTips = getFallbackTips()

        val relevantTips = if (request.userEmotions.isNotEmpty()) {
            fallbackTips.filter { tip ->
                request.userEmotions.any { emotion ->
                    tip.content.contains(emotion, ignoreCase = true) ||
                            tip.category.displayName.contains(emotion, ignoreCase = true)
                }
            }.ifEmpty { fallbackTips }
        } else {
            fallbackTips
        }

        val selectedTip = relevantTips.random()

        return selectedTip.copy(
            id = MentalHealthTip.generateId(request.userId, request.date),
            userId = request.userId,
            date = request.date,
            isPersonalized = false,
            aiModel = "fallback"
        )
    }


    private fun getFallbackTips(): List<MentalHealthTip> {
        return listOf(
            MentalHealthTip(
                title = "Take a Mindful Moment",
                content = "Take 3 deep breaths and notice 5 things you can see, 4 things you can touch, 3 things you can hear, 2 things you can smell, and 1 thing you can taste. This grounding technique helps bring you into the present moment.",
                category = TipCategory.MINDFULNESS,
                difficulty = TipDifficulty.EASY
            ),
            MentalHealthTip(
                title = "Gratitude Check-In",
                content = "Write down three things you're grateful for today, no matter how small. It could be your morning coffee, a text from a friend, or simply having a roof over your head. Gratitude shifts our focus to the positive.",
                category = TipCategory.GRATITUDE,
                difficulty = TipDifficulty.EASY
            ),
            MentalHealthTip(
                title = "Move Your Body",
                content = "Take a 5-minute walk, do some gentle stretches, or dance to your favorite song. Physical movement releases endorphins and can instantly boost your mood while reducing stress and anxiety.",
                category = TipCategory.EXERCISE,
                difficulty = TipDifficulty.EASY
            ),
            MentalHealthTip(
                title = "Digital Detox Break",
                content = "Put your phone in another room for 30 minutes and engage in a screen-free activity. Read a book, take a bath, or have a face-to-face conversation. Your mind will thank you for the break.",
                category = TipCategory.STRESS,
                difficulty = TipDifficulty.MEDIUM
            ),
            MentalHealthTip(
                title = "Self-Compassion Practice",
                content = "Talk to yourself like you would talk to a good friend. When you notice self-criticism, pause and ask: 'What would I say to a friend in this situation?' Treat yourself with the same kindness.",
                category = TipCategory.SELF_CARE,
                difficulty = TipDifficulty.MEDIUM
            ),
            MentalHealthTip(
                title = "Create a Calming Ritual",
                content = "Establish a 10-minute evening routine that signals to your brain it's time to wind down. This could include gentle stretching, herbal tea, journaling, or listening to calming music.",
                category = TipCategory.SLEEP,
                difficulty = TipDifficulty.MEDIUM
            ),
            MentalHealthTip(
                title = "Connect with Nature",
                content = "Step outside and spend at least 10 minutes in nature. If you can't go outside, sit by a window or tend to a houseplant. Nature connection reduces cortisol levels and improves mood.",
                category = TipCategory.MOOD,
                difficulty = TipDifficulty.EASY
            ),
            MentalHealthTip(
                title = "Reach Out to Someone",
                content = "Send a text, make a call, or write a note to someone you care about. Social connections are vital for mental health, and often the simple act of reaching out benefits both people.",
                category = TipCategory.RELATIONSHIPS,
                difficulty = TipDifficulty.EASY
            )
        )
    }
}