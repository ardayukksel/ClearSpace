package com.example.clearspace

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var switchTarget: Switch
    private lateinit var tvSessionLimit: TextView
    private lateinit var seekbarSession: SeekBar
    private lateinit var btnSave: Button

    // UI Elements for mock app selection (based on Figma design)
    private lateinit var btnAppInstagram: LinearLayout
    private lateinit var btnAppTiktok: LinearLayout

    private lateinit var stateManager: ClearSpaceStateManager

    // Track selected state for mock apps
    private var isInstagramSelected = false
    private var isTiktokSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize state manager to read/write SharedPreferences
        stateManager = ClearSpaceStateManager(this)

        // Bind UI components
        switchTarget = findViewById(R.id.switch_target_app)
        tvSessionLimit = findViewById(R.id.tv_session_limit)
        seekbarSession = findViewById(R.id.seekbar_session)
        btnSave = findViewById(R.id.btn_save)
        btnAppInstagram = findViewById(R.id.btn_app_instagram)
        btnAppTiktok = findViewById(R.id.btn_app_tiktok)

        // Restore previous settings from state manager
        val isEnabled = stateManager.isMonitoringEnabled()
        val timeLimit = stateManager.getTimeLimitMinutes()

        switchTarget.isChecked = isEnabled
        seekbarSession.progress = timeLimit
        tvSessionLimit.text = "Session Limit: $timeLimit min"
        updateSliderState(isEnabled)

        // App selection mock logic: Toggle Instagram selection
        btnAppInstagram.setOnClickListener {
            isInstagramSelected = !isInstagramSelected
            btnAppInstagram.alpha = if (isInstagramSelected) 1.0f else 0.4f
        }

        // App selection mock logic: Toggle TikTok selection
        btnAppTiktok.setOnClickListener {
            isTiktokSelected = !isTiktokSelected
            btnAppTiktok.alpha = if (isTiktokSelected) 1.0f else 0.4f
        }

        // Toggle switch listener to enable/disable UI elements
        switchTarget.setOnCheckedChangeListener { _, isChecked ->
            updateSliderState(isChecked)
        }

        // SeekBar listener to dynamically update the time limit text
        seekbarSession.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val displayTime = progress.coerceAtLeast(1) // Enforce minimum 1 minute
                tvSessionLimit.text = "Session Limit: $displayTime min"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Save settings and restart monitoring service when button is clicked
        btnSave.setOnClickListener {
            val selectedTime = seekbarSession.progress.coerceAtLeast(1)
            val isTargetSelected = switchTarget.isChecked

            startMonitoringWithCurrentSettings(selectedTime, isTargetSelected)
        }
    }

    // Updates the visual and interactive state of the slider based on the toggle
    private fun updateSliderState(isEnabled: Boolean) {
        seekbarSession.isEnabled = isEnabled
        tvSessionLimit.alpha = if (isEnabled) 1.0f else 0.4f
    }

    // Starts or stops the AppMonitorService based on saved user preferences
    private fun startMonitoringWithCurrentSettings(selectedTime: Int, isTargetSelected: Boolean) {
        // Save settings securely
        stateManager.saveMonitoringSettings(isTargetSelected, selectedTime)
        stateManager.resetLockState()

        // Stop any currently running overlay to prevent glitches
        stopService(Intent(this, OverlayService::class.java))

        val monitorIntent = Intent(this, AppMonitorService::class.java)

        if (isTargetSelected) {
            val targetPackages = mutableSetOf<String>()
            if (isInstagramSelected) {
                targetPackages.add("com.instagram.android")
            }
            if (isTiktokSelected) {
                targetPackages.add("com.zhiliaoapp.musically")
            }

            stateManager.saveTargetAppPackages(targetPackages)

            monitorIntent.action = AppMonitorService.ACTION_START_MONITORING
            Toast.makeText(this, "Monitoring started. Limit: $selectedTime min", Toast.LENGTH_SHORT).show()

            // Handle Foreground Service start for Android O and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(monitorIntent)
            } else {
                startService(monitorIntent)
            }
        } else {
            // Stop monitoring completely if switch is turned off
            monitorIntent.action = AppMonitorService.ACTION_STOP_MONITORING
            startService(monitorIntent)
            Toast.makeText(this, "Monitoring is OFF", Toast.LENGTH_SHORT).show()
        }
    }
}