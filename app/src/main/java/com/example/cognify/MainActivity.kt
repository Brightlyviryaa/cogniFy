package com.example.cognify

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cognify.adapters.MessageAdapter
import com.example.cognify.models.ChatSession
import com.example.cognify.models.Message
import com.example.cognify.network.*
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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

    // Daftar pesan & sesi
    private val messagesList = mutableListOf<Message>()
    private val chatSessionsList = mutableListOf<ChatSession>()

    // File upload (gambar)
    private val uploadedFiles = mutableListOf<File>()

    private var currentChatId: String? = null
    private var currentUserId: String? = null

    private val TAG = "MainActivity"
    private val activityScope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        private const val MENU_ITEM_CHAT_SESSION_BASE_ID = 1000

        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_CAMERA_PERMISSION = 2
    }

    private var photoFile: File? = null  // Tempat menyimpan foto dari kamera

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inisialisasi Firebase
        auth = FirebaseAuth.getInstance()

        // Hubungkan komponen UI
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recycler_view_messages)
        sendButton = findViewById(R.id.send_button)
        messageInput = findViewById(R.id.message_input)

        // Set up toolbar
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Navigation Drawer Header
        val headerView = navigationView.getHeaderView(0)
        userEmailTextView = headerView.findViewById(R.id.user_email)

        // RecyclerView
        messageAdapter = MessageAdapter(messagesList)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }

        // Listener Navigation Drawer
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_new_chat -> {
                    clearSessionState()
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
                    // Bagian dihapus: menu_profile, menu_settings
                    // ...
                    val chatSessionIndex = menuItem.itemId - MENU_ITEM_CHAT_SESSION_BASE_ID
                    if (chatSessionIndex >= 0 && chatSessionIndex < chatSessionsList.size) {
                        switchChatSession(chatSessionsList[chatSessionIndex])
                    }
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
            }
        }

        // Tombol kirim pesan
        sendButton.setOnClickListener {
            sendMessage()
        }

        // Cek user login
        val currentUser = auth.currentUser
        if (currentUser != null) {
            currentUserId = currentUser.uid
            userEmailTextView.text = currentUser.email
            checkEmailVerification(currentUser)
        } else {
            showLoggedOutUI()
            Log.d(TAG, "No user logged in")
        }
        updateMenuItems()
    }

    override fun onResume() {
        super.onResume()
        val currentUser = auth.currentUser
        if (currentUser != null && currentUserId == null) {
            currentUserId = currentUser.uid
            userEmailTextView.text = currentUser.email
            checkEmailVerification(currentUser)
            Log.d(TAG, "User logged in: ${currentUser.email}")
        } else if (currentUser == null && currentUserId != null) {
            currentUserId = null
            userEmailTextView.text = ""
            showLoggedOutUI()
            Log.d(TAG, "User logged out")
        }
    }

    // ====================================
    //     CAMERA INTENT (boleh dipakai)
    // ====================================
    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        } else {
            dispatchTakePictureIntent()
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            photoFile = createImageFile()
            photoFile?.also {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.provider",
                    it
                )
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            photoFile?.let { file ->
                if (file.exists()) {
                    uploadedFiles.add(file)
                    Toast.makeText(this, "Foto ditambahkan ke upload list", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if ((grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED)
            ) {
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ====================================
    //    CEK EMAIL VERIFIED
    // ====================================
    private fun checkEmailVerification(currentUser: com.google.firebase.auth.FirebaseUser) {
        currentUser.reload().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (!currentUser.isEmailVerified) {
                    AlertDialog.Builder(this)
                        .setTitle("Email Not Verified")
                        .setMessage("Please verify your email before using the dashboard.")
                        .setPositiveButton("Resend Verification") { _, _ ->
                            currentUser.sendEmailVerification()
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Verification Email Sent.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        this,
                                        "Failed sending email. Try again later.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                        .setNegativeButton("OK") { _, _ -> }
                        .setOnDismissListener {
                            auth.signOut()
                            showLoggedOutUI()
                        }
                        .show()
                } else {
                    showLoggedInUI()
                    loadChatSessions()
                }
            } else {
                Toast.makeText(this, "Error reloading user", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Failed reloading user: ${task.exception}")
            }
        }
    }

    private fun updateMenuItems() {
        val menu = navigationView.menu
        val isLoggedIn = auth.currentUser != null
        menu.findItem(R.id.menu_login)?.isVisible = !isLoggedIn
        menu.findItem(R.id.menu_logout)?.isVisible = isLoggedIn
    }

    private fun showLoggedInUI() {
        updateMenuItems()
    }

    private fun showLoggedOutUI() {
        updateMenuItems()
        showLoginRegisterDialog()
    }

    private fun showLoginRegisterDialog() {
        val dialog = Dialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_login_register, null, false)
        dialog.setContentView(view)

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

    // ====================================
    //  LOAD & DISPLAY SESSIONS
    // ====================================
    private fun loadChatSessions() {
        activityScope.launch {
            val currentUser = auth.currentUser ?: return@launch
            try {
                val idTokenResult = currentUser.getIdToken(false).await()
                val idToken = idTokenResult.token ?: ""
                if (idToken.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "User not authenticated",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val response = RetrofitClient.instance.getChatSessions("Bearer $idToken")
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        chatSessionsList.clear()
                        chatSessionsList.addAll(it.sessions)
                        populateChatSessionsToDrawer()
                        // Otomatis buka session pertama jika ada
                        if (it.sessions.isNotEmpty()) {
                            switchChatSession(it.sessions[0])
                        }
                    }
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Gagal memuat sesi percakapan",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Exception loadChatSessions: ${e.message}", e)
            }
        }
    }

    private fun populateChatSessionsToDrawer() {
        val menu = navigationView.menu
        val chatSessionsMenu = menu.findItem(R.id.menu_chat_sessions_heading)?.subMenu
        chatSessionsMenu?.clear()

        chatSessionsList.forEachIndexed { index, session ->
            val menuItemId = MENU_ITEM_CHAT_SESSION_BASE_ID + index
            val sessionTitle = session.judul ?: "Untitled"
            chatSessionsMenu?.add(Menu.NONE, menuItemId, Menu.NONE, sessionTitle)
                ?.setIcon(R.drawable.ic_chat_session)
        }
        messageAdapter.notifyDataSetChanged()
    }

    private fun clearSessionState() {
        currentChatId = null
        messagesList.clear()
        messageAdapter.notifyDataSetChanged()
        uploadedFiles.clear()
        messageInput.setText("")
        Toast.makeText(this, "New session started", Toast.LENGTH_SHORT).show()
    }

    private fun switchChatSession(chatSession: ChatSession) {
        messagesList.clear()
        messageAdapter.notifyDataSetChanged()

        currentChatId = chatSession.sessionId
        fetchMessages(chatSession.sessionId ?: "")
    }

    private fun fetchMessages(sessionId: String) {
        activityScope.launch {
            val currentUser = auth.currentUser ?: return@launch
            try {
                val idTokenResult = currentUser.getIdToken(false).await()
                val idToken = idTokenResult.token ?: ""
                if (idToken.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "User not authenticated",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val response = RetrofitClient.instance.getChatMessages("Bearer $idToken", sessionId)
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        messagesList.clear()
                        messagesList.addAll(it.messages)
                        messageAdapter.notifyDataSetChanged()
                        recyclerView.scrollToPosition(messagesList.size - 1)
                    }
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Gagal memuat pesan",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                Log.e(TAG, "fetchMessages error: ${e.message}", e)
            }
        }
    }

    // ====================================
    //      SEND MESSAGE
    // ====================================
    private fun sendMessage() {
        val text = messageInput.text?.toString()?.trim() ?: ""
        if (text.isEmpty() && uploadedFiles.isEmpty()) {
            Toast.makeText(this, "Pesan tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Anda harus login terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val sessionIdForAPI = currentChatId ?: ""

        activityScope.launch {
            try {
                val idTokenResult = currentUser.getIdToken(false).await()
                val idToken = idTokenResult.token ?: ""
                if (idToken.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "User not authenticated",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Buat multipart form data
                val formData = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("content", text)
                    .addFormDataPart("sessionId", sessionIdForAPI)

                uploadedFiles.forEach { file ->
                    val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    formData.addFormDataPart("images", file.name, reqFile)
                }
                val requestBody = formData.build()

                val response = RetrofitClient.instance.sendMessageRaw(
                    "Bearer $idToken",
                    requestBody
                )

                if (response.isSuccessful) {
                    val respBody = response.body()
                    val assistantMessage = respBody?.assistantMessage
                    if (assistantMessage != null) {
                        messagesList.add(assistantMessage)
                        messageAdapter.notifyItemInserted(messagesList.size - 1)
                        recyclerView.scrollToPosition(messagesList.size - 1)
                    }
                    // Bersihkan input
                    messageInput.text?.clear()
                    uploadedFiles.clear()
                    messageAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@MainActivity, "Gagal mengirim pesan", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                Log.e(TAG, "sendMessage error: ${e.message}", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
        Log.d(TAG, "MainActivity destroyed and coroutine scope cancelled")
    }
}
