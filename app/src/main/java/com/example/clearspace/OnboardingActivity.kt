package com.example.clearspace

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewFlipper: ViewFlipper
    private lateinit var btnNext: Button
    private lateinit var btnBack: ImageButton
    private lateinit var tvStepIndicator: TextView
    private lateinit var pillsContainer: LinearLayout

    // Step indices: 0=Age, 1=Goals, 2=Tutorial, 3=Challenge
    private var currentStep = 0
    private val totalSteps = 4
    private val visibleSteps = 3 // Only Age, Goals, Tutorial shown in pills

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewFlipper = findViewById(R.id.view_flipper_onboarding)
        btnNext = findViewById(R.id.btn_next)
        btnBack = findViewById(R.id.btn_back)
        tvStepIndicator = findViewById(R.id.tv_step_indicator)
        pillsContainer = findViewById(R.id.pills_container)

        updateUI()

        btnNext.setOnClickListener {
            if (currentStep < totalSteps - 1) {
                currentStep++
                viewFlipper.displayedChild = currentStep
                updateUI()
            } else {
                // Last step (Challenge) - complete onboarding
                completeOnboarding()
            }
        }

        btnBack.setOnClickListener {
            if (currentStep > 0) {
                currentStep--
                viewFlipper.displayedChild = currentStep
                updateUI()
            } else {
                finish()
            }
        }
    }

    private fun updateUI() {
        updateStepPills()
        updateStepIndicator()
        updateButton()
    }

    private fun updateStepPills() {
        // Show/hide pills container (hide on Challenge step)
        pillsContainer.visibility = if (currentStep < visibleSteps) View.VISIBLE else View.GONE

        // Update each pill (only 3 pills: Age, Goals, Tutorial)
        for (i in 0 until visibleSteps) {
            val stepLayoutId = resources.getIdentifier("layout_step${i + 1}", "id", packageName)
            val circleId = resources.getIdentifier("circle_step${i + 1}", "id", packageName)
            val numId = resources.getIdentifier("num_step${i + 1}", "id", packageName)
            val txtId = resources.getIdentifier("txt_step${i + 1}", "id", packageName)
            val checkId = resources.getIdentifier("check_step${i + 1}", "id", packageName)

            val layout = findViewById<LinearLayout>(stepLayoutId)
            val circle = findViewById<FrameLayout>(circleId)
            val numText = findViewById<TextView>(numId)
            val labelText = findViewById<TextView>(txtId)
            val checkImg = findViewById<ImageView>(checkId)

            when {
                i < currentStep -> {
                    // Completed step - show checkmark
                    layout.setBackgroundResource(R.drawable.bg_step_pill_completed)
                    circle.setBackgroundResource(R.drawable.bg_step_number_completed)
                    numText.visibility = View.GONE
                    checkImg.visibility = View.VISIBLE
                    labelText.visibility = View.GONE
                }
                i == currentStep -> {
                    // Current active step - show number and label
                    layout.setBackgroundResource(R.drawable.bg_step_pill_active)
                    circle.setBackgroundResource(R.drawable.bg_step_number_active)
                    numText.visibility = View.VISIBLE
                    numText.setTextColor(ContextCompat.getColor(this, R.color.white))
                    checkImg.visibility = View.GONE
                    labelText.visibility = View.VISIBLE
                    labelText.setTextColor(ContextCompat.getColor(this, R.color.textPrimary))
                }
                else -> {
                    // Future/inactive step - show only number circle
                    layout.setBackgroundResource(R.drawable.bg_step_pill_inactive)
                    circle.setBackgroundResource(R.drawable.bg_step_number_inactive)
                    numText.visibility = View.VISIBLE
                    numText.setTextColor(ContextCompat.getColor(this, R.color.mutedDark))
                    checkImg.visibility = View.GONE
                    labelText.visibility = View.GONE
                }
            }
        }
    }

    private fun updateStepIndicator() {
        // Show "Step X of 3" for first 3 steps, hide on Challenge
        if (currentStep < visibleSteps) {
            tvStepIndicator.visibility = View.VISIBLE
            tvStepIndicator.text = "Step ${currentStep + 1} of $visibleSteps"
        } else {
            // Challenge step - hide step indicator
            tvStepIndicator.visibility = View.GONE
        }
    }

    private fun updateButton() {
        when (currentStep) {
            0 -> {
                // Age step - "Continue >"
                btnNext.text = "Continue"
                btnNext.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_chevron_right, 0)
                btnNext.compoundDrawableTintList = ContextCompat.getColorStateList(this, R.color.white)
            }
            1 -> {
                // Goals step - "Skip for now >"
                btnNext.text = "Skip for now"
                btnNext.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_chevron_right, 0)
                btnNext.compoundDrawableTintList = ContextCompat.getColorStateList(this, R.color.white)
            }
            2 -> {
                // Tutorial step - "Continue >"
                btnNext.text = "Continue"
                btnNext.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_chevron_right, 0)
                btnNext.compoundDrawableTintList = ContextCompat.getColorStateList(this, R.color.white)
            }
            3 -> {
                // Challenge step - "Got it — Let's Start!"
                btnNext.text = "Got it — Let's Start!"
                btnNext.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_white, 0, 0, 0)
                btnNext.compoundDrawableTintList = ContextCompat.getColorStateList(this, R.color.white)
            }
        }
    }

    private fun completeOnboarding() {
        // Save onboarding completed flag
        getSharedPreferences("clearspace_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_completed", true)
            .apply()

        // Navigate to main activity
        // startActivity(Intent(this, MainActivity::class.java))
        // finish()

        Toast.makeText(this, "Onboarding Complete!", Toast.LENGTH_SHORT).show()
    }
}