package com.example.mpip.domain

data class ThoughtMessage(
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val notified: Boolean = false,
    val opened: Boolean = false,
    var key: String? = null
)

