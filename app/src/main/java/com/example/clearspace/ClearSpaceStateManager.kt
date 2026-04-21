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

        private const val KEY_SELECTED_AGE = "selected_age"
        private const val KEY_USER_GOAL = "user_goal"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

        private const val KEY_LOGGED_IN_USER_ID = "logged_in_user_id"
        private const val KEY_LOGGED_IN_EMAIL = "logged_in_email"
        private const val KEY_LOGGED_IN_USER_NAME = "logged_in_user_name"

        private const val KEY_USER_NAME_LEGACY = "userName"
        private const val KEY_USER_EMAIL_LEGACY = "userEmail"

        private const val KEY_SESSION_ACCUMULATED_MS = "session_accumulated_ms"
        private const val KEY_SESSION_LAST_RESUME_AT = "session_last_resume_at"
        private const val KEY_SESSION_TARGET_ACTIVE = "session_target_active"
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
        pauseLiveSessionCountdown()
        return prefs.edit()
            .putBoolean(AppMonitorService.KEY_IS_LOCKED, false)
            .putBoolean(AppMonitorService.KEY_CHALLENGE_ACTIVE, false)
            .putBoolean(AppMonitorService.KEY_TARGET_ENABLED, false)
            .commit()
    }

    fun saveLoggedInUser(userId: Int, email: String, userName: String) {
        prefs.edit()
            .putInt(KEY_LOGGED_IN_USER_ID, userId)
            .putString(KEY_LOGGED_IN_EMAIL, email)
            .putString(KEY_LOGGED_IN_USER_NAME, userName)
            .putString(KEY_USER_NAME_LEGACY, userName)
            .putString(KEY_USER_EMAIL_LEGACY, email)
            .apply()
    }

    fun getLoggedInUserId(): Int {
        return prefs.getInt(KEY_LOGGED_IN_USER_ID, -1)
    }

    fun getLoggedInEmail(): String {
        return prefs.getString(KEY_LOGGED_IN_EMAIL, "") ?: ""
    }

    fun getLoggedInUserName(): String {
        return prefs.getString(KEY_LOGGED_IN_USER_NAME, "User") ?: "User"
    }

    fun logoutUser() {
        prefs.edit()
            .remove(KEY_LOGGED_IN_USER_ID)
            .remove(KEY_LOGGED_IN_EMAIL)
            .remove(KEY_LOGGED_IN_USER_NAME)
            .remove(KEY_USER_NAME_LEGACY)
            .remove(KEY_USER_EMAIL_LEGACY)
            .apply()
    }

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

    fun saveSelectedChallenge(challengeType: String) {
        prefs.edit()
            .putString(KEY_SELECTED_CHALLENGE, challengeType)
            .apply()
    }

    fun getSelectedChallenge(): String {
        return prefs.getString(KEY_SELECTED_CHALLENGE, "breathing") ?: "breathing"
    }

    fun saveChallengePreferences(
        breathing: Boolean,
        tap: Boolean,
        hold: Boolean,
        math: Boolean,
        random: Boolean
    ) {
        val finalRandom = random
        val finalBreathing = if (finalRandom) false else breathing
        val finalTap = if (finalRandom) false else tap
        val finalHold = if (finalRandom) false else hold
        val finalMath = if (finalRandom) false else math

        prefs.edit()
            .putBoolean(KEY_CHALLENGE_BREATHING, finalBreathing)
            .putBoolean(KEY_CHALLENGE_TAP, finalTap)
            .putBoolean(KEY_CHALLENGE_HOLD, finalHold)
            .putBoolean(KEY_CHALLENGE_MATH, finalMath)
            .putBoolean(KEY_CHALLENGE_RANDOM, finalRandom)
            .apply()

        val selected = when {
            finalRandom -> "random"
            finalBreathing -> "breathing"
            finalTap -> "tap"
            finalHold -> "hold"
            finalMath -> "math"
            else -> "breathing"
        }

        saveSelectedChallenge(selected)
    }

    fun isBreathingChallengeEnabled(): Boolean {
        return if (isRandomChallengeEnabled()) false else prefs.getBoolean(KEY_CHALLENGE_BREATHING, false)
    }

    fun isTapChallengeEnabled(): Boolean {
        return if (isRandomChallengeEnabled()) false else prefs.getBoolean(KEY_CHALLENGE_TAP, false)
    }

    fun isHoldChallengeEnabled(): Boolean {
        return if (isRandomChallengeEnabled()) false else prefs.getBoolean(KEY_CHALLENGE_HOLD, false)
    }

    fun isMathChallengeEnabled(): Boolean {
        return if (isRandomChallengeEnabled()) false else prefs.getBoolean(KEY_CHALLENGE_MATH, false)
    }

    fun isRandomChallengeEnabled(): Boolean {
        return prefs.getBoolean(KEY_CHALLENGE_RANDOM, false)
    }

    fun resetLiveSessionCountdown() {
        prefs.edit()
            .putLong(KEY_SESSION_ACCUMULATED_MS, 0L)
            .putLong(KEY_SESSION_LAST_RESUME_AT, 0L)
            .putBoolean(KEY_SESSION_TARGET_ACTIVE, false)
            .apply()
    }

    fun resumeLiveSessionCountdown(nowMs: Long = System.currentTimeMillis()) {
        val isAlreadyActive = prefs.getBoolean(KEY_SESSION_TARGET_ACTIVE, false)
        if (isAlreadyActive) return

        prefs.edit()
            .putBoolean(KEY_SESSION_TARGET_ACTIVE, true)
            .putLong(KEY_SESSION_LAST_RESUME_AT, nowMs)
            .apply()
    }

    fun pauseLiveSessionCountdown(nowMs: Long = System.currentTimeMillis()) {
        val isActive = prefs.getBoolean(KEY_SESSION_TARGET_ACTIVE, false)
        if (!isActive) return

        val lastResumeAt = prefs.getLong(KEY_SESSION_LAST_RESUME_AT, 0L)
        val safeElapsed = if (lastResumeAt > 0L) (nowMs - lastResumeAt).coerceAtLeast(0L) else 0L
        val currentAccumulated = prefs.getLong(KEY_SESSION_ACCUMULATED_MS, 0L)

        prefs.edit()
            .putLong(KEY_SESSION_ACCUMULATED_MS, currentAccumulated + safeElapsed)
            .putLong(KEY_SESSION_LAST_RESUME_AT, 0L)
            .putBoolean(KEY_SESSION_TARGET_ACTIVE, false)
            .apply()
    }

    fun isTargetAppSessionActive(): Boolean {
        return prefs.getBoolean(KEY_SESSION_TARGET_ACTIVE, false)
    }

    fun getAccumulatedSessionMs(): Long {
        return prefs.getLong(KEY_SESSION_ACCUMULATED_MS, 0L)
    }

    fun getElapsedSessionMs(nowMs: Long = System.currentTimeMillis()): Long {
        val accumulated = prefs.getLong(KEY_SESSION_ACCUMULATED_MS, 0L)
        val isActive = prefs.getBoolean(KEY_SESSION_TARGET_ACTIVE, false)

        if (!isActive) return accumulated

        val lastResumeAt = prefs.getLong(KEY_SESSION_LAST_RESUME_AT, 0L)
        val liveElapsed = if (lastResumeAt > 0L) (nowMs - lastResumeAt).coerceAtLeast(0L) else 0L
        return accumulated + liveElapsed
    }

    fun getRemainingSessionMs(nowMs: Long = System.currentTimeMillis()): Long {
        val limitMs = getTimeLimitMinutes().coerceAtLeast(1) * 60_000L
        return (limitMs - getElapsedSessionMs(nowMs)).coerceAtLeast(0L)
    }
}