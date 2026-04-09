package com.example.clearspace

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.clearspace.utils.PermissionUtils

class MainActivity : AppCompatActivity() {

    private lateinit var switchTarget: Switch
    private lateinit var tvSessionLimit: TextView
    private lateinit var tvSelectedApp: TextView
    private lateinit var tvMonitoringStatus: TextView
    private lateinit var btnChooseApp: Button
    private lateinit var btnSave: Button
    private lateinit var btnSetup: Button
    private lateinit var btnDashboard: Button
    private lateinit var seekbarSession: SeekBar

    private lateinit var stateManager: ClearSpaceStateManager

    private var selectedTargetPackage: String? = null
    private var selectedTargetName: String? = null

    companion object {
        private const val REQUEST_PICK_APP = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        stateManager = ClearSpaceStateManager(this)

        switchTarget = findViewById(R.id.switch_target_app)
        tvSessionLimit = findViewById(R.id.tv_session_limit)
        tvSelectedApp = findViewById(R.id.tv_selected_app)
        tvMonitoringStatus = findViewById(R.id.tv_monitoring_status)
        btnChooseApp = findViewById(R.id.btn_choose_app)
        btnSave = findViewById(R.id.btn_save)
        btnSetup = findViewById(R.id.btn_setup)
        btnDashboard = findViewById(R.id.btn_dashboard)
        seekbarSession = findViewById(R.id.seekbar_session)

        restoreSavedState()

        switchTarget.setOnCheckedChangeListener { _, isChecked ->
            updateUIState(isChecked)
        }

        seekbarSession.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val displayTime = progress.coerceAtLeast(1)
                tvSessionLimit.text = "Session Limit: $displayTime min"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        btnChooseApp.setOnClickListener {
            val intent = Intent(this, AppPickerActivity::class.java)
            startActivityForResult(intent, REQUEST_PICK_APP)
        }

        btnSetup.setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }

        btnDashboard.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        btnSave.setOnClickListener {
            val monitoringEnabled = switchTarget.isChecked
            val selectedTime = seekbarSession.progress.coerceAtLeast(1)

            if (!monitoringEnabled) {
                stopMonitoringFlow()
                return@setOnClickListener
            }

            if (selectedTargetPackage.isNullOrBlank() || selectedTargetName.isNullOrBlank()) {
                Toast.makeText(this, "Please choose an app to block.", Toast.LENGTH_SHORT).show()
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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_PICK_APP && resultCode == Activity.RESULT_OK) {
            val appName = data?.getStringExtra(AppPickerActivity.EXTRA_APP_NAME)
            val packageName = data?.getStringExtra(AppPickerActivity.EXTRA_APP_PACKAGE)

            if (!appName.isNullOrBlank() && !packageName.isNullOrBlank()) {
                selectedTargetName = appName
                selectedTargetPackage = packageName
                refreshSelectedAppUI()
            }
        }
    }

    private fun restoreSavedState() {
        val isEnabled = stateManager.isMonitoringEnabled()
        val savedTimeLimit = stateManager.getTimeLimitMinutes().coerceAtLeast(1)
        val savedTargetPackage = stateManager.getTargetAppPackage()
        val savedTargetName = stateManager.getTargetAppName()

        selectedTargetPackage = if (savedTargetPackage.isBlank()) null else savedTargetPackage
        selectedTargetName = if (savedTargetName == "None") null else savedTargetName

        switchTarget.isChecked = isEnabled
        seekbarSession.progress = savedTimeLimit
        tvSessionLimit.text = "Session Limit: $savedTimeLimit min"

        refreshSelectedAppUI()
        updateUIState(isEnabled)
    }

    private fun refreshSelectedAppUI() {
        val appName = selectedTargetName ?: "None"
        tvSelectedApp.text = "Selected App: $appName"
        btnChooseApp.text = if (selectedTargetName.isNullOrBlank()) "Choose App" else "Change App"
    }

    private fun updateUIState(isEnabled: Boolean) {
        seekbarSession.isEnabled = isEnabled
        tvSessionLimit.alpha = if (isEnabled) 1.0f else 0.45f
        tvMonitoringStatus.text = if (isEnabled) "Monitoring Status: ON" else "Monitoring Status: OFF"
        tvMonitoringStatus.alpha = if (isEnabled) 1.0f else 0.7f
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

        updateUIState(true)

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

        updateUIState(false)

        Toast.makeText(this, "Monitoring is OFF", Toast.LENGTH_SHORT).show()
    }
}