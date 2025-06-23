package com.example.mpip.domain.enums.diary

enum class DiaryMood(val displayName: String, val emoji: String, val color: String) {
    AMAZING("Amazing", "🤩", "#00B894"),
    HAPPY("Happy", "😊", "#6C63FF"),
    GOOD("Good", "🙂", "#74B9FF"),
    OKAY("Okay", "😐", "#FDCB6E"),
    MEH("Meh", "😑", "#A29BFE"),
    SAD("Sad", "😢", "#E17055"),
    TERRIBLE("Terrible", "😭", "#D63031"),
    GRATEFUL("Grateful", "🙏", "#00B894"),
    EXCITED("Excited", "🤗", "#FF7675"),
    PEACEFUL("Peaceful", "😌", "#55A3FF"),
    ANXIOUS("Anxious", "😰", "#FDCB6E"),
    CONFUSED("Confused", "🤔", "#A29BFE");

    companion object {
        fun fromDisplayName(name: String): DiaryMood? {
            return entries.find { it.displayName == name }
        }

        fun fromEmoji(emoji: String): DiaryMood? {
            return entries.find { it.emoji == emoji }
        }
    }
}