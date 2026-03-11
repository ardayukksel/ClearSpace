package com.example.clearspace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val rgAge = findViewById<RadioGroup>(R.id.rg_age)
        val btnNext = findViewById<Button>(R.id.btn_next)

        btnNext.setOnClickListener {
            // Get the ID of the selected radio button
            val selectedId = rgAge.checkedRadioButtonId

            if (selectedId == -1) {
                // No age selected
                Toast.makeText(this, "Please select your age group", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Determine which age group was selected
            val ageGroup = when (selectedId) {
                R.id.rb_age_1 -> "13-19"
                R.id.rb_age_2 -> "20-25"
                R.id.rb_age_3 -> "26-30"
                R.id.rb_age_4 -> "30+"
                else -> "Unknown"
            }

            // Save the user's age group for future quizzes!
            val sharedPref = getSharedPreferences("ClearSpacePrefs", Context.MODE_PRIVATE)
            sharedPref.edit().putString("userAgeGroup", ageGroup).apply()

            // Move to the MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // Finish onboarding so user doesn't come back here
            finish()
        }
    }
}