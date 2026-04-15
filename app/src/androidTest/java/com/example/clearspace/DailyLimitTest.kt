package com.example.clearspace

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyLimitTest {

    private val TAG = "DailyLimitTest"

    private fun waitUntil(condition: () -> Boolean): Boolean {
        val deadline = android.os.SystemClock.uptimeMillis() + 5_000L
        while (android.os.SystemClock.uptimeMillis() < deadline) {
            if (condition()) return true
            android.os.SystemClock.sleep(100)
        }
        return condition()
    }

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        
        // Grant special permissions so MainActivity doesn't get pushed into background
        instrumentation.uiAutomation.executeShellCommand("appops set com.example.clearspace android:get_usage_stats allow").close()
        instrumentation.uiAutomation.executeShellCommand("appops set com.example.clearspace SYSTEM_ALERT_WINDOW allow").close()

        val context = instrumentation.targetContext
        context.getSharedPreferences(AppMonitorService.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
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
    fun TC_102_DailyLimitSliderUpdatesCorrectly() {
        Log.i(TAG, "Starting TC_102: Daily limit slider updates correctly")

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val resumed = waitUntil { scenario.state == Lifecycle.State.RESUMED }
        assertTrue("MainActivity should reach RESUMED state", resumed)

        Log.i(TAG, "Step 1: Open session limit control")
        scenario.onActivity { activity ->
            val slider = activity.findViewById<View>(R.id.sliderSessionLimit)
            assertEquals(View.VISIBLE, slider.visibility)
        }

        Log.i(TAG, "Step 2: Set limit to 1 hour using the quick chip")
        scenario.onActivity { activity ->
            activity.findViewById<View>(R.id.chip1hour).performClick()
            assertEquals("1 hour", activity.findViewById<android.widget.TextView>(R.id.tvSessionTime).text.toString())
            assertEquals("Deep work", activity.findViewById<android.widget.TextView>(R.id.tvSessionDescription).text.toString())
        }

        Log.i(TAG, "Step 3: Save settings")
        scenario.onActivity { activity ->
            activity.findViewById<Button>(R.id.btnSaveSettings).performClick()
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val stateManager = ClearSpaceStateManager(context)
        val persisted = waitUntil { stateManager.getTimeLimitMinutes() == 60 }
        assertTrue("Saved time limit should eventually be persisted as 60 minutes", persisted)
        assertEquals(60, stateManager.getTimeLimitMinutes())
        scenario.onActivity { activity ->
            val sessionTime = activity.findViewById<android.widget.TextView>(R.id.tvSessionTime)
            val container = activity.findViewById<LinearLayout>(R.id.emptyAppsContainer)
            assertEquals("1 hour", sessionTime.text.toString())
            assertEquals(View.VISIBLE, container.visibility)
        }
        
        Log.i(TAG, "TC_102 completed successfully")
    }
}
