package com.example.clearspace

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnLogin = findViewById<Button>(R.id.btn_login)

        // For now, clicking login skips real authentication and goes to Onboarding
        btnLogin.setOnClickListener {
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)

            // Finish this activity so user can't go back to log in by pressing back button
            finish()
        }
    }
}