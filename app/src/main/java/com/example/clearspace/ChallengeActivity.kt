package com.example.clearspace

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.clearspace.data.network.CompleteChallengeRequest
import com.example.clearspace.data.network.RetrofitClient
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.random.Random

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

    private enum class ChallengeType {
        BREATHING,
        RAPID_TAP,
        HOLD_STILL,
        SIMPLE_MATH
    }

    private var isUnlocking = false
    private var isRelaunchScheduled = false
    private var hasReportedCompletion = false

    private val relaunchHandler = Handler(Looper.getMainLooper())
    private var countDownTimer: CountDownTimer? = null

    private lateinit var stateManager: ClearSpaceStateManager
    private lateinit var tvChallengeTitle: TextView
    private lateinit var tvChallengeSubtitle: TextView
    private lateinit var tvPhase: TextView
    private lateinit var tvTimer: TextView
    private lateinit var tvProgress: TextView
    private lateinit var etAnswer: EditText
    private lateinit var btnAction: Button
    private lateinit var btnUnlock: Button

    private var challengeMode: String = MODE_LOCKED
    private var currentChallengeType: ChallengeType = ChallengeType.BREATHING

    private val phaseSequence = listOf(
        BreathPhase.INHALE,
        BreathPhase.HOLD,
        BreathPhase.EXHALE,
        BreathPhase.INHALE,
        BreathPhase.HOLD,
        BreathPhase.EXHALE
    )
    private var currentPhaseIndex = 0

    private var tapTarget = 20
    private var tapCount = 0

    private var holdRequiredMs = 4000L
    private var holdStartTime = 0L
    private var isHolding = false

    private val holdHandler = Handler(Looper.getMainLooper())
    private val holdRunnable = object : Runnable {
        override fun run() {
            if (!isHolding) return

            val elapsed = System.currentTimeMillis() - holdStartTime
            val remaining = max(0L, holdRequiredMs - elapsed)
            val secondsLeft = ((remaining + 999L) / 1000L).toInt()

            tvTimer.text = secondsLeft.toString()
            tvProgress.text = "Keep holding..."

            if (elapsed >= holdRequiredMs) {
                isHolding = false
                finishChallengeSuccess()
            } else {
                holdHandler.postDelayed(this, 50L)
            }
        }
    }

    private var mathAnswer = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        setContentView(R.layout.activity_challenge)

        stateManager = ClearSpaceStateManager(this)
        challengeMode = resolveMode(intent)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // blocked intentionally
            }
        })

        tvChallengeTitle = findViewById(R.id.tv_challenge_title)
        tvChallengeSubtitle = findViewById(R.id.tv_challenge_subtitle)
        tvPhase = findViewById(R.id.tv_phase)
        tvTimer = findViewById(R.id.tv_timer)
        tvProgress = findViewById(R.id.tv_progress)
        etAnswer = findViewById(R.id.et_answer)
        btnAction = findViewById(R.id.btn_action)
        btnUnlock = findViewById(R.id.btn_unlock)

        applyModeState()
        startSelectedChallenge()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        challengeMode = resolveMode(intent)
        overridePendingTransition(0, 0)

        relaunchHandler.removeCallbacksAndMessages(null)
        holdHandler.removeCallbacksAndMessages(null)
        countDownTimer?.cancel()

        isRelaunchScheduled = false
        isUnlocking = false
        isHolding = false
        hasReportedCompletion = false

        applyModeState()
        startSelectedChallenge()
    }

    override fun onResume() {
        super.onResume()
        isVisible = true
        isRelaunchScheduled = false
        overridePendingTransition(0, 0)

        if (challengeMode == MODE_LOCKED) {
            stopService(Intent(this, OverlayService::class.java))
        } else {
            stateManager.setChallengeActive(false)
        }
    }

    override fun onStop() {
        super.onStop()
        isVisible = false

        if (challengeMode == MODE_LOCKED) {
            enforceChallengeIfNeeded()
        }
    }

    private fun resolveMode(intent: Intent?): String {
        val mode = intent?.getStringExtra(EXTRA_MODE)
        return if (mode == MODE_MANUAL) MODE_MANUAL else MODE_LOCKED
    }

    private fun applyModeState() {
        btnUnlock.visibility = View.GONE
        btnUnlock.isEnabled = false
        btnUnlock.text = ""
        btnUnlock.setOnClickListener(null)

        if (challengeMode == MODE_LOCKED) {
            stateManager.setChallengeActive(true)
            stopService(Intent(this, OverlayService::class.java))
        } else {
            stateManager.setChallengeActive(false)
        }
    }

    private fun startSelectedChallenge() {
        hasReportedCompletion = false
        resetDynamicViews()
        currentChallengeType = pickChallengeFromPreferences()

        when (currentChallengeType) {
            ChallengeType.BREATHING -> startBreathingChallenge()
            ChallengeType.RAPID_TAP -> startRapidTapChallenge()
            ChallengeType.HOLD_STILL -> startHoldChallenge()
            ChallengeType.SIMPLE_MATH -> startMathChallenge()
        }
    }

    private fun pickChallengeFromPreferences(): ChallengeType {
        val breathingEnabled = stateManager.isBreathingChallengeEnabled()
        val tapEnabled = stateManager.isTapChallengeEnabled()
        val holdEnabled = stateManager.isHoldChallengeEnabled()
        val mathEnabled = stateManager.isMathChallengeEnabled()
        val randomEnabled = stateManager.isRandomChallengeEnabled()

        val selectedChallenges = mutableListOf<ChallengeType>()

        if (breathingEnabled) selectedChallenges.add(ChallengeType.BREATHING)
        if (tapEnabled) selectedChallenges.add(ChallengeType.RAPID_TAP)
        if (holdEnabled) selectedChallenges.add(ChallengeType.HOLD_STILL)
        if (mathEnabled) selectedChallenges.add(ChallengeType.SIMPLE_MATH)

        return when {
            randomEnabled && selectedChallenges.isNotEmpty() -> selectedChallenges.random()
            randomEnabled && selectedChallenges.isEmpty() -> {
                listOf(
                    ChallengeType.BREATHING,
                    ChallengeType.RAPID_TAP,
                    ChallengeType.HOLD_STILL,
                    ChallengeType.SIMPLE_MATH
                ).random()
            }
            selectedChallenges.isNotEmpty() -> selectedChallenges.random()
            else -> ChallengeType.BREATHING
        }
    }

    private fun resetDynamicViews() {
        countDownTimer?.cancel()
        holdHandler.removeCallbacksAndMessages(null)
        isHolding = false

        etAnswer.setText("")
        etAnswer.visibility = View.GONE

        btnAction.visibility = View.GONE
        btnAction.isEnabled = true
        btnAction.text = ""
        btnAction.setOnClickListener(null)
        btnAction.setOnTouchListener(null)

        btnUnlock.visibility = View.GONE
        btnUnlock.isEnabled = false
        btnUnlock.text = ""
        btnUnlock.setOnClickListener(null)
    }

    private fun startBreathingChallenge() {
        currentPhaseIndex = 0

        if (challengeMode == MODE_MANUAL) {
            tvChallengeTitle.text = "Focus Reset"
            tvChallengeSubtitle.text = "Complete your selected focus challenge, then return when you're ready."
        } else {
            tvChallengeTitle.text = "Pause & Reflect"
            tvChallengeSubtitle.text = "Complete the challenge before you continue."
        }

        runCurrentPhase()
    }

    private fun runCurrentPhase() {
        if (currentPhaseIndex >= phaseSequence.size) {
            finishChallengeSuccess()
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

    private fun startRapidTapChallenge() {
        tapTarget = 20
        tapCount = 0

        tvChallengeTitle.text = if (challengeMode == MODE_MANUAL) "Focus Reset" else "Quick Action"
        tvChallengeSubtitle.text = "Tap the button $tapTarget times to continue."
        tvPhase.text = "Tap Fast"
        tvTimer.text = tapCount.toString()
        tvProgress.text = "$tapCount / $tapTarget taps"

        btnAction.visibility = View.VISIBLE
        btnAction.text = "Tap Me"
        btnAction.setOnClickListener {
            tapCount++
            tvTimer.text = tapCount.toString()
            tvProgress.text = "$tapCount / $tapTarget taps"

            if (tapCount >= tapTarget) {
                finishChallengeSuccess()
            }
        }
    }

    private fun startHoldChallenge() {
        holdRequiredMs = 4000L

        tvChallengeTitle.text = if (challengeMode == MODE_MANUAL) "Focus Reset" else "Hold Your Focus"
        tvChallengeSubtitle.text = "Press and hold the button for 4 seconds without letting go."
        tvPhase.text = "Press & Hold"
        tvTimer.text = "4"
        tvProgress.text = "Hold steadily"

        btnAction.visibility = View.VISIBLE
        btnAction.text = "Hold Me"

        btnAction.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!isHolding) {
                        isHolding = true
                        holdStartTime = System.currentTimeMillis()
                        holdHandler.removeCallbacksAndMessages(null)
                        holdHandler.post(holdRunnable)
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isHolding) {
                        isHolding = false
                        holdHandler.removeCallbacksAndMessages(null)
                        tvTimer.text = "4"
                        tvProgress.text = "Released too early — try again"
                    }
                    true
                }

                else -> true
            }
        }
    }

    private fun startMathChallenge() {
        val useAddition = Random.nextBoolean()
        val a: Int
        val b: Int
        val symbol: String

        if (useAddition) {
            a = Random.nextInt(2, 10)
            b = Random.nextInt(2, 10)
            mathAnswer = a + b
            symbol = "+"
        } else {
            a = Random.nextInt(6, 15)
            b = Random.nextInt(1, a)
            mathAnswer = a - b
            symbol = "-"
        }

        tvChallengeTitle.text = if (challengeMode == MODE_MANUAL) "Focus Reset" else "Quick Check"
        tvChallengeSubtitle.text = "Solve the math problem to continue."
        tvPhase.text = "$a $symbol $b = ?"
        tvTimer.text = "?"
        tvProgress.text = "Enter your answer"

        etAnswer.visibility = View.VISIBLE
        btnAction.visibility = View.VISIBLE
        btnAction.text = "Submit"

        btnAction.setOnClickListener {
            val typed = etAnswer.text.toString().trim()

            if (typed.isBlank()) {
                Toast.makeText(this, "Enter an answer first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userAnswer = typed.toIntOrNull()
            if (userAnswer == null) {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userAnswer == mathAnswer) {
                finishChallengeSuccess()
            } else {
                etAnswer.setText("")
                tvProgress.text = "Wrong answer — try again"
                Toast.makeText(this, "Wrong answer", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun finishChallengeSuccess() {
        countDownTimer?.cancel()
        holdHandler.removeCallbacksAndMessages(null)
        isHolding = false

        etAnswer.visibility = View.GONE
        btnAction.visibility = View.GONE

        tvPhase.text = "Ready"
        tvTimer.text = "✓"
        tvProgress.text = "Challenge complete"

        if (challengeMode == MODE_MANUAL) {
            tvChallengeSubtitle.text = "Nice. Return to ClearSpace when you're ready."
            btnUnlock.text = "Back to ClearSpace"
            btnUnlock.setOnClickListener {
                reportChallengeCompletionThen {
                    finishManualFocus()
                }
            }
        } else {
            tvChallengeSubtitle.text = "Nice. You can return to your app now."
            btnUnlock.text = "Unlock App"
            btnUnlock.setOnClickListener {
                reportChallengeCompletionThen {
                    unlockAndReturnToTargetApp()
                }
            }
        }

        btnUnlock.visibility = View.VISIBLE
        btnUnlock.isEnabled = true
    }

    private fun reportChallengeCompletionThen(onDone: () -> Unit) {
        if (hasReportedCompletion) {
            onDone()
            return
        }

        val userId = stateManager.getLoggedInUserId()
        if (userId <= 0) {
            onDone()
            return
        }

        btnUnlock.isEnabled = false

        lifecycleScope.launch {
            try {
                RetrofitClient.api.completeChallenge(
                    CompleteChallengeRequest(
                        user_id = userId,
                        challenge_id = getChallengeIdForCurrentType(),
                        result = "completed"
                    )
                )
                hasReportedCompletion = true
            } catch (e: Exception) {
                Toast.makeText(
                    this@ChallengeActivity,
                    "Challenge completed, but reward sync failed",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                onDone()
            }
        }
    }

    private fun getChallengeIdForCurrentType(): Int {
        return when (currentChallengeType) {
            ChallengeType.BREATHING -> 1
            ChallengeType.RAPID_TAP -> 2
            ChallengeType.HOLD_STILL -> 3
            ChallengeType.SIMPLE_MATH -> 4
        }
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
        holdHandler.removeCallbacksAndMessages(null)
        isHolding = false
        isVisible = false
        super.onDestroy()
    }
}