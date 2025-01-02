package com.example.cognify.network

import com.example.cognify.models.ChatSession
import com.example.cognify.models.Message
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

// ----- Data class permintaan & respons tambahan -----

data class LoginRequest(val idToken: String)
data class LoginResponse(val message: String)

data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String
)
data class RegisterResponse(val message: String)

// Data class untuk daftar session
data class FetchSessionsResponse(
    val sessions: List<ChatSession>
)

// Data class untuk daftar messages
data class FetchMessagesResponse(
    val messages: List<Message>
)

// Data class respons create session (tidak kita perlukan lagi
// jika server otomatis buat session)
data class CreateSessionResponse(
    val sessionId: String,
    val userId: String,
    val timestamp: Long,
    val messages: List<Message>,
    val judul: String? = null
)

// Data class untuk request delete session
data class DeleteSessionRequest(
    val sessionId: String
)

// Data class untuk request edit session
data class EditSessionRequest(
    val sessionId: String,
    val newTitle: String
)

// Data class respons edit session
data class EditSessionResponse(
    val message: String
)

// Data class respons send message
data class SendMessageResponse(
    val userMessage: Message?,
    val assistantMessage: Message?
)

interface ApiService {

    // =========================
    // AUTH
    // =========================
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>


    // =========================
    // SESSION MANAGEMENT
    // =========================

    // HAPUS createNewChatSession (sudah tidak dipakai)
    // @POST("chat/session/new")

    // Ambil daftar semua sesi user
    @GET("chat/sessions")
    suspend fun getChatSessions(
        @Header("Authorization") authHeader: String
    ): Response<FetchSessionsResponse>

    // Hapus sesi
    @HTTP(method = "DELETE", path = "chat/session/delete", hasBody = true)
    suspend fun deleteChatSession(
        @Header("Authorization") authHeader: String,
        @Body request: DeleteSessionRequest
    ): Response<Any>

    // Edit judul session
    @PATCH("chat/session/edit")
    suspend fun editSessionTitle(
        @Header("Authorization") authHeader: String,
        @Body request: EditSessionRequest
    ): Response<EditSessionResponse>

    // =========================
    // MESSAGES
    // =========================

    // Ambil daftar pesan
    @GET("chat/session/messages")
    suspend fun getChatMessages(
        @Header("Authorization") authHeader: String,
        @Query("sessionId") sessionId: String
    ): Response<FetchMessagesResponse>

    // Kirim pesan (multipart)
    @Multipart
    @POST("chat/session")
    suspend fun sendMessage(
        @Header("Authorization") authHeader: String,
        @Part("sessionId") sessionId: RequestBody,
        @Part("content") content: RequestBody,
        @Part images: List<MultipartBody.Part>?
    ): Response<SendMessageResponse>

    @POST("chat/session")
    suspend fun sendMessageRaw(
        @Header("Authorization") authHeader: String,
        @Body body: RequestBody
    ): Response<SendMessageResponse>

}
