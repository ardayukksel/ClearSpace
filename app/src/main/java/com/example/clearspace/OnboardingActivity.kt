package com.example.clearspace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewFlipper: ViewFlipper
    private lateinit var btnContinue: Button
    private lateinit var btnBack: ImageButton
    private lateinit var stepIndicatorText: TextView
    private lateinit var stepAge: TextView
    private lateinit var stepGoals: TextView
    private lateinit var stepChallenges: TextView
    private lateinit var stepTutorial: TextView

    private var currentStep = 0
    private var selectedAge: String? = null

    private lateinit var etCommitment: EditText
    private lateinit var tvCharCount: TextView
    private lateinit var btnInspiration1: View
    private lateinit var btnInspiration2: View
    private lateinit var btnInspiration3: View
    private lateinit var btnSkip: View

    private lateinit var cardChallengeBreathing: CardView
    private lateinit var cardChallengeTap: CardView
    private lateinit var cardChallengeHold: CardView
    private lateinit var cardChallengeMath: CardView
    private lateinit var cardChallengeRandom: CardView

    private lateinit var sharedPref: android.content.SharedPreferences
    private lateinit var stateManager: ClearSpaceStateManager

    private var isBreathingSelected = true
    private var isTapSelected = false
    private var isHoldSelected = false
    private var isMathSelected = false
    private var isRandomSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        sharedPref = getSharedPreferences("ClearSpacePrefs", Context.MODE_PRIVATE)
        stateManager = ClearSpaceStateManager(this)

        initViews()
        setupAgeSelection()
        setupGoalStep()
        setupChallengeStep()
        setupNavigation()
        updateStepUI()
        loadSavedValues()
    }

    private fun initViews() {
        viewFlipper = findViewById(R.id.view_flipper_onboarding)
        btnContinue = findViewById(R.id.btn_next)
        btnBack = findViewById(R.id.btn_back)
        stepIndicatorText = findViewById(R.id.step_indicator_text)
        stepAge = findViewById(R.id.step_age)
        stepGoals = findViewById(R.id.step_goals)
        stepChallenges = findViewById(R.id.step_challenges)
        stepTutorial = findViewById(R.id.step_tutorial)

        etCommitment = findViewById(R.id.et_commitment)
        tvCharCount = findViewById(R.id.tv_char_count)
        btnInspiration1 = findViewById(R.id.btn_inspiration_1)
        btnInspiration2 = findViewById(R.id.btn_inspiration_2)
        btnInspiration3 = findViewById(R.id.btn_inspiration_3)
        btnSkip = findViewById(R.id.btn_skip)

        cardChallengeBreathing = findViewById(R.id.card_challenge_breathing)
        cardChallengeTap = findViewById(R.id.card_challenge_tap)
        cardChallengeHold = findViewById(R.id.card_challenge_hold)
        cardChallengeMath = findViewById(R.id.card_challenge_math)
        cardChallengeRandom = findViewById(R.id.card_challenge_random)
    }

    private fun setupAgeSelection() {
        val cardTeenager = findViewById<CardView>(R.id.card_teenager)
        val cardYoungAdult = findViewById<CardView>(R.id.card_young_adult)
        val cardAdult = findViewById<CardView>(R.id.card_adult)
        val cardMature = findViewById<CardView>(R.id.card_mature)

        val ageCards = listOf(cardTeenager, cardYoungAdult, cardAdult, cardMature)
        val ageValues = listOf("13-19", "20-25", "26-30", "30+")

        ageCards.forEachIndexed { index, card ->
            card.setOnClickListener {
                selectedAge = ageValues[index]
                highlightSelectedAgeCard(ageCards, card)
            }
        }

        selectedAge?.let { savedAge ->
            val selectedIndex = ageValues.indexOf(savedAge)
            if (selectedIndex != -1) {
                highlightSelectedAgeCard(ageCards, ageCards[selectedIndex])
            }
        }
    }

    private fun highlightSelectedAgeCard(cards: List<CardView>, selectedCard: CardView) {
        cards.forEach { it.alpha = 0.55f }
        selectedCard.alpha = 1.0f
    }

    private fun setupGoalStep() {
        tvCharCount.text = "${etCommitment.text?.length ?: 0}/160"

        etCommitment.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val count = s?.length ?: 0
                tvCharCount.text = "$count/160"
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        btnInspiration1.setOnClickListener {
            setCommitmentText("I will be more present with the people around me.")
        }

        btnInspiration2.setOnClickListener {
            setCommitmentText("I will spend less time scrolling and more time doing.")
        }

        btnInspiration3.setOnClickListener {
            setCommitmentText("I will check my phone intentionally, not out of habit.")
        }

        btnSkip.setOnClickListener {
            etCommitment.setText("")
            goToNextStepFromGoals()
        }
    }

    private fun setCommitmentText(text: String) {
        etCommitment.setText(text)
        etCommitment.setSelection(etCommitment.text.length)
    }

    private fun setupChallengeStep() {
        cardChallengeBreathing.setOnClickListener {
            isBreathingSelected = !isBreathingSelected
            updateChallengeCards()
        }

        cardChallengeTap.setOnClickListener {
            isTapSelected = !isTapSelected
            updateChallengeCards()
        }

        cardChallengeHold.setOnClickListener {
            isHoldSelected = !isHoldSelected
            updateChallengeCards()
        }

        cardChallengeMath.setOnClickListener {
            isMathSelected = !isMathSelected
            updateChallengeCards()
        }

        cardChallengeRandom.setOnClickListener {
            isRandomSelected = !isRandomSelected
            updateChallengeCards()
        }

        updateChallengeCards()
    }

    private fun updateChallengeCards() {
        setChallengeCardState(cardChallengeBreathing, isBreathingSelected)
        setChallengeCardState(cardChallengeTap, isTapSelected)
        setChallengeCardState(cardChallengeHold, isHoldSelected)
        setChallengeCardState(cardChallengeMath, isMathSelected)
        setChallengeCardState(cardChallengeRandom, isRandomSelected)
    }

    private fun setChallengeCardState(card: CardView, isSelected: Boolean) {
        card.alpha = if (isSelected) 1.0f else 0.55f
        card.cardElevation = if (isSelected) 8f else 2f
    }

    private fun setupNavigation() {
        btnBack.setOnClickListener {
            when (currentStep) {
                0 -> finish()
                1, 2, 3 -> {
                    viewFlipper.showPrevious()
                    currentStep--
                    updateStepUI()
                }
            }
        }

        btnContinue.setOnClickListener {
            when (currentStep) {
                0 -> handleAgeStep()
                1 -> handleGoalsStep()
                2 -> handleChallengesStep()
                3 -> finishOnboarding()
            }
        }
    }

    private fun handleAgeStep() {
        if (selectedAge == null) {
            Toast.makeText(this, "Please select your age.", Toast.LENGTH_SHORT).show()
            return
        }

        sharedPref.edit().putString("userAge", selectedAge).apply()
        viewFlipper.showNext()
        currentStep = 1
        updateStepUI()
    }

    private fun handleGoalsStep() {
        val goalText = etCommitment.text?.toString()?.trim().orEmpty()

        if (goalText.isNotBlank()) {
            sharedPref.edit().putString("userGoal", goalText).apply()
        } else {
            sharedPref.edit().remove("userGoal").apply()
        }

        viewFlipper.showNext()
        currentStep = 2
        updateStepUI()
    }

    private fun goToNextStepFromGoals() {
        sharedPref.edit().remove("userGoal").apply()
        viewFlipper.showNext()
        currentStep = 2
        updateStepUI()
    }

    private fun handleChallengesStep() {
        if (!isBreathingSelected && !isTapSelected && !isHoldSelected && !isMathSelected && !isRandomSelected) {
            Toast.makeText(this, "Select at least one challenge.", Toast.LENGTH_SHORT).show()
            return
        }

        stateManager.saveChallengePreferences(
            breathing = isBreathingSelected,
            tap = isTapSelected,
            hold = isHoldSelected,
            math = isMathSelected,
            random = isRandomSelected
        )

        viewFlipper.showNext()
        currentStep = 3
        updateStepUI()
    }

    private fun finishOnboarding() {
        sharedPref.edit()
            .putBoolean("hasCompletedOnboarding", true)
            .apply()

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun updateStepUI() {
        when (currentStep) {
            0 -> {
                stepIndicatorText.text = "Step 1 of 4"
                btnContinue.text = "Continue >"

                activateStep(stepAge)
                deactivateStep(stepGoals)
                deactivateStep(stepChallenges)
                deactivateStep(stepTutorial)
            }

            1 -> {
                stepIndicatorText.text = "Step 2 of 4"
                btnContinue.text = "Continue >"

                deactivateStep(stepAge)
                activateStep(stepGoals)
                deactivateStep(stepChallenges)
                deactivateStep(stepTutorial)
            }

            2 -> {
                stepIndicatorText.text = "Step 3 of 4"
                btnContinue.text = "Continue >"

                deactivateStep(stepAge)
                deactivateStep(stepGoals)
                activateStep(stepChallenges)
                deactivateStep(stepTutorial)
            }

            3 -> {
                stepIndicatorText.text = "Step 4 of 4"
                btnContinue.text = "Get Started"

                deactivateStep(stepAge)
                deactivateStep(stepGoals)
                deactivateStep(stepChallenges)
                activateStep(stepTutorial)
            }
        }
    }

    private fun activateStep(textView: TextView) {
        textView.setBackgroundResource(R.drawable.bg_step_active)
        textView.setTextColor(getColor(R.color.textPrimary))
    }

    private fun deactivateStep(textView: TextView) {
        textView.setBackgroundResource(R.drawable.bg_step_inactive)
        textView.setTextColor(getColor(R.color.mutedDark))
    }

    private fun loadSavedValues() {
        selectedAge = sharedPref.getString("userAge", null)

        val savedGoal = sharedPref.getString("userGoal", "") ?: ""
        if (savedGoal.isNotBlank()) {
            etCommitment.setText(savedGoal)
            etCommitment.setSelection(etCommitment.text.length)
        }

        isBreathingSelected = stateManager.isBreathingChallengeEnabled()
        isTapSelected = stateManager.isTapChallengeEnabled()
        isHoldSelected = stateManager.isHoldChallengeEnabled()
        isMathSelected = stateManager.isMathChallengeEnabled()
        isRandomSelected = stateManager.isRandomChallengeEnabled()

        if (!isBreathingSelected && !isTapSelected && !isHoldSelected && !isMathSelected && !isRandomSelected) {
            isBreathingSelected = true
        }

        updateChallengeCards()
    }
}