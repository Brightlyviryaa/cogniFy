package com.example.cognify

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    // Firebase Authentication instance
    private lateinit var auth: FirebaseAuth

    // Firebase Firestore instance
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI elements
        val nameInput = findViewById<EditText>(R.id.nameInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val loginText = findViewById<TextView>(R.id.loginText)

        // Set click listener for the register button
        registerButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                registerUser(name, email, password)
            } else {
                Toast.makeText(this, "Silakan masukkan nama, email, dan password.", Toast.LENGTH_SHORT).show()
            }
        }

        // Set click listener for "Already have an account" text
        loginText.setOnClickListener {
            // Go back to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // Function to register user with Firebase Authentication and Firestore
    private fun registerUser(name: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registration successful
                    val firebaseUser = auth.currentUser
                    firebaseUser?.let {
                        val uid = it.uid

                        // Create a new user document in Firestore
                        val userMap = hashMapOf(
                            "name" to name,
                            "email" to email,
                            "uid" to uid,
                            "createdAt" to System.currentTimeMillis()
                            // Tambahkan field lainnya sesuai kebutuhan
                        )

                        firestore.collection("users").document(uid)
                            .set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Registrasi berhasil!", Toast.LENGTH_SHORT).show()
                                // Redirect to LoginActivity after successful registration
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()  // Close RegisterActivity
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Gagal menyimpan data pengguna: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    // Registration failed
                    Toast.makeText(this, "Registrasi gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
