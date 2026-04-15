package com.example.clearspace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.progressindicator.CircularProgressIndicator

class DashboardActivity : AppCompatActivity() {

    private lateinit var stateManager: ClearSpaceStateManager
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        stateManager = ClearSpaceStateManager(this)

        // Views
        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val tvSessionBadge = findViewById<TextView>(R.id.tvSessionBadge)
        val tvLimitBadge = findViewById<TextView>(R.id.tvLimitBadge)
        val tvTimerDisplay = findViewById<TextView>(R.id.tvTimerDisplay)
        val tvElapsedTime = findViewById<TextView>(R.id.tvElapsedTime)
        val tvRemainingTime = findViewById<TextView>(R.id.tvRemainingTime)
        val circularProgress = findViewById<CircularProgressIndicator>(R.id.circularProgress)
        val tvChallengeTitle = findViewById<TextView>(R.id.tvChallengeTitle)
        val tvChallengeSubtitle = findViewById<TextView>(R.id.tvChallengeSubtitle)
        val btnSetupChallenge = findViewById<Button>(R.id.btnSetupChallenge)
        val btnAddCommitment = findViewById<Button>(R.id.btnAddCommitment)
        val onboardingCard = findViewById<LinearLayout>(R.id.onboardingCard)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // Load user data
        val sharedPref = getSharedPreferences("ClearSpacePrefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("userName", "User") ?: "User"
        tvUserName.text = name

        // Load session data
        val timeLimit = stateManager.getTimeLimitMinutes()
        val monitoringEnabled = stateManager.isMonitoringEnabled()
        val isChallengeActive = stateManager.isChallengeActive()

        // Update UI based on session state
        tvLimitBadge.text = "Limit: $timeLimit min"

        if (monitoringEnabled) {
            tvSessionBadge.text = "Active"
            tvSessionBadge.setBackgroundResource(R.drawable.bg_session_badge_active)
        } else {
            tvSessionBadge.text = "No session"
            tvSessionBadge.setBackgroundResource(R.drawable.bg_session_badge)
        }

        // Timer display
        val totalSeconds = timeLimit * 60
        tvTimerDisplay.text = formatTime(totalSeconds)
        tvRemainingTime.text = "$timeLimit min"
        tvElapsedTime.text = "0 min"
        circularProgress.max = 100
        circularProgress.progress = 100

        // Challenge state
        if (isChallengeActive) {
            tvChallengeTitle.text = "Challenge active"
            tvChallengeSubtitle.text = "Complete to unlock"
            btnSetupChallenge.visibility = android.view.View.GONE
        } else {
            tvChallengeTitle.text = "No challenge set"
            tvChallengeSubtitle.text = "Set in the Focus tab"
            btnSetupChallenge.visibility = android.view.View.VISIBLE
        }

        // Click listeners
        btnAddCommitment.setOnClickListener {
            // TODO: Open commitment dialog
        }

        btnSetupChallenge.setOnClickListener {
            startActivity(Intent(this, ChallengeActivity::class.java))
        }

        onboardingCard.setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }

        // Bottom Navigation
        bottomNavigation.selectedItemId = R.id.nav_dashboard
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_dashboard -> true
                R.id.nav_focus -> {
                    startActivity(Intent(this, ChallengeActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun formatTime(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}