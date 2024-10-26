package com.example.cognify

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var signupButton: Button
    private lateinit var clearDocumentButton: Button
    private lateinit var profileButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Authentication instance
        auth = FirebaseAuth.getInstance()

        // Link UI elements
        signupButton = findViewById(R.id.signup_button)
        clearDocumentButton = findViewById(R.id.clear_document_button)
        profileButton = findViewById(R.id.profile_button)

        // Check if user is logged in
        if (auth.currentUser != null) {
            showLoggedInUI()
        } else {
            showLoggedOutUI()
        }

        signupButton.setOnClickListener {
            showLoginRegisterDialog()
        }

        clearDocumentButton.setOnClickListener {
            clearDocument()
        }

        profileButton.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
    }

    private fun showLoggedInUI() {
        signupButton.visibility = View.GONE
        clearDocumentButton.visibility = View.VISIBLE
        profileButton.visibility = View.VISIBLE
    }

    private fun showLoggedOutUI() {
        signupButton.visibility = View.VISIBLE
        clearDocumentButton.visibility = View.GONE
        profileButton.visibility = View.GONE
    }

    private fun showLoginRegisterDialog() {
        val dialog = Dialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_login_register, null)
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

    private fun clearDocument() {
        Toast.makeText(this, "Document cleared!", Toast.LENGTH_SHORT).show()
        // Implement the clear document logic here if needed
    }
}
