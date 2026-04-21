package com.example.clearspace

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class ChallengePreferencesActivity : AppCompatActivity() {

    private lateinit var stateManager: ClearSpaceStateManager

    private lateinit var btnBack: ImageButton
    private lateinit var btnSave: Button
    private lateinit var tvSummary: TextView

    private lateinit var cardChallengeBreathing: CardView
    private lateinit var cardChallengeTap: CardView
    private lateinit var cardChallengeHold: CardView
    private lateinit var cardChallengeMath: CardView
    private lateinit var cardChallengeRandom: CardView

    private var isBreathingSelected = true
    private var isTapSelected = false
    private var isHoldSelected = false
    private var isMathSelected = false
    private var isRandomSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge_preferences)

        stateManager = ClearSpaceStateManager(this)

        initViews()
        loadSavedValues()
        setupClickListeners()
        updateChallengeCards()
        updateSummary()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btn_back)
        btnSave = findViewById(R.id.btn_save)
        tvSummary = findViewById(R.id.tv_summary)

        cardChallengeBreathing = findViewById(R.id.card_challenge_breathing)
        cardChallengeTap = findViewById(R.id.card_challenge_tap)
        cardChallengeHold = findViewById(R.id.card_challenge_hold)
        cardChallengeMath = findViewById(R.id.card_challenge_math)
        cardChallengeRandom = findViewById(R.id.card_challenge_random)
    }

    private fun loadSavedValues() {
        isBreathingSelected = stateManager.isBreathingChallengeEnabled()
        isTapSelected = stateManager.isTapChallengeEnabled()
        isHoldSelected = stateManager.isHoldChallengeEnabled()
        isMathSelected = stateManager.isMathChallengeEnabled()
        isRandomSelected = stateManager.isRandomChallengeEnabled()

        normalizeChallengeSelection()

        if (!isBreathingSelected && !isTapSelected && !isHoldSelected && !isMathSelected && !isRandomSelected) {
            isBreathingSelected = true
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            normalizeChallengeSelection()

            if (!isBreathingSelected && !isTapSelected && !isHoldSelected && !isMathSelected && !isRandomSelected) {
                isBreathingSelected = true
            }

            stateManager.saveChallengePreferences(
                breathing = isBreathingSelected,
                tap = isTapSelected,
                hold = isHoldSelected,
                math = isMathSelected,
                random = isRandomSelected
            )

            finish()
        }

        cardChallengeBreathing.setOnClickListener {
            isBreathingSelected = !isBreathingSelected
            if (isBreathingSelected) {
                isRandomSelected = false
            }
            normalizeChallengeSelection()
            updateChallengeCards()
            updateSummary()
        }

        cardChallengeTap.setOnClickListener {
            isTapSelected = !isTapSelected
            if (isTapSelected) {
                isRandomSelected = false
            }
            normalizeChallengeSelection()
            updateChallengeCards()
            updateSummary()
        }

        cardChallengeHold.setOnClickListener {
            isHoldSelected = !isHoldSelected
            if (isHoldSelected) {
                isRandomSelected = false
            }
            normalizeChallengeSelection()
            updateChallengeCards()
            updateSummary()
        }

        cardChallengeMath.setOnClickListener {
            isMathSelected = !isMathSelected
            if (isMathSelected) {
                isRandomSelected = false
            }
            normalizeChallengeSelection()
            updateChallengeCards()
            updateSummary()
        }

        cardChallengeRandom.setOnClickListener {
            isRandomSelected = !isRandomSelected
            if (isRandomSelected) {
                isBreathingSelected = false
                isTapSelected = false
                isHoldSelected = false
                isMathSelected = false
            }
            normalizeChallengeSelection()
            updateChallengeCards()
            updateSummary()
        }
    }

    private fun normalizeChallengeSelection() {
        if (isRandomSelected) {
            isBreathingSelected = false
            isTapSelected = false
            isHoldSelected = false
            isMathSelected = false
        }
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

    private fun updateSummary() {
        val selected = mutableListOf<String>()

        if (isBreathingSelected) selected.add("Breathing")
        if (isTapSelected) selected.add("Rapid Tap")
        if (isHoldSelected) selected.add("Hold")
        if (isMathSelected) selected.add("Math")

        tvSummary.text = when {
            isRandomSelected ->
                "Random enabled. ClearSpace will choose one challenge at random."

            selected.isNotEmpty() ->
                "Selected: ${selected.joinToString(", ")}"

            else ->
                "No challenge selected. Breathing will be used by default."
        }
    }
}