package com.example.cognify

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EditProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Just a toast message to confirm activity load for now
        Toast.makeText(this, "Edit Profile Screen", Toast.LENGTH_SHORT).show()
    }
}
