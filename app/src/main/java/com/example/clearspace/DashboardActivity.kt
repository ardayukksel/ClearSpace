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

        // Challenge preferences summary
        val challengeSummary = buildChallengeSummary()

        if (isChallengeActive) {
            tvChallengeTitle.text = "Challenge active"
            tvChallengeSubtitle.text = "Complete to unlock"
            btnSetupChallenge.visibility = android.view.View.GONE
        } else {
            tvChallengeTitle.text = "Challenge preferences"
            tvChallengeSubtitle.text = challengeSummary
            btnSetupChallenge.visibility = android.view.View.VISIBLE
        }

        // Click listeners
        btnAddCommitment.setOnClickListener {
            // TODO: Open commitment dialog
        }

        btnSetupChallenge.setOnClickListener {
            startActivity(Intent(this, ChallengePreferencesActivity::class.java))
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
                    val intent = Intent(this, ChallengeActivity::class.java).apply {
                        putExtra(ChallengeActivity.EXTRA_MODE, ChallengeActivity.MODE_MANUAL)
                    }
                    startActivity(intent)
                    finish()
                    true
                }

                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val tvChallengeTitle = findViewById<TextView>(R.id.tvChallengeTitle)
        val tvChallengeSubtitle = findViewById<TextView>(R.id.tvChallengeSubtitle)
        val btnSetupChallenge = findViewById<Button>(R.id.btnSetupChallenge)

        if (stateManager.isChallengeActive()) {
            tvChallengeTitle.text = "Challenge active"
            tvChallengeSubtitle.text = "Complete to unlock"
            btnSetupChallenge.visibility = android.view.View.GONE
        } else {
            tvChallengeTitle.text = "Challenge preferences"
            tvChallengeSubtitle.text = buildChallengeSummary()
            btnSetupChallenge.visibility = android.view.View.VISIBLE
        }
    }

    private fun buildChallengeSummary(): String {
        val selected = mutableListOf<String>()

        if (stateManager.isBreathingChallengeEnabled()) selected.add("Breathing")
        if (stateManager.isTapChallengeEnabled()) selected.add("Rapid Tap")
        if (stateManager.isHoldChallengeEnabled()) selected.add("Hold")
        if (stateManager.isMathChallengeEnabled()) selected.add("Math")

        val randomEnabled = stateManager.isRandomChallengeEnabled()

        return when {
            randomEnabled && selected.isNotEmpty() ->
                "Random from: ${selected.joinToString(", ")}"

            randomEnabled ->
                "Random from all challenge types"

            selected.isNotEmpty() ->
                selected.joinToString(", ")

            else ->
                "Breathing"
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