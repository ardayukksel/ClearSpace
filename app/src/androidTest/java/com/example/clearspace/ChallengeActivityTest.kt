package com.example.clearspace

import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.Lifecycle
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChallengeActivityTest {

    private fun waitUntil(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return true
            SystemClock.sleep(100)
        }
        return condition()
    }

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences(AppMonitorService.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putBoolean(AppMonitorService.KEY_IS_LOCKED, true)
            .putBoolean(AppMonitorService.KEY_CHALLENGE_ACTIVE, true)
            .putString(AppMonitorService.KEY_TARGET_APP_PACKAGE, "com.example.someapp")
            .commit()
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences(AppMonitorService.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun frontend_TC_104() {
        Log.i("ChallengeActivityTest", "Starting frontend_TC_104: Challenge completion blocker")
        
        // ==========================================
        // Objective: Ensure the frontend blocks progression until challenge is completed.
        // Test Steps:
        // 1. Trigger interruption (launch ChallengeActivity).
        // 2. Try pressing back/home/continue without answering.
        // ==========================================

        // Expected Result:
        // - User cannot proceed without valid challenge completion.
        // - Appropriate validation message appears if needed.

        Log.i("ChallengeActivityTest", "Step 1: Launching ChallengeActivity (Interruption triggered)")
        val scenario = ActivityScenario.launch(ChallengeActivity::class.java)

        // Verify Challenge screen is shown
        Log.i("ChallengeActivityTest", "Verifying Challenge screen UI elements")
        scenario.onActivity { activity ->
            val title = activity.findViewById<TextView>(R.id.tv_challenge_title)
            assertEquals("Pause & Reflect", title.text.toString())
        }

        // Verify unlock button is hidden
        Log.i("ChallengeActivityTest", "Verifying Unlock button is hidden initially")
        scenario.onActivity { activity ->
            val unlockButton = activity.findViewById<Button>(R.id.btn_unlock)
            assertEquals(View.GONE, unlockButton.visibility)
            assertEquals(false, unlockButton.isEnabled)
        }

        // Step 2: Try pressing back
        Log.i("ChallengeActivityTest", "Step 2: Pressing back button to attempt bypass")
        scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        
        Log.i("ChallengeActivityTest", "Verifying Challenge screen is still shown after back press")
        scenario.onActivity { activity ->
            val title = activity.findViewById<TextView>(R.id.tv_challenge_title)
            assertEquals("Pause & Reflect", title.text.toString())
        }

        // Verify unlock is still hidden
        Log.i("ChallengeActivityTest", "Verifying Unlock button is still hidden after back press bypass attempt")
        scenario.onActivity { activity ->
            val unlockButton = activity.findViewById<Button>(R.id.btn_unlock)
            assertEquals(View.GONE, unlockButton.visibility)
        }
        assertTrue("Activity should still be alive after blocked back press", scenario.state != Lifecycle.State.DESTROYED)

        // Verify unlock becomes available
        Log.i("ChallengeActivityTest", "Verifying Unlock button is now visible")
        val unlockVisible = waitUntil(30_000) {
            var visible = false
            scenario.onActivity { activity ->
                val unlockButton = activity.findViewById<Button>(R.id.btn_unlock)
                visible = unlockButton.visibility == View.VISIBLE && unlockButton.isEnabled
            }
            visible
        }
        assertTrue("Unlock button should become visible after challenge completes", unlockVisible)

        // Step 3: User answers/completes challenge
        Log.i("ChallengeActivityTest", "Step 3: Completing the challenge by clicking Unlock")
        scenario.onActivity { activity ->
            activity.findViewById<Button>(R.id.btn_unlock).performClick()
        }

        // Give the activity time to finish
        Log.i("ChallengeActivityTest", "Waiting for finish/relaunch routing logic")
        SystemClock.sleep(300)

        // Verify state is cleared
        Log.i("ChallengeActivityTest", "Verifying AppMonitor shared preferences state is cleared upon success")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences(AppMonitorService.PREFS_NAME, Context.MODE_PRIVATE)
        assertEquals(false, prefs.getBoolean(AppMonitorService.KEY_IS_LOCKED, false))
        assertEquals(false, prefs.getBoolean(AppMonitorService.KEY_CHALLENGE_ACTIVE, false))

        // Verify challenge screen exits foreground after unlock.
        Log.i("ChallengeActivityTest", "Polling lifecycle state after unlock (up to 3 seconds)")
        val leftForeground = waitUntil(3_000) { scenario.state != Lifecycle.State.RESUMED }
        assertTrue("Activity should no longer be resumed after successful unlock", leftForeground)
        
        Log.i("ChallengeActivityTest", "frontend_TC_104 completed successfully")
    }
}
