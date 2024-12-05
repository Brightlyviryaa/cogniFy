// src/main/java/com/example/cognify/MainActivity.kt

package com.example.cognify

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cognify.adapters.MessageAdapter
import com.example.cognify.models.ChatSession
import com.example.cognify.models.Message
import com.example.cognify.network.OpenAIService
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var sendButton: ImageButton
    private lateinit var messageInput: TextInputEditText
    private lateinit var userEmailTextView: TextView

    private val messagesList = mutableListOf<Message>()
    private val chatSessionsList = mutableListOf<ChatSession>()

    private lateinit var firestore: FirebaseFirestore
    private var messagesListener: ListenerRegistration? = null

    private var currentChatId: String? = null
    private var currentUserId: String? = null

    // **IMPORTANT:** Jangan menghardcode API Key di sini untuk keamanan.
    // Ganti dengan metode penyimpanan API Key yang aman, seperti menggunakan BuildConfig atau lainnya.
    private val openAIAPIKey = "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" // Ganti dengan API Key Anda

    // Base ID untuk item menu sesi chat dinamis
    private val MENU_ITEM_CHAT_SESSION_BASE_ID = 1000

    // Tag untuk logging
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi Firebase Authentication dan Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Menghubungkan elemen UI dengan findViewById
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recycler_view_messages)
        sendButton = findViewById(R.id.send_button)
        messageInput = findViewById(R.id.message_input)

        // Set up Toolbar
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Menangani klik item menu di Toolbar
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_profile -> {
                    startActivity(Intent(this, EditProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Komponen Navigation Drawer - Akses melalui headerView
        val headerView = navigationView.getHeaderView(0)
        userEmailTextView = headerView.findViewById(R.id.user_email)

        // Set up RecyclerView untuk pesan
        messageAdapter = MessageAdapter(messagesList)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }

        // Listener untuk NavigationView
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_new_chat -> {
                    createNewChatSession {
                        // Beralih ke sesi chat baru
                        // Anda bisa menambahkan logika tambahan di sini jika diperlukan
                    }
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.menu_profile -> {
                    startActivity(Intent(this, EditProfileActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.menu_settings -> {
                    Toast.makeText(this, "Pengaturan Dipilih", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.menu_logout -> {
                    auth.signOut()
                    showLoggedOutUI()
                    Toast.makeText(this, "Keluar Berhasil", Toast.LENGTH_SHORT).show()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.menu_login -> {
                    showLoginRegisterDialog()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> {
                    // Menangani klik sesi chat dinamis
                    val chatSessionIndex = menuItem.itemId - MENU_ITEM_CHAT_SESSION_BASE_ID
                    if (chatSessionIndex >= 0 && chatSessionIndex < chatSessionsList.size) {
                        switchChatSession(chatSessionsList[chatSessionIndex])
                    }
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
            }
        }

        // Listener untuk tombol Kirim
        sendButton.setOnClickListener {
            sendMessage()
        }

        // Cek apakah user sudah login
        val currentUser = auth.currentUser
        if (currentUser != null) {
            currentUserId = currentUser.uid
            userEmailTextView.text = currentUser.email
            showLoggedInUI()
            loadChatSessions()
            Log.d(TAG, "User logged in: ${currentUser.email}")
        } else {
            showLoggedOutUI()
            Log.d(TAG, "No user logged in")
        }

        // Update menu items berdasarkan status login saat aplikasi dimulai
        updateMenuItems()
    }

    override fun onResume() {
        super.onResume()
        val currentUser = auth.currentUser
        if (currentUser != null && currentUserId == null) {
            currentUserId = currentUser.uid
            userEmailTextView.text = currentUser.email
            showLoggedInUI()
            loadChatSessions()
            Log.d(TAG, "User logged in: ${currentUser.email}")
        } else if (currentUser == null && currentUserId != null) {
            // User telah logout
            currentUserId = null
            userEmailTextView.text = ""
            showLoggedOutUI()
            Log.d(TAG, "User logged out")
        }
    }

    private fun updateMenuItems() {
        val menu = navigationView.menu
        val isLoggedIn = auth.currentUser != null

        menu.findItem(R.id.menu_login)?.isVisible = !isLoggedIn
        menu.findItem(R.id.menu_logout)?.isVisible = isLoggedIn
    }

    private fun showLoggedInUI() {
        // Menampilkan elemen UI untuk user yang login
        updateMenuItems()
    }

    private fun showLoggedOutUI() {
        // Menampilkan dialog login/register
        updateMenuItems()
        showLoginRegisterDialog()
    }

    private fun showLoginRegisterDialog() {
        val dialog = Dialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_login_register, null, false)
        dialog.setContentView(view)

        // Set dialog dimensions
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val loginButton = view.findViewById<Button>(R.id.loginButton)
        val registerButton = view.findViewById<Button>(R.id.registerButton)

        loginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            dialog.dismiss()
        }

        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun createNewChatSession(onSessionCreated: () -> Unit) {
        val currentUser = auth.currentUser ?: return

        val chatMap = hashMapOf(
            "userId" to currentUser.uid,
            "timestamp" to System.currentTimeMillis(),
            "messages" to emptyList<Map<String, Any>>()
        )

        firestore.collection("chats")
            .add(chatMap)
            .addOnSuccessListener { documentReference ->
                currentChatId = documentReference.id
                Toast.makeText(this, "Sesi percakapan baru dibuat.", Toast.LENGTH_SHORT).show()
                loadChatSessions()
                onSessionCreated()
                Log.d(TAG, "New chat session created with ID: $currentChatId")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal membuat sesi percakapan: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Failed to create chat session", e)
            }
    }

    private fun loadChatSessions() {
        currentUserId?.let { userId ->
            firestore.collection("chats")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Toast.makeText(this, "Error loading chat sessions: ${error.message}", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Error loading chat sessions", error)
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        chatSessionsList.clear()
                        val menu = navigationView.menu
                        val chatSessionsMenu = menu.findItem(R.id.menu_chat_sessions_heading)?.subMenu
                        chatSessionsMenu?.clear()

                        for ((index, doc) in snapshots.documents.withIndex()) {
                            val chatSession = ChatSession(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                timestamp = doc.getLong("timestamp") ?: 0L,
                                messages = emptyList()
                            )
                            chatSessionsList.add(chatSession)

                            // Tambahkan setiap sesi chat sebagai item menu
                            val menuItemId = MENU_ITEM_CHAT_SESSION_BASE_ID + index
                            chatSessionsMenu?.add(Menu.NONE, menuItemId, Menu.NONE, "Chat ${index + 1}")
                                ?.setIcon(R.drawable.ic_chat_session)
                        }

                        // Load sesi chat terbaru secara default
                        if (chatSessionsList.isNotEmpty() && currentChatId == null) {
                            switchChatSession(chatSessionsList[0])
                        }
                        Log.d(TAG, "Loaded ${chatSessionsList.size} chat sessions")
                    }
                }
        }
    }

    private fun switchChatSession(chatSession: ChatSession) {
        // Clear current messages
        messagesList.clear()
        messageAdapter.notifyDataSetChanged()

        // Remove previous listener
        messagesListener?.remove()

        currentChatId = chatSession.id
        Log.d(TAG, "Switched to chat session ID: $currentChatId")

        listenForMessages()
    }

    private fun listenForMessages() {
        if (currentChatId == null) return

        messagesListener = firestore.collection("chats")
            .document(currentChatId!!)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error listening for messages", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val messagesData = snapshot.get("messages")
                    messagesList.clear()
                    if (messagesData is List<*>) {
                        messagesData.forEach { msgData ->
                            val msg = msgData as? Map<*, *>
                            if (msg != null) {
                                val role = msg["role"] as? String ?: "user"
                                val contentAny = msg["content"]
                                val imgUrlsAny = msg["img_urls"]
                                val imgUrls = (imgUrlsAny as? List<*>)?.filterIsInstance<String>() ?: emptyList()

                                val formattedContent = when (contentAny) {
                                    is String -> contentAny
                                    is List<*> -> {
                                        val contentBuilder = StringBuilder()
                                        contentAny.forEach { item ->
                                            when (item) {
                                                is Map<*, *> -> {
                                                    val type = item["type"] as? String
                                                    when (type) {
                                                        "text" -> {
                                                            val text = item["text"] as? String
                                                            if (text != null) {
                                                                contentBuilder.append("$text\n")
                                                            }
                                                        }
                                                        "image_url" -> {
                                                            val imageUrlMap = item["image_url"] as? Map<*, *>
                                                            val url = imageUrlMap?.get("url") as? String
                                                            if (url != null) {
                                                                contentBuilder.append("[Image URL]: $url\n")
                                                            }
                                                        }
                                                    }
                                                }
                                                is String -> contentBuilder.append("$item\n")
                                                else -> {}
                                            }
                                        }
                                        contentBuilder.toString().trim()
                                    }
                                    else -> ""
                                }

                                val message = Message(
                                    role = role,
                                    content = formattedContent,
                                    img_urls = imgUrls
                                )
                                messagesList.add(message)
                                Log.d(TAG, "Message added: $message")
                            } else {
                                Log.e(TAG, "Invalid message format: $msgData")
                            }
                        }
                    } else {
                        Log.e(TAG, "messagesData is not a List: $messagesData")
                    }

                    messageAdapter.updateMessages(messagesList)
                    recyclerView.scrollToPosition(messagesList.size - 1)
                    Log.d(TAG, "Updated messages list with ${messagesList.size} messages")
                }
            }
    }

    private fun sendMessage() {
        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) {
            Toast.makeText(this, "Pesan tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        if (auth.currentUser == null) {
            Toast.makeText(this, "Anda harus login terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentChatId == null) {
            // Create a new chat session and then send the message
            createNewChatSession {
                sendMessageToFirestore(text)
            }
        } else {
            sendMessageToFirestore(text)
        }
    }

    private fun sendMessageToFirestore(text: String) {
        val userMessage = mapOf(
            "role" to "user",
            "content" to text,
            "img_urls" to emptyList<String>()
        )

        firestore.collection("chats")
            .document(currentChatId!!)
            .update("messages", FieldValue.arrayUnion(userMessage))
            .addOnSuccessListener {
                messageInput.text?.clear()
                sendToOpenAI(userMessage)
                Log.d(TAG, "User message sent to Firestore")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal mengirim pesan", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Failed to send user message to Firestore", it)
            }
    }

    private fun sendToOpenAI(userMessage: Map<String, Any>) {
        val apiUrl = "https://api.openai.com/v1/"

        // Inisialisasi OkHttpClient dengan timeout yang ditingkatkan
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        // Inisialisasi Retrofit dengan OkHttpClient yang telah dikonfigurasi
        val retrofit = Retrofit.Builder()
            .baseUrl(apiUrl)
            .client(okHttpClient) // Menggunakan OkHttpClient dengan timeout yang ditingkatkan
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(OpenAIService::class.java)

        // Membuat JSON payload sesuai dengan format OpenAI API menggunakan Gson
        val messagesArray = JsonArray()

        val userMessageObject = JsonObject()
        userMessageObject.addProperty("role", userMessage["role"] as String)
        userMessageObject.addProperty("content", userMessage["content"] as String)

        messagesArray.add(userMessageObject)

        val payload = JsonObject()
        payload.addProperty("model", "gpt-4") // Pastikan model ini valid
        payload.add("messages", messagesArray)
        payload.addProperty("max_tokens", 300)

        // Membuat header Authorization
        val authHeader = "Bearer $openAIAPIKey"

        // Mengirim permintaan
        service.sendMessage(authHeader, payload)
            .enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.isSuccessful) {
                        response.body()?.let { handleOpenAIResponse(it) }
                        Log.d(TAG, "OpenAI response successful")
                    } else {
                        Toast.makeText(this@MainActivity, "Error: ${response.message()}", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "OpenAI response error: ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Failed to send message to OpenAI", t)
                }
            })
    }

    private fun handleOpenAIResponse(response: JsonObject) {
        try {
            Log.d(TAG, "Handling OpenAI response")
            val choices = response.getAsJsonArray("choices")
            if (choices.size() > 0) {
                val choice = choices.get(0).asJsonObject
                val message = choice.getAsJsonObject("message")

                val role = message.get("role").asString
                val content = message.get("content").asString
                val imgUrls = if (message.has("img_urls")) {
                    val jsonArray = message.getAsJsonArray("img_urls")
                    mutableListOf<String>().apply {
                        for (i in 0 until jsonArray.size()) {
                            add(jsonArray.get(i).asString)
                        }
                    }
                } else {
                    emptyList<String>()
                }

                val assistantMessage = mapOf(
                    "role" to role,
                    "content" to content,
                    "img_urls" to imgUrls
                )

                if (currentChatId == null) {
                    Toast.makeText(this, "Sesi percakapan belum dibuat.", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Current chat ID is null while adding assistant message")
                    return
                }

                firestore.collection("chats")
                    .document(currentChatId!!)
                    .update("messages", FieldValue.arrayUnion(assistantMessage))
                    .addOnSuccessListener {
                        // Pesan asisten berhasil ditambahkan
                        Log.d(TAG, "Assistant message added to Firestore")
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Gagal menambahkan pesan asisten", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Failed to add assistant message to Firestore", it)
                    }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error parsing response: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Error parsing OpenAI response", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Hapus listener jika sudah diinisialisasi
        messagesListener?.remove()
        Log.d(TAG, "MainActivity destroyed and listener removed")
    }
}
