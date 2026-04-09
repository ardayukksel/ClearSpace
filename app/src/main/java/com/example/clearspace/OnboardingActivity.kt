package com.example.clearspace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {

    private var currentStep = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val viewFlipper = findViewById<ViewFlipper>(R.id.view_flipper_onboarding)
        val btnNext = findViewById<Button>(R.id.btn_next)

        val rgAge = findViewById<RadioGroup>(R.id.rg_age)
        val etGoal = findViewById<EditText>(R.id.et_goal)

        val sharedPref = getSharedPreferences("ClearSpacePrefs", Context.MODE_PRIVATE)

        btnNext.setOnClickListener {
            when (currentStep) {
                0 -> {
                    if (rgAge.checkedRadioButtonId == -1) {
                        Toast.makeText(this, "Please select your age.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val selectedAge = when (rgAge.checkedRadioButtonId) {
                        R.id.rb_age_1 -> "13-19"
                        R.id.rb_age_2 -> "20-25"
                        R.id.rb_age_3 -> "26-30"
                        else -> "30+"
                    }

                    sharedPref.edit().putString("userAge", selectedAge).apply()
                    viewFlipper.showNext()
                    currentStep++
                }

                1 -> {
                    val goalText = etGoal.text.toString().trim()
                    if (goalText.isNotBlank()) {
                        sharedPref.edit().putString("userGoal", goalText).apply()
                    }
                    viewFlipper.showNext()
                    btnNext.text = "Get Started"
                    currentStep++
                }

                2 -> {
                    sharedPref.edit().putBoolean("hasCompletedOnboarding", true).apply()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}