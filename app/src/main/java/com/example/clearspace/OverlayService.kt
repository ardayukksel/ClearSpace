package com.example.clearspace

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View

    override fun onBind(intent: Intent?): IBinder? {
        return null // Return null because this service is only used for overlay display
    }

    override fun onCreate() {
        super.onCreate()

        // 1. Get the WindowManager (manager that controls views on top of the screen)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 2. Inflate the overlay_view.xml layout into an actual View object
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_view, null)

        // 3. Required configuration for drawing over other apps
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        // 4. Forcefully add the overlay view to the top of the screen
        windowManager.addView(overlayView, layoutParams)

        // 5. When the button is pressed, open the new ChallengeActivity!
        val btnChallenge = overlayView.findViewById<Button>(R.id.btn_continue_challenge)

        btnChallenge.setOnClickListener {
            // Create an Intent to start the ChallengeActivity
            val challengeIntent = Intent(this, ChallengeActivity::class.java)

            // IMPORTANT: Add this flag to start an Activity from a Service
            challengeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Start the full-screen deep breath challenge
            startActivity(challengeIntent)

            // Stop this overlay service (this will trigger onDestroy and remove the black screen)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // When the service is completely destroyed, cleanly remove the overlay view
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}