package com.example.mpip.domain

data class ThoughtMessage(
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val seen: Boolean = false
)

