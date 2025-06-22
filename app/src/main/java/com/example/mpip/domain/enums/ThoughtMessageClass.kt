package com.example.mpip.domain.enums

data class ThoughtMessageClass(
    val senderId: String = "",
    val senderUsername: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val seen: Boolean = false
)
