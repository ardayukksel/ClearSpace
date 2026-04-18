package com.example.clearspace

import android.content.Context
import android.content.SharedPreferences

class ClearSpaceStateManager(context: Context) {

    companion object {
        private const val KEY_CHALLENGE_BREATHING = "challenge_breathing"
        private const val KEY_CHALLENGE_TAP = "challenge_tap"
        private const val KEY_CHALLENGE_HOLD = "challenge_hold"
        private const val KEY_CHALLENGE_MATH = "challenge_math"
        private const val KEY_CHALLENGE_RANDOM = "challenge_random"
        private const val KEY_SELECTED_CHALLENGE = "selected_challenge"

        // Onboarding keys
        private const val KEY_SELECTED_AGE = "selected_age"
        private const val KEY_USER_GOAL = "user_goal"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }

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

    // ==================== User Login ====================

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

    // ==================== Onboarding ====================

    fun saveSelectedAge(age: String) {
        prefs.edit()
            .putString(KEY_SELECTED_AGE, age)
            .apply()
    }

    fun getSelectedAge(): String {
        return prefs.getString(KEY_SELECTED_AGE, "") ?: ""
    }

    fun saveUserGoal(goal: String) {
        prefs.edit()
            .putString(KEY_USER_GOAL, goal)
            .apply()
    }

    fun getUserGoal(): String {
        return prefs.getString(KEY_USER_GOAL, "") ?: ""
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, completed)
            .apply()
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    // ==================== Challenge Preferences (Single Selection) ====================

    /**
     * Save the selected challenge type (single selection only)
     * @param challengeType One of: "breathing", "tap", "hold", "math", "random"
     */
    fun saveSelectedChallenge(challengeType: String) {
        prefs.edit()
            .putString(KEY_SELECTED_CHALLENGE, challengeType)
            .apply()
    }

    /**
     * Get the currently selected challenge type
     * @return The selected challenge type, defaults to "breathing"
     */
    fun getSelectedChallenge(): String {
        return prefs.getString(KEY_SELECTED_CHALLENGE, "breathing") ?: "breathing"
    }

    // Legacy methods for backward compatibility

    fun saveChallengePreferences(
        breathing: Boolean,
        tap: Boolean,
        hold: Boolean,
        math: Boolean,
        random: Boolean
    ) {
        // Convert to single selection - pick the first enabled one
        val selected = when {
            breathing -> "breathing"
            tap -> "tap"
            hold -> "hold"
            math -> "math"
            random -> "random"
            else -> "breathing"
        }
        saveSelectedChallenge(selected)
    }

    fun isBreathingChallengeEnabled(): Boolean {
        return getSelectedChallenge() == "breathing"
    }

    fun isTapChallengeEnabled(): Boolean {
        return getSelectedChallenge() == "tap"
    }

    fun isHoldChallengeEnabled(): Boolean {
        return getSelectedChallenge() == "hold"
    }

    fun isMathChallengeEnabled(): Boolean {
        return getSelectedChallenge() == "math"
    }

    fun isRandomChallengeEnabled(): Boolean {
        return getSelectedChallenge() == "random"
    }
}
