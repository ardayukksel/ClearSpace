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

    private lateinit var tvSelectedApp: TextView
    private lateinit var btnSelectApp: Button
    private lateinit var switchTarget: Switch
    private lateinit var tvSessionLimit: TextView
    private lateinit var seekbarSession: SeekBar
    private lateinit var btnSave: Button

    private lateinit var sharedPref: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        sharedPref = getSharedPreferences(AppMonitorService.PREFS_NAME, Context.MODE_PRIVATE)

        tvSelectedApp = findViewById(R.id.tv_selected_app)
        btnSelectApp = findViewById(R.id.btn_select_app)
        switchTarget = findViewById(R.id.switch_target_app)
        tvSessionLimit = findViewById(R.id.tv_session_limit)
        seekbarSession = findViewById(R.id.seekbar_session)
        btnSave = findViewById(R.id.btn_save)

        val savedSwitchState = sharedPref.getBoolean(AppMonitorService.KEY_TARGET_ENABLED, false)
        val savedTimeLimit = sharedPref.getInt(AppMonitorService.KEY_TIME_LIMIT, 10).coerceAtLeast(1)
        val savedAppName = sharedPref.getString(AppMonitorService.KEY_TARGET_APP_NAME, "None")

        switchTarget.isChecked = savedSwitchState
        seekbarSession.progress = savedTimeLimit
        tvSessionLimit.text = "Session Limit: $savedTimeLimit min"
        tvSelectedApp.text = "Target App: $savedAppName"

        updateSliderState(savedSwitchState)

        if (!PermissionUtils.hasUsageStatsPermission(this)) {
            PermissionUtils.requestUsageStatsPermission(this)
        } else if (!PermissionUtils.hasOverlayPermission(this)) {
            PermissionUtils.requestOverlayPermission(this)
        }

        btnSelectApp.setOnClickListener {
            showAppPicker()
        }

        switchTarget.setOnCheckedChangeListener { _, isChecked ->
            updateSliderState(isChecked)
        }

        seekbarSession.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val safeProgress = progress.coerceAtLeast(1)
                tvSessionLimit.text = "Session Limit: $safeProgress min"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        btnSave.setOnClickListener {
            saveAndApplyMonitoring()
        }
    }

    override fun onResume() {
        super.onResume()

        val savedAppName = sharedPref.getString(AppMonitorService.KEY_TARGET_APP_NAME, "None")
        val savedTimeLimit = sharedPref.getInt(AppMonitorService.KEY_TIME_LIMIT, 10).coerceAtLeast(1)

        tvSelectedApp.text = "Target App: $savedAppName"
        tvSessionLimit.text = "Session Limit: $savedTimeLimit min"
        seekbarSession.progress = savedTimeLimit
    }

    private fun showAppPicker() {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)

        val appEntries = resolveInfos
            .mapNotNull { info ->
                val appName = info.loadLabel(pm)?.toString() ?: return@mapNotNull null
                val packageName = info.activityInfo.packageName

                if (packageName == packageNameFromSelf()) {
                    return@mapNotNull null
                }

                appName to packageName
            }
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }

        val appNames = appEntries.map { it.first }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select App to Block")
            .setItems(appNames) { _, which ->
                val selectedName = appEntries[which].first
                val selectedPackage = appEntries[which].second

                sharedPref.edit()
                    .putString(AppMonitorService.KEY_TARGET_APP_NAME, selectedName)
                    .putString(AppMonitorService.KEY_TARGET_APP_PACKAGE, selectedPackage)
                    .apply()

                tvSelectedApp.text = "Target App: $selectedName"
                Toast.makeText(this, "Selected: $selectedName", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun saveAndApplyMonitoring() {
        val isTargetSelected = switchTarget.isChecked
        val selectedTime = seekbarSession.progress.coerceAtLeast(1)
        val selectedAppPackage = sharedPref.getString(AppMonitorService.KEY_TARGET_APP_PACKAGE, "")

        if (isTargetSelected && selectedAppPackage.isNullOrBlank()) {
            Toast.makeText(this, "Please select a target app first.", Toast.LENGTH_SHORT).show()
            return
        }

        if (isTargetSelected && !PermissionUtils.hasUsageStatsPermission(this)) {
            Toast.makeText(this, "Please grant Usage Access permission first.", Toast.LENGTH_SHORT).show()
            PermissionUtils.requestUsageStatsPermission(this)
            return
        }

        if (isTargetSelected && !PermissionUtils.hasOverlayPermission(this)) {
            Toast.makeText(this, "Please grant Overlay permission first.", Toast.LENGTH_SHORT).show()
            PermissionUtils.requestOverlayPermission(this)
            return
        }

        with(sharedPref.edit()) {
            putBoolean(AppMonitorService.KEY_TARGET_ENABLED, isTargetSelected)
            putInt(AppMonitorService.KEY_TIME_LIMIT, selectedTime)
            apply()
        }

        val monitorIntent = Intent(this, AppMonitorService::class.java)

        if (isTargetSelected) {
            monitorIntent.action = AppMonitorService.ACTION_START_MONITORING

            Toast.makeText(
                this,
                "Monitoring started. Limit: $selectedTime min",
                Toast.LENGTH_SHORT
            ).show()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(monitorIntent)
            } else {
                startService(monitorIntent)
            }
        } else {
            monitorIntent.action = AppMonitorService.ACTION_STOP_MONITORING
            startService(monitorIntent)

            Toast.makeText(this, "Monitoring is OFF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSliderState(isEnabled: Boolean) {
        seekbarSession.isEnabled = isEnabled
        tvSessionLimit.alpha = if (isEnabled) 1.0f else 0.4f
    }

    private fun packageNameFromSelf(): String = packageName
}