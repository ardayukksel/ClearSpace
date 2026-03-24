package com.example.clearspace

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.action.ViewActions.click
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChallengeActivityTest {

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
    fun backend_TC_104() {
        //User cannot bypass interruption without completing challenge
        Log.i("ChallengeActivityTest", "Starting backend_TC_104")

        val scenario = ActivityScenario.launch(ChallengeActivity::class.java)

        // Challenge screen is shown
        Log.i("ChallengeActivityTest", "Checking if Challenge screen is shown")
        onView(withText("Pause & Reflect")).check(matches(isDisplayed()))

        // Unlock button should not be available immediately
        Log.i("ChallengeActivityTest", "Checking if Unlock button is hidden initially")
        onView(withId(R.id.btn_unlock))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        // Back press should NOT dismiss the challenge
        Log.i("ChallengeActivityTest", "Pressing back button")
        pressBack()
        Log.i("ChallengeActivityTest", "Checking if Challenge screen is still shown after back press")
        onView(withText("Pause & Reflect")).check(matches(isDisplayed()))

        // Still not unlockable before timer finishes
        Log.i("ChallengeActivityTest", "Checking if Unlock button is still hidden after back press")
        onView(withId(R.id.btn_unlock))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        // Wait for the 5-second challenge timer to finish
        Log.i("ChallengeActivityTest", "Waiting for 5.5 seconds for timer to finish")
        SystemClock.sleep(5500)

        // Now unlock becomes available
        Log.i("ChallengeActivityTest", "Checking if Unlock button is now visible")
        onView(withId(R.id.btn_unlock))
            .check(matches(isDisplayed()))

        // User completes the challenge by pressing unlock
        Log.i("ChallengeActivityTest", "Clicking Unlock button")
        onView(withId(R.id.btn_unlock)).perform(click())

        // Give the finish/relaunch logic a moment to run
        Log.i("ChallengeActivityTest", "Waiting for finish/relaunch logic")
        SystemClock.sleep(300)

        // App state should be cleared after successful unlock
        Log.i("ChallengeActivityTest", "Verifying shared preferences state")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences(AppMonitorService.PREFS_NAME, Context.MODE_PRIVATE)
        assertEquals(false, prefs.getBoolean(AppMonitorService.KEY_IS_LOCKED, false))
        assertEquals(false, prefs.getBoolean(AppMonitorService.KEY_CHALLENGE_ACTIVE, false))

        // Activity should now be closing
        Log.i("ChallengeActivityTest", "Verifying activity is finishing")
        scenario.onActivity { activity ->
            assertEquals(true, activity.isFinishing)
        }
        
        Log.i("ChallengeActivityTest", "backend_TC_104 completed successfully")
    }
}
