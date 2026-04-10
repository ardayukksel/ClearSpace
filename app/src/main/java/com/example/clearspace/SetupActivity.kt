package com.example.clearspace

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.clearspace.utils.PermissionUtils

class SetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        val btnUsage = findViewById<Button>(R.id.btn_usage_permission)
        val btnOverlay = findViewById<Button>(R.id.btn_overlay_permission)
        val btnAccessibility = findViewById<Button>(R.id.btn_accessibility_permission)
        val btnDone = findViewById<Button>(R.id.btn_done)

        btnUsage.setOnClickListener {
            PermissionUtils.requestUsageStatsPermission(this)
        }

        btnOverlay.setOnClickListener {
            PermissionUtils.requestOverlayPermission(this)
        }

        btnAccessibility.setOnClickListener {
            PermissionUtils.requestAccessibilityPermission(this)
        }

        btnDone.setOnClickListener {
            if (PermissionUtils.hasUsageStatsPermission(this) && PermissionUtils.hasOverlayPermission(this)) {
                finish()
            } else {
                Toast.makeText(this, "Please grant Usage and Overlay permissions to continue.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateButtonStates()
    }

    private fun updateButtonStates() {
        val btnUsage = findViewById<Button>(R.id.btn_usage_permission)
        val btnOverlay = findViewById<Button>(R.id.btn_overlay_permission)
        val btnAccessibility = findViewById<Button>(R.id.btn_accessibility_permission)

        if (PermissionUtils.hasUsageStatsPermission(this)) {
            btnUsage.text = "Usage Permission: GRANTED"
            btnUsage.isEnabled = false
        } else {
            btnUsage.text = "Grant Usage Permission"
            btnUsage.isEnabled = true
        }

        if (PermissionUtils.hasOverlayPermission(this)) {
            btnOverlay.text = "Overlay Permission: GRANTED"
            btnOverlay.isEnabled = false
        } else {
            btnOverlay.text = "Grant Overlay Permission"
            btnOverlay.isEnabled = true
        }

        if (PermissionUtils.hasAccessibilityPermission(this)) {
            btnAccessibility.text = "Accessibility Permission: GRANTED"
            btnAccessibility.isEnabled = false
        } else {
            btnAccessibility.text = "Grant Accessibility Permission"
            btnAccessibility.isEnabled = true
        }
    }
}
