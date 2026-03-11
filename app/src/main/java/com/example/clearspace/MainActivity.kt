package com.example.clearspace

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.clearspace.utils.PermissionUtils

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Request necessary permissions
        if (!PermissionUtils.hasUsageStatsPermission(this)) {
            PermissionUtils.requestUsageStatsPermission(this)
        } else if (!PermissionUtils.hasOverlayPermission(this)) {
            PermissionUtils.requestOverlayPermission(this)
        }

        // Find UI elements by ID
        val tvSelectedApp = findViewById<TextView>(R.id.tv_selected_app)
        val btnSelectApp = findViewById<Button>(R.id.btn_select_app)
        val switchTarget = findViewById<Switch>(R.id.switch_target_app)
        val tvSessionLimit = findViewById<TextView>(R.id.tv_session_limit)
        val seekbarSession = findViewById<SeekBar>(R.id.seekbar_session)
        val btnSave = findViewById<Button>(R.id.btn_save)

        // Load saved preferences
        val sharedPref = getSharedPreferences("ClearSpacePrefs", Context.MODE_PRIVATE)
        val savedSwitchState = sharedPref.getBoolean("isTargetEnabled", false)
        val savedTimeLimit = sharedPref.getInt("timeLimit", 10)
        val savedAppName = sharedPref.getString("targetAppName", "None")

        // Initialize UI with saved values
        switchTarget.isChecked = savedSwitchState
        seekbarSession.progress = savedTimeLimit
        tvSessionLimit.text = "Session Limit: $savedTimeLimit min"
        tvSelectedApp.text = "Target App: $savedAppName"

        // --- App Picker Logic ---
        btnSelectApp.setOnClickListener {
            val pm = packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null)
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

            // Get all installed apps that can be launched
            val resolveInfos = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)

            val appNames = mutableListOf<String>()
            val appPackages = mutableListOf<String>()

            // Extract names and package IDs
            for (info in resolveInfos) {
                appNames.add(info.loadLabel(pm).toString())
                appPackages.add(info.activityInfo.packageName)
            }

            // Show apps in a list dialog
            AlertDialog.Builder(this)
                .setTitle("Select App to Block")
                .setItems(appNames.toTypedArray()) { _, which ->
                    val selectedName = appNames[which]
                    val selectedPackage = appPackages[which]

                    // Save the selected app to SharedPreferences
                    sharedPref.edit()
                        .putString("targetAppName", selectedName)
                        .putString("targetAppPackage", selectedPackage)
                        .apply()

                    // Update UI
                    tvSelectedApp.text = "Target App: $selectedName"
                    Toast.makeText(this, "Selected: $selectedName", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
        // -----------------------------

        fun updateSliderState(isEnabled: Boolean) {
            seekbarSession.isEnabled = isEnabled
            tvSessionLimit.alpha = if (isEnabled) 1.0f else 0.4f
        }

        updateSliderState(savedSwitchState)

        switchTarget.setOnCheckedChangeListener { _, isChecked ->
            updateSliderState(isChecked)
        }

        seekbarSession.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvSessionLimit.text = "Session Limit: $progress min"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnSave.setOnClickListener {
            val isTargetSelected = switchTarget.isChecked
            val selectedTime = seekbarSession.progress

            // Save the switch state and time limit
            with(sharedPref.edit()) {
                putBoolean("isTargetEnabled", isTargetSelected)
                putInt("timeLimit", selectedTime)
                apply()
            }

            val monitorIntent = Intent(this, AppMonitorService::class.java)

            // Start or stop the background service based on switch state
            if (isTargetSelected) {
                Toast.makeText(this, "Saved! Limit: $selectedTime min", Toast.LENGTH_SHORT).show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(monitorIntent)
                } else {
                    startService(monitorIntent)
                }
            } else {
                Toast.makeText(this, "Monitoring is OFF", Toast.LENGTH_SHORT).show()
                stopService(monitorIntent)
            }
        }
    }
}