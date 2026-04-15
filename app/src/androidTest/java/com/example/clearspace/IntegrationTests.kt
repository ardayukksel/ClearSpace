package com.example.clearspace

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.clearspace.data.model.RegulatedApp
import com.example.clearspace.manager.AppUsageManager
import com.example.clearspace.service.AuthService
import com.example.clearspace.service.BreachDetectionService
import com.example.clearspace.service.SessionService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntegrationTests {

    private val TAG = "IntegrationTests"

    @Test
    fun test_TC301_CompleteNormalFlow() {
        Log.i(TAG, "Starting TC-301: Complete normal flow from setup to first interruption")

        val authService = AuthService()
        val sessionService = SessionService()
        val appUsageManager = AppUsageManager(sessionService = sessionService)
        val breachDetectionService = BreachDetectionService()

        val user = authService.register("Flow User", "flow@test.com")
        user.sessionLimitMinutes = 10
        user.dailyLimitMinutes = 120

        val app = RegulatedApp(
            packageName = "com.instagram.android",
            displayName = "Instagram",
            isEnabled = true,
            sessionLimitMinutes = 10,
            dailyLimitMinutes = 120
        )

        val session = appUsageManager.beginUsageSession(user, app)
        assertNotNull(session)
        val activeSession = session!!

        // Simulate threshold breach to represent first interruption trigger.
        activeSession.durationSeconds = 10 * 60

        assertTrue(appUsageManager.isSessionOverLimit(activeSession))
        assertTrue(breachDetectionService.hasSessionLimitBeenBreached(activeSession))
        assertEquals(activeSession.sessionId, sessionService.getActiveSessionForUser(user)?.sessionId)

        Log.i(TAG, "Finished TC-301")
    }

    @Test
    fun test_TC302_UserReachesTotalDailyLimit() {
        Log.i(TAG, "Starting TC-302: User reaches total daily limit")

        val sessionService = SessionService()
        val appUsageManager = AppUsageManager(sessionService = sessionService)

        val user = AuthService().register("Daily User", "daily@test.com")
        user.dailyLimitMinutes = 30

        val app = RegulatedApp(
            packageName = "com.tiktok.android",
            displayName = "TikTok",
            isEnabled = true,
            sessionLimitMinutes = 20,
            dailyLimitMinutes = 30
        )

        val firstSession = appUsageManager.beginUsageSession(user, app)
        assertNotNull(firstSession)
        firstSession!!.durationSeconds = 20 * 60

        val secondSession = appUsageManager.beginUsageSession(user, app)
        assertNotNull(secondSession)
        secondSession!!.durationSeconds = 10 * 60

        val sessionsForUser = listOf(firstSession, secondSession)
        assertTrue(appUsageManager.isDailyUsageOverLimit(user, sessionsForUser))

        Log.i(TAG, "Finished TC-302")
    }
}
