package com.example.clearspace

import android.content.Context
import android.content.SharedPreferences

class ClearSpaceStateManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(AppMonitorService.PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val KEY_TARGET_APP_PACKAGES = "targetAppPackages" // 다중 앱 차단용 키
        const val KEY_SAVED_CHALLENGE = "savedChallenge" // 챌린지 종류(예: breathing)
    }

    fun isMonitoringEnabled(): Boolean {
        return prefs.getBoolean(AppMonitorService.KEY_TARGET_ENABLED, false)
    }

    fun getTimeLimitMinutes(): Int {
        return prefs.getInt(AppMonitorService.KEY_TIME_LIMIT, 10).coerceAtLeast(1)
    }

    // 변경됨: 단일 String이 아닌 Set<String>으로 여러 앱 패키지명 반환
    fun getTargetAppPackages(): Set<String> {
        return prefs.getStringSet(KEY_TARGET_APP_PACKAGES, emptySet()) ?: emptySet()
    }

    // 변경됨: 여러 앱 패키지명을 저장
    fun saveTargetAppPackages(packages: Set<String>) {
        prefs.edit()
            .putStringSet(KEY_TARGET_APP_PACKAGES, packages)
            .apply()
    }

    // React의 Context (ChallengeScreen) 대응
    fun getSavedChallenge(): String {
        return prefs.getString(KEY_SAVED_CHALLENGE, "breathing") ?: "breathing"
    }

    fun saveChallenge(challengeType: String) {
        prefs.edit().putString(KEY_SAVED_CHALLENGE, challengeType).apply()
    }

    fun isLocked(): Boolean {
        return prefs.getBoolean(AppMonitorService.KEY_IS_LOCKED, false)
    }

    fun isChallengeActive(): Boolean {
        return prefs.getBoolean(AppMonitorService.KEY_CHALLENGE_ACTIVE, false)
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
            .commit()
    }
}