// src/main/java/com/example/cognify/LoginActivity.kt

package com.example.cognify

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cognify.network.LoginRequest
import com.example.cognify.network.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth

    // UI elements
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var forgotPassword: TextView
    private lateinit var registerText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize UI elements
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        forgotPassword = findViewById(R.id.forgotPassword)
        registerText = findViewById(R.id.registerText)

        // Pengecekan status autentikasi saat aktivitas dibuat
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Jika sudah login, arahkan ke MainActivity
            navigateToMainActivity()
            return
        }

        // Set click listener untuk tombol login
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Please enter both email and password.", Toast.LENGTH_SHORT).show()
            }
        }

        // Set click listener untuk teks lupa password
        forgotPassword.setOnClickListener {
            val email = emailInput.text.toString().trim()
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Password reset email sent.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Failed to send reset email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please enter your email to reset password.", Toast.LENGTH_SHORT).show()
            }
        }

        // Set click listener untuk teks register
        registerText.setOnClickListener {
            // Navigasi ke RegisterActivity
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // Fungsi untuk login pengguna dengan Firebase Authentication dan backend
    private fun loginUser(email: String, password: String) {
        loginButton.isEnabled = false // Nonaktifkan tombol untuk mencegah multiple click
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login berhasil
                    val user = auth.currentUser
                    user?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            val idToken = tokenTask.result?.token
                            if (idToken != null) {
                                // Kirim ID Token ke backend
                                sendIdTokenToBackend(idToken)
                            } else {
                                Toast.makeText(this, "Failed to retrieve ID token.", Toast.LENGTH_SHORT).show()
                                loginButton.isEnabled = true
                            }
                        } else {
                            Toast.makeText(this, "Failed to retrieve ID token.", Toast.LENGTH_SHORT).show()
                            loginButton.isEnabled = true
                        }
                    }
                } else {
                    // Login gagal
                    handleAuthError(task.exception)
                    loginButton.isEnabled = true
                }
            }
    }

    // Fungsi untuk mengirim ID Token ke backend
    private fun sendIdTokenToBackend(idToken: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.login(LoginRequest(idToken))
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        Toast.makeText(this@LoginActivity, loginResponse?.message ?: "Login successful!", Toast.LENGTH_SHORT).show()
                        navigateToMainActivity()
                    } else {
                        if (response.code() == 403) {
                            Toast.makeText(this@LoginActivity, "Email not verified. Please check your inbox.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@LoginActivity, "Login failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                        loginButton.isEnabled = true
                    }
                }
            } catch (e: IOException) {
                // Jaringan error
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Network error. Please try again.", Toast.LENGTH_SHORT).show()
                    loginButton.isEnabled = true
                }
            } catch (e: HttpException) {
                // HTTP error
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Server error. Please try again.", Toast.LENGTH_SHORT).show()
                    loginButton.isEnabled = true
                }
            } catch (e: Exception) {
                // Kesalahan lainnya
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "An unexpected error occurred.", Toast.LENGTH_SHORT).show()
                    loginButton.isEnabled = true
                }
            }
        }
    }

    // Fungsi untuk menangani kesalahan autentikasi
    private fun handleAuthError(exception: Exception?) {
        when (exception) {
            is FirebaseAuthInvalidCredentialsException -> {
                Toast.makeText(this, "Invalid password. Please try again.", Toast.LENGTH_SHORT).show()
            }
            is FirebaseAuthInvalidUserException -> {
                when (exception.errorCode) {
                    "ERROR_USER_NOT_FOUND" -> {
                        Toast.makeText(this, "Email not registered. Please sign up first.", Toast.LENGTH_SHORT).show()
                    }
                    "ERROR_USER_DISABLED" -> {
                        Toast.makeText(this, "User account is disabled.", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        // Menangani kesalahan lainnya dalam FirebaseAuthInvalidUserException
                        Toast.makeText(this, "Authentication failed: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            is FirebaseAuthException -> {
                when (exception.errorCode) {
                    "ERROR_TOO_MANY_REQUESTS" -> {
                        Toast.makeText(this, "Too many login attempts. Please try again later.", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(this, "Authentication failed: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else -> {
                Toast.makeText(this, "Login failed: ${exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fungsi untuk navigasi ke MainActivity
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        // Membersihkan tumpukan aktivitas
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
