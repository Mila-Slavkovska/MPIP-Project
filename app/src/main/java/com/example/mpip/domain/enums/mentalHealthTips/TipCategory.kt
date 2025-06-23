package com.example.mpip.domain.enums.mentalHealthTips

enum class TipCategory(val displayName: String, val emoji: String, val description: String) {
    GENERAL("General Wellness", "🌱", "General mental health and wellness tips"),
    ANXIETY("Anxiety", "😰", "Tips for managing anxiety and worry"),
    STRESS("Stress", "😫", "Stress management and relaxation techniques"),
    MOOD("Mood", "😊", "Tips for improving mood and emotional well-being"),
    SLEEP("Sleep", "😴", "Sleep hygiene and rest optimization"),
    MINDFULNESS("Mindfulness", "🧘", "Mindfulness and meditation practices"),
    RELATIONSHIPS("Relationships", "❤️", "Tips for healthy relationships and social connections"),
    PRODUCTIVITY("Productivity", "⚡", "Mental health-focused productivity tips"),
    SELF_CARE("Self-Care", "💝", "Self-care and personal nurturing activities"),
    GRATITUDE("Gratitude", "🙏", "Gratitude practices and positive thinking"),
    EXERCISE("Exercise", "🏃", "Physical activity for mental health"),
    NUTRITION("Nutrition", "🥗", "Nutrition tips for better mental health")
}