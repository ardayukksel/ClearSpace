package com.example.clearspace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    private lateinit var stateManager: ClearSpaceStateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        stateManager = ClearSpaceStateManager(this)

        val tvUserName = findViewById<TextView>(R.id.tv_user_name)
        val tvUserGoal = findViewById<TextView>(R.id.tv_user_goal)
        val tvDashboardApp = findViewById<TextView>(R.id.tv_dashboard_app)
        val tvDashboardLimit = findViewById<TextView>(R.id.tv_dashboard_limit)
        val tvDashboardMonitoring = findViewById<TextView>(R.id.tv_dashboard_monitoring)
        val btnReviewOnboarding = findViewById<Button>(R.id.btn_review_onboarding)
        val btnGoSetup = findViewById<Button>(R.id.btn_go_setup)
        val btnBackHome = findViewById<Button>(R.id.btn_back_home)

        val sharedPref = getSharedPreferences("ClearSpacePrefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("userName", "User") ?: "User"
        val goal = sharedPref.getString("userGoal", "Focus on what matters.") ?: "Focus on what matters."

        tvUserName.text = "Hello, $name"
        tvUserGoal.text = "Goal: $goal"

        val targetAppName = stateManager.getTargetAppName()
        val timeLimit = stateManager.getTimeLimitMinutes()
        val monitoringEnabled = stateManager.isMonitoringEnabled()

        tvDashboardApp.text = "Blocked App: ${if (targetAppName == "None") "Not selected" else targetAppName}"
        tvDashboardLimit.text = "Current Limit: $timeLimit min"
        tvDashboardMonitoring.text = "Monitoring: ${if (monitoringEnabled) "ON" else "OFF"}"

        btnReviewOnboarding.setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }

        btnGoSetup.setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }

        btnBackHome.setOnClickListener {
            finish()
        }
    }
}