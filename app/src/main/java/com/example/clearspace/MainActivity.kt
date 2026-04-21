package com.example.clearspace

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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

    private lateinit var stateManager: ClearSpaceStateManager
    private var currentSessionMinutes = 30

    private var selectedAppName: String = ""
    private var selectedAppPackage: String = ""

    private val blockedApps = mutableListOf<BlockedApp>()
    private lateinit var blockedAppsAdapter: BlockedAppAdapter

    private val appPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val appName = data?.getStringExtra(AppPickerActivity.EXTRA_APP_NAME).orEmpty()
                val appPackage = data?.getStringExtra(AppPickerActivity.EXTRA_APP_PACKAGE).orEmpty()

                if (appName.isNotBlank() && appPackage.isNotBlank()) {
                    selectedAppName = appName
                    selectedAppPackage = appPackage

                    stateManager.saveTargetApp(appName, appPackage)
                    loadSelectedAppIntoList()

                    Toast.makeText(this, "$appName selected", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No app selected", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        stateManager = ClearSpaceStateManager(this)

        initViews()
        setupRecyclerView()
        setupGreeting()
        loadSavedState()
        setupBlockingSwitch()
        setupSlider()
        setupChips()
        setupButtons()
        setupBottomNavigation()
        updateBlockedAppsVisibility()
    }

    override fun onResume() {
        super.onResume()

        loadSavedState()
        updateBlockedAppsVisibility()

        // 🔥 IMPORTANT:
        // When returning from Focus/manual challenge, force Home to be selected again
        // so the bold text + active indicator/purple circle are correct.
        bottomNavigation.selectedItemId = R.id.nav_home
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
    }

    private fun setupRecyclerView() {
        blockedAppsAdapter = BlockedAppAdapter(blockedApps)
        rvBlockedApps.layoutManager = LinearLayoutManager(this)
        rvBlockedApps.adapter = blockedAppsAdapter
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
        tvUserName.text = stateManager.getLoggedInUserName()
    }

    private fun loadSavedState() {
        selectedAppName = stateManager.getTargetAppName()
        selectedAppPackage = stateManager.getTargetAppPackage()

        currentSessionMinutes = stateManager.getTimeLimitMinutes().coerceAtLeast(1)
        switchBlocking.isChecked = stateManager.isMonitoringEnabled()

        sliderSessionLimit.value = currentSessionMinutes.toFloat().coerceIn(1f, 600f)
        updateSessionTimeDisplay()
        updateChipSelection()

        tvBlockingStatus.text = if (switchBlocking.isChecked) {
            "Active & Protected"
        } else {
            "Inactive"
        }

        loadSelectedAppIntoList()
    }

    private fun loadSelectedAppIntoList() {
        blockedApps.clear()

        if (selectedAppName.isNotBlank() && selectedAppPackage.isNotBlank()) {
            val icon = try {
                packageManager.getApplicationIcon(selectedAppPackage)
            } catch (_: Exception) {
                null
            }

            blockedApps.add(
                BlockedApp(
                    packageName = selectedAppPackage,
                    appName = selectedAppName,
                    appIcon = icon
                )
            )
        }

        blockedAppsAdapter.notifyDataSetChanged()
        updateBlockedAppsVisibility()
    }

    private fun setupBlockingSwitch() {
        switchBlocking.setOnCheckedChangeListener { _, isChecked ->
            tvBlockingStatus.text = if (isChecked) {
                "Active & Protected"
            } else {
                "Inactive"
            }
        }
    }

    private fun setupSlider() {
        sliderSessionLimit.valueFrom = 1f
        sliderSessionLimit.valueTo = 600f
        sliderSessionLimit.stepSize = 1f

        sliderSessionLimit.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                currentSessionMinutes = value.toInt().coerceAtLeast(1)
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

                if (currentSessionMinutes != minutes) {
                    setSessionTime(minutes)
                }
            }
        }
    }

    private fun setSessionTime(minutes: Int) {
        currentSessionMinutes = minutes.coerceAtLeast(1)
        sliderSessionLimit.value = currentSessionMinutes.toFloat().coerceIn(1f, 600f)
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
            minutes <= 5 -> "Quick focus"
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
            openAppPicker()
        }

        btnSaveSettings.setOnClickListener {
            saveSettings()
        }

        btnNotification.setOnClickListener {
            Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show()
        }

        btnSettings.setOnClickListener {
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppPicker() {
        val intent = Intent(this, AppPickerActivity::class.java)
        appPickerLauncher.launch(intent)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_home

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true

                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    true
                }

                R.id.nav_focus -> {
                    val intent = Intent(this, ChallengeActivity::class.java).apply {
                        putExtra(ChallengeActivity.EXTRA_MODE, ChallengeActivity.MODE_MANUAL)
                    }
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }
    }

    private fun saveSettings() {
        if (selectedAppPackage.isBlank()) {
            Toast.makeText(this, "Please select a target app first", Toast.LENGTH_SHORT).show()
            return
        }

        val isBlocking = switchBlocking.isChecked
        val sessionLimit = currentSessionMinutes.coerceAtLeast(1)

        stateManager.saveMonitoringSettings(isBlocking, sessionLimit)

        if (isBlocking) {
            val startIntent = Intent(this, AppMonitorService::class.java).apply {
                action = AppMonitorService.ACTION_START_MONITORING
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(startIntent)
            } else {
                startService(startIntent)
            }
        } else {
            val stopIntent = Intent(this, AppMonitorService::class.java).apply {
                action = AppMonitorService.ACTION_STOP_MONITORING
            }
            startService(stopIntent)
        }

        Toast.makeText(
            this,
            "Settings saved! App: $selectedAppName, Session: ${formatTime(sessionLimit)}, Blocking: ${if (isBlocking) "ON" else "OFF"}",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateBlockedAppsVisibility() {
        if (blockedApps.isEmpty()) {
            emptyAppsContainer.visibility = View.VISIBLE
            rvBlockedApps.visibility = View.GONE
        } else {
            emptyAppsContainer.visibility = View.GONE
            rvBlockedApps.visibility = View.VISIBLE
        }
    }

    data class BlockedApp(
        val packageName: String,
        val appName: String,
        val appIcon: Drawable?
    )

    class BlockedAppAdapter(
        private val items: List<BlockedApp>
    ) : RecyclerView.Adapter<BlockedAppAdapter.BlockedAppViewHolder>() {

        class BlockedAppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): BlockedAppViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return BlockedAppViewHolder(view)
        }

        override fun onBindViewHolder(holder: BlockedAppViewHolder, position: Int) {
            val item = items[position]
            holder.textView.text = item.appName
        }

        override fun getItemCount(): Int = items.size
    }
}