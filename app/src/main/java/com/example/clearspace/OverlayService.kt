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

    companion object {
        var isRunning: Boolean = false
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var isOverlayAdded = false
    private var isTransitioningToChallenge = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (shouldSuppressOverlay()) {
            stopSelf()
            return START_NOT_STICKY
        }

        showOverlayIfNeeded()
        return START_STICKY
    }

    private fun shouldSuppressOverlay(): Boolean {
        val prefs = getSharedPreferences(AppMonitorService.PREFS_NAME, Context.MODE_PRIVATE)
        val isChallengeActive = prefs.getBoolean(AppMonitorService.KEY_CHALLENGE_ACTIVE, false)
        val transitionUntil = prefs.getLong(AppMonitorService.KEY_CHALLENGE_TRANSITION_UNTIL, 0L)
        val inTransitionWindow = System.currentTimeMillis() < transitionUntil

        return isChallengeActive || inTransitionWindow || isTransitioningToChallenge
    }

    private fun showOverlayIfNeeded() {
        if (overlayView != null || isOverlayAdded || shouldSuppressOverlay()) return

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
            if (isTransitioningToChallenge) return@setOnClickListener

            isTransitioningToChallenge = true
            btnChallenge.isEnabled = false

            val sharedPref = getSharedPreferences(
                AppMonitorService.PREFS_NAME,
                Context.MODE_PRIVATE
            )

            sharedPref.edit()
                .putBoolean(AppMonitorService.KEY_CHALLENGE_ACTIVE, true)
                .putLong(
                    AppMonitorService.KEY_CHALLENGE_TRANSITION_UNTIL,
                    System.currentTimeMillis() + 1500L
                )
                .commit()

            removeOverlayImmediately()

            val challengeIntent = Intent(this, ChallengeActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
            }
            startActivity(challengeIntent)

            stopSelf()
        }
    }

    private fun removeOverlayImmediately() {
        overlayView?.let { view ->
            view.visibility = View.GONE

            if (isOverlayAdded) {
                try {
                    windowManager.removeViewImmediate(view)
                } catch (_: Exception) {
                }
                isOverlayAdded = false
            }
        }

        overlayView = null
    }

    override fun onDestroy() {
        removeOverlayImmediately()
        isTransitioningToChallenge = false
        isRunning = false
        super.onDestroy()
    }
}