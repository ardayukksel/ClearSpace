package com.example.clearspace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.clearspace.data.network.RetrofitClient
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.launch
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var stateManager: ClearSpaceStateManager
    private var countDownTimer: CountDownTimer? = null

    private lateinit var tvUserName: TextView
    private lateinit var tvSessionBadge: TextView
    private lateinit var tvLimitBadge: TextView
    private lateinit var tvTimerDisplay: TextView
    private lateinit var tvElapsedTime: TextView
    private lateinit var tvRemainingTime: TextView
    private lateinit var circularProgress: CircularProgressIndicator
    private lateinit var tvChallengeTitle: TextView
    private lateinit var tvChallengeSubtitle: TextView
    private lateinit var btnSetupChallenge: Button
    private lateinit var btnAddCommitment: Button
    private lateinit var onboardingCard: LinearLayout
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var tvCurrentStreak: TextView
    private lateinit var tvLongestStreak: TextView
    private lateinit var tvLastStreakDate: TextView
    private lateinit var tvPoints: TextView
    private lateinit var tvLevel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        stateManager = ClearSpaceStateManager(this)

        initViews()
        loadUserData()
        loadSessionData()
        refreshChallengeSection()
        setupClickListeners()
        setupBottomNavigation()
        loadGamificationData()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
        refreshChallengeSection()
        loadSessionData()
        startCountdownRefresh()
        loadGamificationData()
        bottomNavigation.selectedItemId = R.id.nav_dashboard
    }

    override fun onPause() {
        super.onPause()
        stopCountdownRefresh()
    }

    private fun initViews() {
        tvUserName = findViewById(R.id.tvUserName)
        tvSessionBadge = findViewById(R.id.tvSessionBadge)
        tvLimitBadge = findViewById(R.id.tvLimitBadge)
        tvTimerDisplay = findViewById(R.id.tvTimerDisplay)
        tvElapsedTime = findViewById(R.id.tvElapsedTime)
        tvRemainingTime = findViewById(R.id.tvRemainingTime)
        circularProgress = findViewById(R.id.circularProgress)
        tvChallengeTitle = findViewById(R.id.tvChallengeTitle)
        tvChallengeSubtitle = findViewById(R.id.tvChallengeSubtitle)
        btnSetupChallenge = findViewById(R.id.btnSetupChallenge)
        btnAddCommitment = findViewById(R.id.btnAddCommitment)
        onboardingCard = findViewById(R.id.onboardingCard)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        tvCurrentStreak = findViewById(R.id.tvCurrentStreak)
        tvLongestStreak = findViewById(R.id.tvLongestStreak)
        tvLastStreakDate = findViewById(R.id.tvLastStreakDate)
        tvPoints = findViewById(R.id.tvPoints)
        tvLevel = findViewById(R.id.tvLevel)
    }

    private fun loadUserData() {
        val rawName = stateManager.getLoggedInUserName().ifBlank {
            val sharedPref = getSharedPreferences("ClearSpacePrefs", Context.MODE_PRIVATE)
            sharedPref.getString("userName", "User") ?: "User"
        }

        tvUserName.text = formatDisplayName(rawName)
    }

    private fun loadSessionData() {
        val timeLimit = stateManager.getTimeLimitMinutes().coerceAtLeast(1)
        val monitoringEnabled = stateManager.isMonitoringEnabled()
        val limitMs = timeLimit * 60_000L
        val elapsedMs = stateManager.getElapsedSessionMs()
        val remainingMs = stateManager.getRemainingSessionMs()

        tvLimitBadge.text = "Limit: ${formatMinutesLabel(timeLimit)}"

        if (monitoringEnabled) {
            tvSessionBadge.text = if (stateManager.isTargetAppSessionActive()) "Active" else "Paused"
            tvSessionBadge.setBackgroundResource(R.drawable.bg_session_badge_active)
        } else {
            tvSessionBadge.text = "No session"
            tvSessionBadge.setBackgroundResource(R.drawable.bg_session_badge)
        }

        tvTimerDisplay.text = formatTimeFromMs(remainingMs)
        tvRemainingTime.text = formatMinutesAndSecondsShort(remainingMs)
        tvElapsedTime.text = formatMinutesAndSecondsShort(elapsedMs)

        circularProgress.max = 1000
        val progress = if (limitMs <= 0L) {
            0
        } else {
            ((remainingMs.toDouble() / limitMs.toDouble()) * 1000.0).toInt().coerceIn(0, 1000)
        }
        circularProgress.progress = progress
    }

    private fun startCountdownRefresh() {
        stopCountdownRefresh()

        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                loadSessionData()
            }

            override fun onFinish() = Unit
        }.start()
    }

    private fun stopCountdownRefresh() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    private fun refreshChallengeSection() {
        if (stateManager.isChallengeActive()) {
            tvChallengeTitle.text = "Challenge active"
            tvChallengeSubtitle.text = "Complete to unlock"
            btnSetupChallenge.visibility = View.GONE
        } else {
            tvChallengeTitle.text = "Challenge preferences"
            tvChallengeSubtitle.text = buildChallengeSummary()
            btnSetupChallenge.visibility = View.VISIBLE
        }
    }

    private fun loadGamificationData() {
        val userId = stateManager.getLoggedInUserId()

        if (userId <= 0) {
            showGamificationFallback()
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getUserGamification(userId)
                val data = response.gamification

                tvCurrentStreak.text = "Current streak: ${data.current_streak} day(s)"
                tvLongestStreak.text = "Longest streak: ${data.longest_streak} day(s)"
                tvLastStreakDate.text = "Last completed day: ${data.last_streak_date ?: "--"}"
                tvPoints.text = "Points: ${data.points}"
                tvLevel.text = "Level: ${data.level}"
            } catch (e: Exception) {
                showGamificationFallback()
            }
        }
    }

    private fun showGamificationFallback() {
        tvCurrentStreak.text = "Current streak: --"
        tvLongestStreak.text = "Longest streak: --"
        tvLastStreakDate.text = "Last completed day: --"
        tvPoints.text = "Points: --"
        tvLevel.text = "Level: --"
    }

    private fun setupClickListeners() {
        btnAddCommitment.setOnClickListener {
            // Add your commitment feature later
        }

        btnSetupChallenge.setOnClickListener {
            startActivity(Intent(this, ChallengePreferencesActivity::class.java))
        }

        onboardingCard.setOnClickListener {
            val intent = Intent(this, OnboardingActivity::class.java).apply {
                putExtra(OnboardingActivity.EXTRA_REVIEW_MODE, true)
            }
            startActivity(intent)
        }
    }

    private fun setupBottomNavigation() {
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
                    true
                }

                else -> false
            }
        }
    }

    private fun buildChallengeSummary(): String {
        return when {
            stateManager.isRandomChallengeEnabled() -> "Random"
            stateManager.isBreathingChallengeEnabled() &&
                    !stateManager.isTapChallengeEnabled() &&
                    !stateManager.isHoldChallengeEnabled() &&
                    !stateManager.isMathChallengeEnabled() -> "Breathing"

            else -> {
                val selected = mutableListOf<String>()
                if (stateManager.isBreathingChallengeEnabled()) selected.add("Breathing")
                if (stateManager.isTapChallengeEnabled()) selected.add("Rapid Tap")
                if (stateManager.isHoldChallengeEnabled()) selected.add("Hold")
                if (stateManager.isMathChallengeEnabled()) selected.add("Math")

                if (selected.isNotEmpty()) {
                    selected.joinToString(", ")
                } else {
                    "Breathing"
                }
            }
        }
    }

    private fun formatTimeFromMs(ms: Long): String {
        val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun formatMinutesAndSecondsShort(ms: Long): String {
        val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L

        return when {
            minutes > 0L && seconds > 0L -> "${minutes}m ${seconds}s"
            minutes > 0L -> "${minutes} min"
            else -> "${seconds} sec"
        }
    }

    private fun formatMinutesLabel(minutes: Int): String {
        return when {
            minutes == 60 -> "1 hour"
            minutes < 60 -> "$minutes min"
            minutes % 60 == 0 -> "${minutes / 60} hours"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }

    private fun formatDisplayName(rawName: String): String {
        val trimmed = rawName.trim()
        if (trimmed.isBlank()) return "User"

        return trimmed
            .lowercase(Locale.getDefault())
            .split(Regex("\\s+"))
            .joinToString(" ") { part ->
                part.replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCountdownRefresh()
    }
}