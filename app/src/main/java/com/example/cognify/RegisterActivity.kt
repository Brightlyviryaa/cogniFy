// src/main/java/com/example/cognify/RegisterActivity.kt

package com.example.cognify

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cognify.network.RegisterRequest
import com.example.cognify.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class RegisterActivity : AppCompatActivity() {

    // UI elements
    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var registerButton: Button
    private lateinit var loginText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize UI elements
        nameInput = findViewById(R.id.nameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        registerButton = findViewById(R.id.registerButton)
        loginText = findViewById(R.id.loginText)
        progressBar = findViewById(R.id.progressBar)

        // Set click listener untuk tombol register
        registerButton.setOnClickListener {
            val username = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                // Tambahkan validasi tambahan jika diperlukan
                registerUser(username, email, password)
            } else {
                Toast.makeText(this, "Silakan masukkan nama, email, dan password.", Toast.LENGTH_SHORT).show()
            }
        }

        // Set click listener untuk teks "Already have an account"
        loginText.setOnClickListener {
            // Pergi ke LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // Fungsi untuk mendaftarkan pengguna dengan backend
    private fun registerUser(username: String, email: String, password: String) {
        registerButton.isEnabled = false // Nonaktifkan tombol untuk mencegah multiple click
        progressBar.visibility = View.VISIBLE // Tampilkan ProgressBar

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Buat permintaan pendaftaran
                val request = RegisterRequest(
                    email = email,
                    password = password,
                    username = username
                )

                val response = RetrofitClient.instance.register(request)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE // Sembunyikan ProgressBar
                    if (response.isSuccessful) {
                        val registerResponse = response.body()
                        Toast.makeText(
                            this@RegisterActivity,
                            registerResponse?.message ?: "Registrasi berhasil!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Redirect ke LoginActivity setelah registrasi berhasil
                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        // Tangani respons error dari backend
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(
                            this@RegisterActivity,
                            errorBody ?: "Registrasi gagal: ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()
                        registerButton.isEnabled = true
                    }
                }
            } catch (e: IOException) {
                // Kesalahan jaringan
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE // Sembunyikan ProgressBar
                    Toast.makeText(
                        this@RegisterActivity,
                        "Kesalahan jaringan. Silakan coba lagi.",
                        Toast.LENGTH_SHORT
                    ).show()
                    registerButton.isEnabled = true
                }
            } catch (e: HttpException) {
                // Kesalahan HTTP
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE // Sembunyikan ProgressBar
                    Toast.makeText(
                        this@RegisterActivity,
                        "Kesalahan server. Silakan coba lagi.",
                        Toast.LENGTH_SHORT
                    ).show()
                    registerButton.isEnabled = true
                }
            } catch (e: Exception) {
                // Kesalahan lainnya
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE // Sembunyikan ProgressBar
                    Toast.makeText(
                        this@RegisterActivity,
                        "Terjadi kesalahan tak terduga.",
                        Toast.LENGTH_SHORT
                    ).show()
                    registerButton.isEnabled = true
                }
            }
        }
    }
}
