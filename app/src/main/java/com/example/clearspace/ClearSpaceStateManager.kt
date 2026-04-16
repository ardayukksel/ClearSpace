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
        return prefs.getString(AppMonitorService.KEY_TARGET_APP_NAME, "") ?: ""
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

    fun saveLoggedInUser(userId: Int, email: String, userName: String) {
        prefs.edit()
            .putInt("logged_in_user_id", userId)
            .putString("logged_in_email", email)
            .putString("logged_in_user_name", userName)
            .putString("userName", userName)
            .putString("userEmail", email)
            .apply()
    }

    fun getLoggedInUserId(): Int {
        return prefs.getInt("logged_in_user_id", 1)
    }

    fun getLoggedInEmail(): String {
        return prefs.getString("logged_in_email", "") ?: ""
    }

    fun getLoggedInUserName(): String {
        return prefs.getString("logged_in_user_name", "Alex") ?: "Alex"
    }
}