package com.example.clearspace

import android.content.Context
import android.content.SharedPreferences

class ClearSpaceStateManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(AppMonitorService.PREFS_NAME, Context.MODE_PRIVATE)

    fun isMonitoringEnabled(): Boolean {
        return prefs.getBoolean(AppMonitorService.KEY_TARGET_ENABLED, false)
    }

    fun getTimeLimitMinutes(): Int {
        return prefs.getInt(AppMonitorService.KEY_TIME_LIMIT, 10).coerceAtLeast(1)
    }

    fun getTargetAppName(): String {
        return prefs.getString(AppMonitorService.KEY_TARGET_APP_NAME, "None") ?: "None"
    }

    fun getTargetAppPackage(): String {
        return prefs.getString(AppMonitorService.KEY_TARGET_APP_PACKAGE, "") ?: ""
    }

    fun isLocked(): Boolean {
        return prefs.getBoolean(AppMonitorService.KEY_IS_LOCKED, false)
    }

    fun isChallengeActive(): Boolean {
        return prefs.getBoolean(AppMonitorService.KEY_CHALLENGE_ACTIVE, false)
    }

    fun saveTargetApp(appName: String, packageName: String) {
        prefs.edit()
            .putString(AppMonitorService.KEY_TARGET_APP_NAME, appName)
            .putString(AppMonitorService.KEY_TARGET_APP_PACKAGE, packageName)
            .apply()
    }

    fun saveMonitoringSettings(enabled: Boolean, timeLimitMinutes: Int) {
        prefs.edit()
            .putBoolean(AppMonitorService.KEY_TARGET_ENABLED, enabled)
            .putInt(AppMonitorService.KEY_TIME_LIMIT, timeLimitMinutes.coerceAtLeast(1))
            .apply()
    }

    fun setLocked(locked: Boolean) {
        prefs.edit()
            .putBoolean(AppMonitorService.KEY_IS_LOCKED, locked)
            .apply()
    }

    fun setChallengeActive(active: Boolean) {
        prefs.edit()
            .putBoolean(AppMonitorService.KEY_CHALLENGE_ACTIVE, active)
            .apply()
    }

    fun setLockAndChallenge(locked: Boolean, challengeActive: Boolean) {
        prefs.edit()
            .putBoolean(AppMonitorService.KEY_IS_LOCKED, locked)
            .putBoolean(AppMonitorService.KEY_CHALLENGE_ACTIVE, challengeActive)
            .apply()
    }

    fun resetLockState() {
        prefs.edit()
            .putBoolean(AppMonitorService.KEY_IS_LOCKED, false)
            .putBoolean(AppMonitorService.KEY_CHALLENGE_ACTIVE, false)
            .apply()
    }

    fun clearAfterUnlock(): Boolean {
        return prefs.edit()
            .putBoolean(AppMonitorService.KEY_IS_LOCKED, false)
            .putBoolean(AppMonitorService.KEY_CHALLENGE_ACTIVE, false)
            .putBoolean(AppMonitorService.KEY_TARGET_ENABLED, false)
            .commit()
    }
}