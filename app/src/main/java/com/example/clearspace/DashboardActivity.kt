package com.example.clearspace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val tvUserName = findViewById<TextView>(R.id.tv_user_name)
        val tvUserGoal = findViewById<TextView>(R.id.tv_user_goal)
        val btnReviewOnboarding = findViewById<Button>(R.id.btn_review_onboarding)
        val btnBackHome = findViewById<Button>(R.id.btn_back_home)

        // Fetch user data saved during Onboarding/Login
        val sharedPref = getSharedPreferences("ClearSpacePrefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("userName", "User") ?: "User"
        val goal = sharedPref.getString("userGoal", "Focus on what matters.")

        tvUserName.text = "Hello, $name"
        tvUserGoal.text = "My Goal: $goal"

        // Navigate back to Onboarding to review steps
        btnReviewOnboarding.setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }

        // Return to Main/Home Screen
        btnBackHome.setOnClickListener {
            finish()
        }
    }
}