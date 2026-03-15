package com.example.clearspace

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class ClearSpaceAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ClearSpaceA11yService"

        @Volatile
        private var latestForegroundPackage: String = ""

        fun getLatestForegroundPackage(): String {
            return latestForegroundPackage
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString()?.trim().orEmpty()
        if (packageName.isBlank()) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                latestForegroundPackage = packageName
                Log.d(TAG, "Foreground package from accessibility: $packageName")
                enforceIfNeeded(packageName)
            }
        }
    }

    private fun enforceIfNeeded(currentPackage: String) {
        val sharedPref = getSharedPreferences(AppMonitorService.PREFS_NAME, Context.MODE_PRIVATE)

        val isEnabled = sharedPref.getBoolean(AppMonitorService.KEY_TARGET_ENABLED, false)
        val isLocked = sharedPref.getBoolean(AppMonitorService.KEY_IS_LOCKED, false)
        val isChallengeActive = sharedPref.getBoolean(AppMonitorService.KEY_CHALLENGE_ACTIVE, false)
        val targetAppPackage = sharedPref.getString(AppMonitorService.KEY_TARGET_APP_PACKAGE, "") ?: ""

        if (!isEnabled || targetAppPackage.isBlank()) return

        val ownPackage = packageName

        if (isLocked && currentPackage == targetAppPackage) {
            Log.d(TAG, "Locked target app detected via accessibility. Launching overlay.")
            val overlayIntent = Intent(this, OverlayService::class.java)
            startService(overlayIntent)
            return
        }

        if (isLocked && isChallengeActive && currentPackage != ownPackage) {
            Log.d(TAG, "Challenge abandoned while locked. Relaunching challenge.")
            val challengeIntent = Intent(this, ChallengeActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
            }
            startActivity(challengeIntent)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted.")
    }
}