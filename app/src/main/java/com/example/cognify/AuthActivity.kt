package com.example.cognify

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        // Show dialog to choose login or register
        showLoginRegisterDialog()
    }

    private fun showLoginRegisterDialog() {
        // Create a new dialog and set its content view
        val dialog = Dialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_login_register, null)
        dialog.setContentView(view)

        // Find buttons in the dialog
        val loginButton = view.findViewById<Button>(R.id.loginButton)
        val registerButton = view.findViewById<Button>(R.id.registerButton)

        // Set up click listener for login button
        loginButton.setOnClickListener {
            // Navigate to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
            dialog.dismiss()
        }

        // Set up click listener for register button
        registerButton.setOnClickListener {
            // Navigate to RegisterActivity
            startActivity(Intent(this, RegisterActivity::class.java))
            dialog.dismiss()
        }

        // Show the dialog
        dialog.show()
    }
}
