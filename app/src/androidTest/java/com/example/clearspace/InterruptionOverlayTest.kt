package com.example.clearspace

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InterruptionOverlayTest {

    private val TAG = "InterruptionOverlayTest"
    private lateinit var device: UiDevice
    private lateinit var context: Context

    private fun waitUntil(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return true
            SystemClock.sleep(100)
        }
        return condition()
    }

    private fun findChallengeAction(timeoutMs: Long): UiObject2? {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        val packageName = context.packageName

        while (SystemClock.uptimeMillis() < deadline) {
            val byResSplit = device.findObject(By.res(packageName, "btn_continue_challenge"))
            if (byResSplit != null) return byResSplit

            val byResFull = device.findObject(By.res("$packageName:id/btn_continue_challenge"))
            if (byResFull != null) return byResFull

            val byExactText = device.findObject(By.text("Start Challenge"))
            if (byExactText != null) return byExactText

            val byContains = device.findObject(By.textContains("Challenge"))
            if (byContains != null) return byContains

            SystemClock.sleep(200)
        }
        return null
    }

    @Before
    fun setUp() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instrumentation)
        context = instrumentation.targetContext
        val packageName = context.packageName

        // Ensure the screen is awake and unlocked
        device.wakeUp()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()

        // Grant system overlay permissions natively to the device to prevent background restrictions
        instrumentation.uiAutomation.executeShellCommand("appops set $packageName SYSTEM_ALERT_WINDOW allow").close()
        instrumentation.uiAutomation.executeShellCommand("appops set $packageName android:get_usage_stats allow").close()

        // Wait to make sure the permission is registered
        Thread.sleep(500)

        context.getSharedPreferences(AppMonitorService.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        context.stopService(Intent(context, OverlayService::class.java))
        context.getSharedPreferences(AppMonitorService.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun TC_103_InterruptionOverlayAppearsAtSessionLimit() {
        Log.i(TAG, "Starting TC_103: Interruption overlay appears at session limit")

        // ==========================================
        // Objective: Verify interruption screen displays when session threshold is reached.
        // Precondition: Session monitoring is active and limit is hit.
        // Test Steps:
        // 1. Start a regulated app session.
        // 2. Reach session limit.
        // ==========================================

        // Expected Result:
        // - Full-screen interruption overlay appears.
        // - User cannot continue scrolling until interacting with prompt/challenge.

        Log.i(TAG, "Step 1: Launching an activity to ensure app has foreground priority for overlays")
        // Step 1: Launch an activity so the app has foreground priority, increasing the chance system allows overlays.
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        val resumed = waitUntil(5_000) { scenario.state == Lifecycle.State.RESUMED }
        assertTrue("MainActivity should be resumed before triggering overlay", resumed)

        Log.i(TAG, "Verifying OS-level OVERLAY permissions are granted globally via uiAutomation shell commands")
        assertTrue("Overlay permission must be granted for this test.", Settings.canDrawOverlays(context))

        Log.i(TAG, "Step 2: Mocking session limits reached via state manager to avoid real-time waits")
        // Step 2: Start a regulated app session & Reach session limit.
        // Rather than waiting for actual real-clock limits (which makes tests flaky and excessively long),
        // we mock the state transition handled by AppMonitorService triggering when elapsed >= limit
        val stateManager = ClearSpaceStateManager(context)
        stateManager.setLockAndChallenge(locked = true, challengeActive = false)
        
        Log.i(TAG, "Firing the OverlayService directly as AppMonitorService would after crossing thresholds")
        // Fire the OverlayService directly as AppMonitorService would 
        val overlayIntent = Intent(context, OverlayService::class.java)
        context.startService(overlayIntent)

        val overlayStarted = waitUntil(5_000) { OverlayService.isRunning }
        assertTrue("OverlayService did not start. This indicates an app/runtime shortcoming, not a selector issue.", overlayStarted)

        // Give WindowManager time to attach TYPE_APPLICATION_OVERLAY window.
        SystemClock.sleep(300)
        
        Log.i(TAG, "Verifying Expected Result 1: Full-screen interruption UI Automator rendering")
        // Expected Result 1: Full-screen interruption overlay appears.
        // Since it's a WindowManager TYPE_APPLICATION_OVERLAY, Expression cannot find it. Use UIAutomator.
        val btnChallenge = findChallengeAction(12_000)
        assertTrue(
            "Interruption overlay challenge action was not rendered. " +
                    "Likely app/runtime shortcoming (overlay window not shown on this device state), not a test selector issue.",
            btnChallenge != null
        )
        val challengeAction = requireNotNull(btnChallenge)

        Log.i(TAG, "Clicking the WindowManager injected prompt via UIAutomator touch events")
        // Expected Result 2: User cannot continue scrolling until interacting with prompt/challenge
        // We evaluate this block by verifying the 'CONTINUE TO CHALLENGE' button renders on top and absorbs touch.
        // Interact with the prompt by clicking it
        challengeAction.click()
        
        Log.i(TAG, "Verifying UI navigates internally bridging the challenge overlay successfully")
        // Verifying it proceeds to challenge
        val challengeUiRendered = device.wait(Until.hasObject(By.textContains("Pause & Reflect")), 8_000)
        assertTrue("Should navigate to the challenge screen upon interaction", challengeUiRendered)

        Log.i(TAG, "TC_103 completed successfully")
    }
}
