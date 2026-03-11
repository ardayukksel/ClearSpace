package com.example.clearspace

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ChallengeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge)

        val tvTimer = findViewById<TextView>(R.id.tv_timer)
        val btnUnlock = findViewById<Button>(R.id.btn_unlock)

        // A countdown timer for 5 seconds (5000ms), ticking every 1 second (1000ms)
        object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Calculate remaining seconds and update the UI
                val secondsLeft = millisUntilFinished / 1000 + 1
                tvTimer.text = secondsLeft.toString()
            }

            override fun onFinish() {
                // When the timer finishes, set text to 0 and show the unlock button
                tvTimer.text = "0"
                btnUnlock.visibility = View.VISIBLE
            }
        }.start()

        // Handle the click event for the unlock button
        btnUnlock.setOnClickListener {
            // Close this activity and return to the previous state
            finish()
        }
    }
}