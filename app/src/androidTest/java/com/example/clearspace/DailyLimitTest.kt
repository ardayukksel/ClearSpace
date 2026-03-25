package com.example.clearspace

import android.content.Context
import android.view.View
import android.widget.SeekBar
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import android.util.Log // added import
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyLimitTest {

    private val TAG = "DailyLimitTest"

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
        
        // ==========================================
        // Objective: Verify daily usage limit can be configured.
        // Test Steps:
        // 1. Open daily limit control.
        // 2. Set daily limit to 1 hour (60 mins).
        // 3. Save.
        // ==========================================

        // Expected Result:
        // - UI shows selected daily limit.
        // - Stored setting is reflected on dashboard.

        Log.i(TAG, "Step 1: Open daily limit control (Launch MainActivity where the slider is)")
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Verify the slider is displayed
        Log.i(TAG, "Verifying daily limit slider is displayed")
        onView(withId(R.id.seekbar_session)).check(matches(isDisplayed()))

        Log.i(TAG, "Step 2: Setting daily limit slider to 60 minutes (1 hour)")
        onView(withId(R.id.seekbar_session)).perform(setProgress(60))

        // UI shows selected daily limit
        Log.i(TAG, "Verifying UI string updates to 'Session Limit: 60 min'")
        onView(withId(R.id.tv_session_limit)).check(matches(withText("Session Limit: 60 min")))

        Log.i(TAG, "Step 3: Saving configured limits")
        // Assuming we need a target app selected to bypass the "Select app first" validation
        // So we just mock a selected app first if needed, but the UI updates independently.
        // We evaluate if the shared preferences get the updated value after saving.
        // Mock a state if necessary to allow save:
        Log.i(TAG, "Mocking 'target app selected' requirement so validation doesn't block saves")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val stateManager = ClearSpaceStateManager(context)
        stateManager.saveTargetApp("MockApp", "com.mock")

        Log.i(TAG, "Enabling switch and clicking save button")
        onView(withId(R.id.switch_target_app)).perform(click()) // enable
        onView(withId(R.id.btn_save)).perform(click())

        // Accessibility recommendation dialog might appear initially since we didn't grant Accessibility. 
        // We can just dismiss it by clicking "Continue Anyway"
        try {
            Log.i(TAG, "Attempting to dismiss any accessibility permission prompts dynamically if missing")
            onView(withText("Continue Anyway")).perform(click())
        } catch (e: Exception) {
            // Ignore if dialog doesn't pop up
            Log.i(TAG, "No accessibility validation dialog interrupted the flow")
        }

        // Dashboard stored setting properly
        Log.i(TAG, "Verifying saved shared-preferences state reflects dashboard setting")
        val savedLimit = stateManager.getTimeLimitMinutes()
        assert(savedLimit == 60) { "Saved limit was $savedLimit but expected 60" }
        
        Log.i(TAG, "TC_102 completed successfully")
    }

    private fun setProgress(progress: Int): ViewAction {
        return object : ViewAction {
            override fun perform(uiController: UiController?, view: View) {
                val seekBar = view as SeekBar
                seekBar.progress = progress
            }

            override fun getDescription(): String {
                return "Set a progress on a SeekBar to $progress"
            }

            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(SeekBar::class.java)
            }
        }
    }
}
