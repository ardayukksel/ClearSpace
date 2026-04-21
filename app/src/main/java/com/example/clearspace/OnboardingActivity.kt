package com.example.clearspace

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewFlipper: ViewFlipper
    private lateinit var btnContinue: Button
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

    private lateinit var cardChallengeBreathing: CardView
    private lateinit var cardChallengeTap: CardView
    private lateinit var cardChallengeHold: CardView
    private lateinit var cardChallengeMath: CardView
    private lateinit var cardChallengeRandom: CardView

    private lateinit var sharedPref: android.content.SharedPreferences
    private lateinit var stateManager: ClearSpaceStateManager

    // 🔥 NO DEFAULTS
    private var isBreathingSelected = false
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
        loadSavedValues()
        updateStepUI()
    }

    private fun initViews() {
        viewFlipper = findViewById(R.id.view_flipper_onboarding)
        btnContinue = findViewById(R.id.btn_next)
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

        cardChallengeBreathing = findViewById(R.id.card_challenge_breathing)
        cardChallengeTap = findViewById(R.id.card_challenge_tap)
        cardChallengeHold = findViewById(R.id.card_challenge_hold)
        cardChallengeMath = findViewById(R.id.card_challenge_math)
        cardChallengeRandom = findViewById(R.id.card_challenge_random)
    }

    // ================= AGE =================

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

        // 🔥 FIX: NO DEFAULT SELECTION
        if (selectedAge != null) {
            val index = ageValues.indexOf(selectedAge)
            if (index != -1) {
                highlightSelectedAgeCard(ageCards, ageCards[index])
            }
        } else {
            ageCards.forEach {
                it.alpha = 1.0f
                it.cardElevation = 2f
            }
        }
    }

    private fun highlightSelectedAgeCard(cards: List<CardView>, selectedCard: CardView) {
        cards.forEach {
            it.alpha = 0.55f
            it.cardElevation = 2f
        }
        selectedCard.alpha = 1.0f
        selectedCard.cardElevation = 8f
    }

    // ================= GOALS =================

    private fun setupGoalStep() {
        tvCharCount.text = "${etCommitment.text?.length ?: 0}/160"

        etCommitment.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                tvCharCount.text = "${s?.length ?: 0}/160"
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
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
    }

    private fun setCommitmentText(text: String) {
        etCommitment.setText(text)
        etCommitment.setSelection(etCommitment.text.length)
    }

    // ================= CHALLENGES =================

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
    }

    private fun updateChallengeCards() {
        setCard(cardChallengeBreathing, isBreathingSelected)
        setCard(cardChallengeTap, isTapSelected)
        setCard(cardChallengeHold, isHoldSelected)
        setCard(cardChallengeMath, isMathSelected)
        setCard(cardChallengeRandom, isRandomSelected)
    }

    private fun setCard(card: CardView, selected: Boolean) {
        card.alpha = if (selected) 1f else 0.55f
        card.cardElevation = if (selected) 8f else 2f
    }

    // ================= NAVIGATION =================

    private fun setupNavigation() {
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
        if (selectedAge.isNullOrBlank()) {
            Toast.makeText(this, "Please select your age.", Toast.LENGTH_SHORT).show()
            return
        }

        stateManager.saveSelectedAge(selectedAge!!)
        viewFlipper.showNext()
        currentStep = 1
        updateStepUI()
    }

    private fun handleGoalsStep() {
        val text = etCommitment.text.toString().trim()

        if (text.isBlank()) {
            Toast.makeText(this, "Please write your commitment.", Toast.LENGTH_SHORT).show()
            return
        }

        stateManager.saveUserGoal(text)
        viewFlipper.showNext()
        currentStep = 2
        updateStepUI()
    }

    private fun handleChallengesStep() {
        val any =
            isBreathingSelected || isTapSelected || isHoldSelected || isMathSelected || isRandomSelected

        if (!any) {
            Toast.makeText(this, "Select at least one challenge.", Toast.LENGTH_SHORT).show()
            return
        }

        stateManager.saveChallengePreferences(
            isBreathingSelected,
            isTapSelected,
            isHoldSelected,
            isMathSelected,
            isRandomSelected
        )

        viewFlipper.showNext()
        currentStep = 3
        updateStepUI()
    }

    private fun finishOnboarding() {
        stateManager.setOnboardingCompleted(true)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    // ================= UI =================

    private fun updateStepUI() {
        when (currentStep) {
            0 -> stepIndicatorText.text = "Step 1 of 4"
            1 -> stepIndicatorText.text = "Step 2 of 4"
            2 -> stepIndicatorText.text = "Step 3 of 4"
            3 -> stepIndicatorText.text = "Step 4 of 4"
        }
    }

    // ================= LOAD =================

    private fun loadSavedValues() {
        selectedAge = stateManager.getSelectedAge().ifBlank { null }

        val goal = stateManager.getUserGoal()
        if (goal.isNotBlank()) {
            etCommitment.setText(goal)
        }

        // 🔥 NO DEFAULTS
        isBreathingSelected = stateManager.isBreathingChallengeEnabled()
        isTapSelected = stateManager.isTapChallengeEnabled()
        isHoldSelected = stateManager.isHoldChallengeEnabled()
        isMathSelected = stateManager.isMathChallengeEnabled()
        isRandomSelected = stateManager.isRandomChallengeEnabled()

        updateChallengeCards()
    }
}