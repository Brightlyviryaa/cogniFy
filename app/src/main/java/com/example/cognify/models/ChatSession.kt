// src/main/java/com/example/cognify/models/ChatSession.kt

package com.example.cognify.models

data class ChatSession(
    val id: String = "",
    val userId: String = "",
    val timestamp: Long = 0L,
    val messages: List<Message> = emptyList()
)
