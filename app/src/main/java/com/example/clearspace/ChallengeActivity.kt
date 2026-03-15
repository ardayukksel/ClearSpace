package com.example.clearspace

import android.content.Context
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

    private var isUnlocking = false
    private val relaunchHandler = Handler(Looper.getMainLooper())
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Back is intentionally blocked while challenge is active
            }
        })

        val sharedPref = getSharedPreferences(AppMonitorService.PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit()
            .putBoolean(AppMonitorService.KEY_CHALLENGE_ACTIVE, true)
            .apply()

        stopService(Intent(this, OverlayService::class.java))

        val tvTimer = findViewById<TextView>(R.id.tv_timer)
        val btnUnlock = findViewById<Button>(R.id.btn_unlock)

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

        btnUnlock.setOnClickListener {
            unlockAndReturnToTargetApp()
        }
    }

    override fun onResume() {
        super.onResume()
        stopService(Intent(this, OverlayService::class.java))
    }

    override fun onPause() {
        super.onPause()
        enforceChallengeIfNeeded()
    }

    override fun onStop() {
        super.onStop()
        enforceChallengeIfNeeded()
    }

    private fun enforceChallengeIfNeeded() {
        if (isUnlocking) return

        val sharedPref = getSharedPreferences(AppMonitorService.PREFS_NAME, Context.MODE_PRIVATE)
        val isLocked = sharedPref.getBoolean(AppMonitorService.KEY_IS_LOCKED, false)
        val isChallengeActive = sharedPref.getBoolean(AppMonitorService.KEY_CHALLENGE_ACTIVE, false)

        if (isLocked && isChallengeActive) {
            relaunchHandler.removeCallbacksAndMessages(null)
            relaunchHandler.postDelayed({
                val intent = Intent(this, ChallengeActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                }
                startActivity(intent)
            }, 250)
        }
    }

    private fun unlockAndReturnToTargetApp() {
        val sharedPref = getSharedPreferences(AppMonitorService.PREFS_NAME, Context.MODE_PRIVATE)
        val targetPackage = sharedPref.getString(AppMonitorService.KEY_TARGET_APP_PACKAGE, "") ?: ""

        isUnlocking = true

        val success = sharedPref.edit()
            .putBoolean(AppMonitorService.KEY_IS_LOCKED, false)
            .putBoolean(AppMonitorService.KEY_CHALLENGE_ACTIVE, false)
            .putBoolean(AppMonitorService.KEY_TARGET_ENABLED, false)
            .commit()

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
            return
        }

        relaunchHandler.postDelayed({
            val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            } else {
                Toast.makeText(this, "Could not reopen target app.", Toast.LENGTH_SHORT).show()
            }

            finish()
        }, 300)
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        relaunchHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}