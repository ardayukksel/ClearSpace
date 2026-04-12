package com.example.clearspace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class OnboardingActivity : AppCompatActivity() {

    private var currentStep = 0
    private var selectedAge: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val viewFlipper = findViewById<ViewFlipper>(R.id.view_flipper_onboarding)
        val btnContinue = findViewById<Button>(R.id.btn_next)

        // Age selection cards (Check if these IDs exist in onboarding_step_age.xml)
        val cardTeenager = findViewById<CardView>(R.id.card_teenager)
        val cardYoungAdult = findViewById<CardView>(R.id.card_young_adult)
        val cardAdult = findViewById<CardView>(R.id.card_adult)
        val cardMature = findViewById<CardView>(R.id.card_mature)

        // Goal input (Check if this ID exists in onboarding_step_goals.xml)
        val etCommitment = findViewById<EditText>(R.id.et_commitment)

        val sharedPref = getSharedPreferences("ClearSpacePrefs", Context.MODE_PRIVATE)

        // Card click listeners for age selection
        val ageCards = listOfNotNull(cardTeenager, cardYoungAdult, cardAdult, cardMature)
        val ageValues = listOf("13-19", "20-25", "26-30", "30+")

        ageCards.forEachIndexed { index, card ->
            card.setOnClickListener {
                selectedAge = ageValues[index]
                // Reset all cards and highlight selected
                ageCards.forEach { it.alpha = 0.5f }
                card.alpha = 1.0f
            }
        }

        btnContinue.setOnClickListener {
            when (currentStep) {
                0 -> {
                    if (selectedAge == null) {
                        Toast.makeText(this, "Please select your age.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    sharedPref.edit().putString("userAge", selectedAge).apply()
                    viewFlipper.showNext()
                    currentStep++
                }

                1 -> {
                    val goalText = etCommitment?.text?.toString()?.trim()
                    if (!goalText.isNullOrBlank()) {
                        sharedPref.edit().putString("userGoal", goalText).apply()
                    }
                    viewFlipper.showNext()
                    btnContinue.text = "Get Started"
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
