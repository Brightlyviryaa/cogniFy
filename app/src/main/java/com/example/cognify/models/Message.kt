// src/main/java/com/example/cognify/models/Message.kt

package com.example.cognify.models

data class Message(
    val role: String = "",
    val content: String = "",
    val img_urls: List<String> = emptyList()
)
