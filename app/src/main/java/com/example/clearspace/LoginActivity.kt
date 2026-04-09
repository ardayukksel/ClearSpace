package com.example.clearspace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.et_email)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val tvForgotPassword = findViewById<TextView>(R.id.tv_forgot_password)
        val tvSignup = findViewById<TextView>(R.id.tv_signup)

        val sharedPref = getSharedPreferences("ClearSpacePrefs", Context.MODE_PRIVATE)
        val hasCompletedOnboarding = sharedPref.getBoolean("hasCompletedOnboarding", false)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Please enter your email and password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val name = email.substringBefore("@").replace(Regex("[._]"), " ").trim()
            sharedPref.edit()
                .putString("userName", if (name.isBlank()) "User" else name)
                .putString("userEmail", email)
                .apply()

            val nextActivity = if (hasCompletedOnboarding) {
                MainActivity::class.java
            } else {
                OnboardingActivity::class.java
            }

            startActivity(Intent(this, nextActivity))
            finish()
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Forgot Password flow not implemented yet.", Toast.LENGTH_SHORT).show()
        }

        tvSignup.setOnClickListener {
            Toast.makeText(this, "Sign Up flow not implemented yet.", Toast.LENGTH_SHORT).show()
        }
    }
}