package com.example.clearspace

import android.app.ComponentCaller
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
    }

    private var isUnlocking = false
    private var isRelaunchScheduled = false
    private val relaunchHandler = Handler(Looper.getMainLooper())
    private var countDownTimer: CountDownTimer? = null

    private lateinit var stateManager: ClearSpaceStateManager
    private lateinit var tvTimer: TextView
    private lateinit var btnUnlock: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        setContentView(R.layout.activity_challenge)

        stateManager = ClearSpaceStateManager(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Back is intentionally blocked while challenge is active
            }
        })

        stateManager.setChallengeActive(true)
        stopService(Intent(this, OverlayService::class.java))

        tvTimer = findViewById(R.id.tv_timer)
        btnUnlock = findViewById(R.id.btn_unlock)

        btnUnlock.visibility = View.GONE
        startChallengeTimer()

        btnUnlock.setOnClickListener {
            unlockAndReturnToTargetApp()
        }
    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent)
        setIntent(intent)
        overridePendingTransition(0, 0)

        // If challenge is already visible, do not restart the timer.
        // Just keep the user on the same challenge screen.
        relaunchHandler.removeCallbacksAndMessages(null)
        isRelaunchScheduled = false
    }

    override fun onResume() {
        super.onResume()
        isVisible = true
        isRelaunchScheduled = false
        overridePendingTransition(0, 0)
        stopService(Intent(this, OverlayService::class.java))
    }

    override fun onPause() {
        super.onPause()
        // Do not relaunch here as well; onStop is enough and avoids duplicate triggers.
    }

    override fun onStop() {
        super.onStop()
        isVisible = false
        enforceChallengeIfNeeded()
    }

    private fun startChallengeTimer() {
        countDownTimer?.cancel()
        tvTimer.text = "5"
        btnUnlock.visibility = View.GONE

        countDownTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000 + 1
                tvTimer.text = secondsLeft.toString()
            }

            override fun onFinish() {
                tvTimer.text = "0"
                btnUnlock.visibility = View.VISIBLE
            }
        }.start()
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

                val intent = Intent(this, ChallengeActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_NO_ANIMATION
                    )
                }
                startActivity(intent)
                overridePendingTransition(0, 0)
            }, 120)
        }
    }

    private fun unlockAndReturnToTargetApp() {
        val targetPackage = stateManager.getTargetAppPackage()

        isUnlocking = true
        relaunchHandler.removeCallbacksAndMessages(null)
        isRelaunchScheduled = false

        val success = stateManager.clearAfterUnlock()

        if (!success) {
            Toast.makeText(this, "Failed to unlock app state.", Toast.LENGTH_SHORT).show()
            isUnlocking = false
            return
        }

        stopService(Intent(this, OverlayService::class.java))

        val stopIntent = Intent(this, AppMonitorService::class.java).apply {
            action = AppMonitorService.ACTION_STOP_MONITORING
        }
        startService(stopIntent)

        if (targetPackage.isBlank()) {
            Toast.makeText(this, "No target app selected.", Toast.LENGTH_SHORT).show()
            finish()
            overridePendingTransition(0, 0)
            return
        }

        relaunchHandler.postDelayed({
            val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)

            if (launchIntent != null) {
                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
                startActivity(launchIntent)
            } else {
                Toast.makeText(this, "Could not reopen target app.", Toast.LENGTH_SHORT).show()
            }

            finish()
            overridePendingTransition(0, 0)
        }, 80)
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