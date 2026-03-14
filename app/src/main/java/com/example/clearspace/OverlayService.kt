package com.example.clearspace

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var isOverlayAdded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (overlayView != null || isOverlayAdded) {
            return
        }

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_view, null)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        windowManager.addView(overlayView, layoutParams)
        isOverlayAdded = true

        val btnChallenge = overlayView?.findViewById<Button>(R.id.btn_continue_challenge)

        btnChallenge?.setOnClickListener {
            val challengeIntent = Intent(this, ChallengeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(challengeIntent)

            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        overlayView?.let { view ->
            if (isOverlayAdded) {
                windowManager.removeView(view)
                isOverlayAdded = false
            }
        }

        overlayView = null
    }
}