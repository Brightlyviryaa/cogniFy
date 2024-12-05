// src/main/java/com/example/cognify/models/Message.kt

package com.example.cognify.models

data class Message(
    val role: String = "",
    val content: String = "",  // Bisa String atau List<Map<String, Any>>
    val img_urls: List<String> = emptyList()
)
