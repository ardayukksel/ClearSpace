package com.example.clearspace

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class ChallengeActivity : AppCompatActivity() {

    companion object {
        var isVisible: Boolean = false

        const val EXTRA_MODE = "extra_mode"
        const val MODE_LOCKED = "locked"
        const val MODE_MANUAL = "manual"
    }

    private enum class BreathPhase(
        val label: String,
        val seconds: Int
    ) {
        INHALE("Breathe In", 4),
        HOLD("Hold", 2),
        EXHALE("Breathe Out", 4)
    }

    private var isUnlocking = false
    private var isRelaunchScheduled = false
    private val relaunchHandler = Handler(Looper.getMainLooper())
    private var countDownTimer: CountDownTimer? = null

    private lateinit var stateManager: ClearSpaceStateManager
    private lateinit var tvChallengeTitle: TextView
    private lateinit var tvChallengeSubtitle: TextView
    private lateinit var tvPhase: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvProgress: TextView
    private lateinit var btnUnlock: Button

    private var challengeMode: String = MODE_LOCKED

    private val phaseSequence = listOf(
        BreathPhase.INHALE,
        BreathPhase.HOLD,
        BreathPhase.EXHALE,
        BreathPhase.INHALE,
        BreathPhase.HOLD,
        BreathPhase.EXHALE
    )

    private var currentPhaseIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        setContentView(R.layout.activity_challenge)

        stateManager = ClearSpaceStateManager(this)
        challengeMode = intent.getStringExtra(EXTRA_MODE) ?: MODE_LOCKED

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Blocked intentionally while challenge is active
            }
        })

        tvChallengeTitle = findViewById(R.id.tv_challenge_title)
        tvChallengeSubtitle = findViewById(R.id.tv_challenge_subtitle)
        tvPhase = findViewById(R.id.tv_phase)
        tvTimer = findViewById(R.id.tv_timer)
        tvProgress = findViewById(R.id.tv_progress)
        btnUnlock = findViewById(R.id.btn_unlock)

        btnUnlock.visibility = View.GONE
        btnUnlock.isEnabled = false

        if (challengeMode == MODE_LOCKED) {
            stateManager.setChallengeActive(true)
            stopService(Intent(this, OverlayService::class.java))
        } else {
            stateManager.setChallengeActive(false)
        }

        startBreathingChallenge()

        btnUnlock.setOnClickListener {
            if (challengeMode == MODE_MANUAL) {
                finishManualFocus()
            } else {
                unlockAndReturnToTargetApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        challengeMode = intent.getStringExtra(EXTRA_MODE) ?: MODE_LOCKED
        overridePendingTransition(0, 0)
        relaunchHandler.removeCallbacksAndMessages(null)
        isRelaunchScheduled = false
    }

    override fun onResume() {
        super.onResume()
        isVisible = true
        isRelaunchScheduled = false
        overridePendingTransition(0, 0)

        if (challengeMode == MODE_LOCKED) {
            stopService(Intent(this, OverlayService::class.java))
        }
    }

    override fun onStop() {
        super.onStop()
        isVisible = false

        if (challengeMode == MODE_LOCKED) {
            enforceChallengeIfNeeded()
        }
    }

    private fun startBreathingChallenge() {
        currentPhaseIndex = 0
        btnUnlock.visibility = View.GONE
        btnUnlock.isEnabled = false

        if (challengeMode == MODE_MANUAL) {
            tvChallengeTitle.text = "Focus Reset"
            tvChallengeSubtitle.text = "Take a short breathing break, then return when you're ready."
        } else {
            tvChallengeTitle.text = "Pause & Reflect"
            tvChallengeSubtitle.text = "Complete the breathing exercise before you continue."
        }

        runCurrentPhase()
    }

    private fun runCurrentPhase() {
        if (currentPhaseIndex >= phaseSequence.size) {
            finishBreathingChallenge()
            return
        }

        val currentPhase = phaseSequence[currentPhaseIndex]
        val totalPhases = phaseSequence.size

        tvPhase.text = currentPhase.label
        tvProgress.text = "Cycle ${currentPhaseIndex + 1} / $totalPhases"

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(currentPhase.seconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000L).toInt() + 1
                tvTimer.text = secondsLeft.toString()
            }

            override fun onFinish() {
                tvTimer.text = "0"
                currentPhaseIndex++
                runCurrentPhase()
            }
        }.start()
    }

    private fun finishBreathingChallenge() {
        tvPhase.text = "Ready"
        tvTimer.text = "✓"
        tvProgress.text = "Challenge complete"

        if (challengeMode == MODE_MANUAL) {
            tvChallengeSubtitle.text = "Nice. Return to ClearSpace when you're ready."
            btnUnlock.text = "Back to ClearSpace"
        } else {
            tvChallengeSubtitle.text = "Nice. You can return to your app now."
            btnUnlock.text = "Unlock App"
        }

        btnUnlock.visibility = View.VISIBLE
        btnUnlock.isEnabled = true
    }

    private fun enforceChallengeIfNeeded() {
        if (isUnlocking || isRelaunchScheduled) return

        val isLocked = stateManager.isLocked()
        val isChallengeActive = stateManager.isChallengeActive()

        if (isLocked && isChallengeActive) {
            isRelaunchScheduled = true
            relaunchHandler.removeCallbacksAndMessages(null)
            relaunchHandler.postDelayed({
                isRelaunchScheduled = false

                if (!stateManager.isLocked() || !stateManager.isChallengeActive() || isUnlocking) {
                    return@postDelayed
                }

                if (isVisible) {
                    return@postDelayed
                }

                val challengeIntent = Intent(this, ChallengeActivity::class.java).apply {
                    putExtra(EXTRA_MODE, MODE_LOCKED)
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_NO_ANIMATION
                    )
                }
                startActivity(challengeIntent)
                overridePendingTransition(0, 0)
            }, 100)
        }
    }

    private fun finishManualFocus() {
        isUnlocking = true
        btnUnlock.isEnabled = false
        stateManager.setChallengeActive(false)
        finish()
        overridePendingTransition(0, 0)
    }

    private fun unlockAndReturnToTargetApp() {
        val targetPackage = stateManager.getTargetAppPackage()

        isUnlocking = true
        relaunchHandler.removeCallbacksAndMessages(null)
        isRelaunchScheduled = false
        btnUnlock.isEnabled = false

        val success = stateManager.clearAfterUnlock()

        if (!success) {
            Toast.makeText(this, "Failed to unlock app state.", Toast.LENGTH_SHORT).show()
            btnUnlock.isEnabled = true
            isUnlocking = false
            return
        }

        stopService(Intent(this, OverlayService::class.java))

        if (targetPackage.isBlank()) {
            Toast.makeText(this, "No target app selected.", Toast.LENGTH_SHORT).show()
            finish()
            overridePendingTransition(0, 0)
            return
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)

        if (launchIntent == null) {
            Toast.makeText(this, "Could not reopen target app.", Toast.LENGTH_SHORT).show()
            finish()
            overridePendingTransition(0, 0)
            return
        }

        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        )

        startActivity(launchIntent)

        finish()
        overridePendingTransition(0, 0)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        relaunchHandler.removeCallbacksAndMessages(null)
        isVisible = false
        super.onDestroy()
    }
}