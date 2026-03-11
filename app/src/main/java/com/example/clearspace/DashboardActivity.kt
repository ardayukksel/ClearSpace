package com.example.clearspace

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // 1. Start the overlay service to block the screen
        val overlayIntent = Intent(this, OverlayService::class.java)
        startService(overlayIntent)

        // 2. Find the back button from the layout
        val btnBack = findViewById<Button>(R.id.btn_back)

        // 3. Set a click listener to close the dashboard and return to the main screen
        btnBack.setOnClickListener {
            finish()
        }
    }
}