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
import com.example.clearspace.utils.PermissionUtils

class MainActivity : AppCompatActivity() {

    private lateinit var switchTarget: Switch
    private lateinit var tvSessionLimit: TextView
    private lateinit var seekbarSession: SeekBar
    private lateinit var btnSave: Button

    private lateinit var btnAppInstagram: LinearLayout
    private lateinit var btnAppTiktok: LinearLayout

    private lateinit var stateManager: ClearSpaceStateManager

    private var selectedTargetPackage: String? = null
    private var selectedTargetName: String? = null

    companion object {
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"
        private const val TIKTOK_PACKAGE = "com.zhiliaoapp.musically"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        stateManager = ClearSpaceStateManager(this)

        switchTarget = findViewById(R.id.switch_target_app)
        tvSessionLimit = findViewById(R.id.tv_session_limit)
        seekbarSession = findViewById(R.id.seekbar_session)
        btnSave = findViewById(R.id.btn_save)
        btnAppInstagram = findViewById(R.id.btn_app_instagram)
        btnAppTiktok = findViewById(R.id.btn_app_tiktok)

        restoreSavedState()

        btnAppInstagram.setOnClickListener {
            selectApp("Instagram", INSTAGRAM_PACKAGE)
        }

        btnAppTiktok.setOnClickListener {
            selectApp("TikTok", TIKTOK_PACKAGE)
        }

        switchTarget.setOnCheckedChangeListener { _, isChecked ->
            updateSliderState(isChecked)
        }

        seekbarSession.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val displayTime = progress.coerceAtLeast(1)
                tvSessionLimit.text = "Session Limit: $displayTime min"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnSave.setOnClickListener {
            val monitoringEnabled = switchTarget.isChecked
            val selectedTime = seekbarSession.progress.coerceAtLeast(1)

            if (!monitoringEnabled) {
                stopMonitoringFlow()
                return@setOnClickListener
            }

            if (selectedTargetPackage.isNullOrBlank() || selectedTargetName.isNullOrBlank()) {
                Toast.makeText(this, "Please select an app to block.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!PermissionUtils.hasUsageStatsPermission(this)) {
                Toast.makeText(this, "Please allow Usage Access for ClearSpace.", Toast.LENGTH_LONG).show()
                PermissionUtils.requestUsageStatsPermission(this)
                return@setOnClickListener
            }

            if (!PermissionUtils.hasOverlayPermission(this)) {
                Toast.makeText(this, "Please allow Display Over Other Apps for ClearSpace.", Toast.LENGTH_LONG).show()
                PermissionUtils.requestOverlayPermission(this)
                return@setOnClickListener
            }

            startMonitoringFlow(selectedTime)
        }
    }

    override fun onResume() {
        super.onResume()
        restoreSavedState()
    }

    private fun restoreSavedState() {
        val isEnabled = stateManager.isMonitoringEnabled()
        val savedTimeLimit = stateManager.getTimeLimitMinutes().coerceAtLeast(1)
        val savedTargetPackage = stateManager.getTargetAppPackage()
        val savedTargetName = stateManager.getTargetAppName()

        switchTarget.isChecked = isEnabled
        seekbarSession.progress = savedTimeLimit
        tvSessionLimit.text = "Session Limit: $savedTimeLimit min"
        updateSliderState(isEnabled)

        when (savedTargetPackage) {
            INSTAGRAM_PACKAGE -> {
                selectedTargetPackage = INSTAGRAM_PACKAGE
                selectedTargetName = if (savedTargetName.isBlank()) "Instagram" else savedTargetName
            }
            TIKTOK_PACKAGE -> {
                selectedTargetPackage = TIKTOK_PACKAGE
                selectedTargetName = if (savedTargetName.isBlank()) "TikTok" else savedTargetName
            }
            else -> {
                selectedTargetPackage = null
                selectedTargetName = null
            }
        }

        refreshAppSelectionUI()
    }

    private fun selectApp(appName: String, packageName: String) {
        selectedTargetName = appName
        selectedTargetPackage = packageName
        refreshAppSelectionUI()
    }

    private fun refreshAppSelectionUI() {
        val instagramSelected = selectedTargetPackage == INSTAGRAM_PACKAGE
        val tiktokSelected = selectedTargetPackage == TIKTOK_PACKAGE

        btnAppInstagram.alpha = if (instagramSelected) 1.0f else 0.45f
        btnAppTiktok.alpha = if (tiktokSelected) 1.0f else 0.45f
    }

    private fun updateSliderState(isEnabled: Boolean) {
        seekbarSession.isEnabled = isEnabled
        tvSessionLimit.alpha = if (isEnabled) 1.0f else 0.4f
    }

    private fun startMonitoringFlow(selectedTime: Int) {
        val appName = selectedTargetName ?: return
        val packageName = selectedTargetPackage ?: return

        stateManager.saveTargetApp(appName, packageName)
        stateManager.saveMonitoringSettings(true, selectedTime)
        stateManager.resetLockState()

        stopService(Intent(this, OverlayService::class.java))

        val monitorIntent = Intent(this, AppMonitorService::class.java).apply {
            action = AppMonitorService.ACTION_START_MONITORING
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(monitorIntent)
        } else {
            startService(monitorIntent)
        }

        Toast.makeText(
            this,
            "Monitoring started for $appName. Limit: $selectedTime min",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun stopMonitoringFlow() {
        stateManager.saveMonitoringSettings(false, seekbarSession.progress.coerceAtLeast(1))
        stateManager.resetLockState()

        stopService(Intent(this, OverlayService::class.java))

        val monitorIntent = Intent(this, AppMonitorService::class.java).apply {
            action = AppMonitorService.ACTION_STOP_MONITORING
        }
        startService(monitorIntent)

        Toast.makeText(this, "Monitoring is OFF", Toast.LENGTH_SHORT).show()
    }
}