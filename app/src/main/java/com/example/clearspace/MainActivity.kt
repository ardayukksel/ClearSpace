package com.example.clearspace

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    // Views
    private lateinit var tvGreeting: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvBlockingStatus: TextView
    private lateinit var tvSessionTime: TextView
    private lateinit var tvSessionDescription: TextView
    private lateinit var switchBlocking: SwitchCompat
    private lateinit var sliderSessionLimit: Slider
    private lateinit var chipGroupTime: ChipGroup
    private lateinit var btnAddApp: Button
    private lateinit var btnSaveSettings: Button
    private lateinit var btnNotification: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var emptyAppsContainer: LinearLayout
    private lateinit var rvBlockedApps: RecyclerView

    // Data
    private val blockedApps = mutableListOf<BlockedApp>()
    private var currentSessionMinutes = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupGreeting()
        setupBlockingSwitch()
        setupSlider()
        setupChips()
        setupButtons()
        setupBottomNavigation()
    }

    private fun initViews() {
        tvGreeting = findViewById(R.id.tvGreeting)
        tvUserName = findViewById(R.id.tvUserName)
        tvBlockingStatus = findViewById(R.id.tvBlockingStatus)
        tvSessionTime = findViewById(R.id.tvSessionTime)
        tvSessionDescription = findViewById(R.id.tvSessionDescription)
        switchBlocking = findViewById(R.id.switchBlocking)
        sliderSessionLimit = findViewById(R.id.sliderSessionLimit)
        chipGroupTime = findViewById(R.id.chipGroupTime)
        btnAddApp = findViewById(R.id.btnAddApp)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)
        btnNotification = findViewById(R.id.btnNotification)
        btnSettings = findViewById(R.id.btnSettings)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        emptyAppsContainer = findViewById(R.id.emptyAppsContainer)
        rvBlockedApps = findViewById(R.id.rvBlockedApps)

        // Setup RecyclerView
        rvBlockedApps.layoutManager = LinearLayoutManager(this)
    }

    private fun setupGreeting() {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        val greeting = when {
            hourOfDay < 12 -> "Good morning"
            hourOfDay < 17 -> "Good afternoon"
            else -> "Good evening"
        }

        tvGreeting.text = greeting
        // Username can be fetched from SharedPreferences or user session
        tvUserName.text = "Alex"
    }

    private fun setupBlockingSwitch() {
        switchBlocking.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                tvBlockingStatus.text = "Active & Protected"
            } else {
                tvBlockingStatus.text = "Inactive"
            }
        }
    }

    private fun setupSlider() {
        sliderSessionLimit.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                currentSessionMinutes = value.toInt()
                updateSessionTimeDisplay()
                updateChipSelection()
            }
        }
    }

    private fun setupChips() {
        val chip15min: Chip = findViewById(R.id.chip15min)
        val chip30min: Chip = findViewById(R.id.chip30min)
        val chip1hour: Chip = findViewById(R.id.chip1hour)
        val chip2hours: Chip = findViewById(R.id.chip2hours)
        val chip5hours: Chip = findViewById(R.id.chip5hours)

        chip15min.setOnClickListener { setSessionTime(15) }
        chip30min.setOnClickListener { setSessionTime(30) }
        chip1hour.setOnClickListener { setSessionTime(60) }
        chip2hours.setOnClickListener { setSessionTime(120) }
        chip5hours.setOnClickListener { setSessionTime(300) }

        chipGroupTime.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chipId = checkedIds[0]
                val minutes = when (chipId) {
                    R.id.chip15min -> 15
                    R.id.chip30min -> 30
                    R.id.chip1hour -> 60
                    R.id.chip2hours -> 120
                    R.id.chip5hours -> 300
                    else -> 30
                }
                setSessionTime(minutes)
            }
        }
    }

    private fun setSessionTime(minutes: Int) {
        currentSessionMinutes = minutes
        sliderSessionLimit.value = minutes.toFloat().coerceIn(5f, 600f)
        updateSessionTimeDisplay()
    }

    private fun updateSessionTimeDisplay() {
        tvSessionTime.text = formatTime(currentSessionMinutes)
        tvSessionDescription.text = getSessionDescription(currentSessionMinutes)
    }

    private fun formatTime(minutes: Int): String {
        return when {
            minutes < 60 -> "$minutes min"
            minutes == 60 -> "1 hour"
            minutes % 60 == 0 -> "${minutes / 60} hours"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }

    private fun getSessionDescription(minutes: Int): String {
        return when {
            minutes <= 15 -> "Quick focus"
            minutes <= 30 -> "Balanced focus"
            minutes <= 60 -> "Deep work"
            minutes <= 120 -> "Extended focus"
            else -> "Marathon session"
        }
    }

    private fun updateChipSelection() {
        val chipId = when (currentSessionMinutes) {
            15 -> R.id.chip15min
            30 -> R.id.chip30min
            60 -> R.id.chip1hour
            120 -> R.id.chip2hours
            300 -> R.id.chip5hours
            else -> -1
        }

        if (chipId != -1) {
            chipGroupTime.check(chipId)
        } else {
            chipGroupTime.clearCheck()
        }
    }

    private fun setupButtons() {
        btnAddApp.setOnClickListener {
            // Show app picker dialog
            showAppPickerDialog()
        }

        btnSaveSettings.setOnClickListener {
            saveSettings()
        }

        btnNotification.setOnClickListener {
            // Handle notification click
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show()
        }

        btnSettings.setOnClickListener {
            // Handle settings click
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_home

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on home
                    true
                }
                R.id.nav_dashboard -> {
                    // Navigate to Dashboard
                    Toast.makeText(this, "Dashboard", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_focus -> {
                    // Navigate to Focus
                    Toast.makeText(this, "Focus", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    private fun showAppPickerDialog() {
        // TODO: Implement app picker dialog
        // This would show a list of installed apps for the user to select
        Toast.makeText(this, "Select apps to block", Toast.LENGTH_SHORT).show()
    }

    private fun saveSettings() {
        // Save settings to SharedPreferences or database
        val isBlocking = switchBlocking.isChecked
        val sessionLimit = currentSessionMinutes

        // TODO: Save to persistent storage
        Toast.makeText(
            this,
            "Settings saved! Session: ${formatTime(sessionLimit)}, Blocking: ${if (isBlocking) "ON" else "OFF"}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateBlockedAppsVisibility() {
        if (blockedApps.isEmpty()) {
            emptyAppsContainer.visibility = android.view.View.VISIBLE
            rvBlockedApps.visibility = android.view.View.GONE
        } else {
            emptyAppsContainer.visibility = android.view.View.GONE
            rvBlockedApps.visibility = android.view.View.VISIBLE
        }
    }

    // Data class for blocked apps
    data class BlockedApp(
        val packageName: String,
        val appName: String,
        val appIcon: android.graphics.drawable.Drawable?
    )
}