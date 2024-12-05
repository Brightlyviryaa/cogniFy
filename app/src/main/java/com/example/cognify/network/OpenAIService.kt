// src/main/java/com/example/cognify/network/OpenAIService.kt

package com.example.cognify.network

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAIService {
    @POST("chat/completions")
    fun sendMessage(
        @Header("Authorization") auth: String,
        @Body payload: JsonObject
    ): Call<JsonObject>
}
