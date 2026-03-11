package com.example.clearspace

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.os.Build
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat

class AppMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 2000L

    private var isBlocking = false
    private var sessionStartTime = 0L
    private var lastKnownApp = ""

    private val monitorRunnable = object : Runnable {
        override fun run() {
            checkAppUsageAndBlock()
            handler.postDelayed(this, checkInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(1, createNotification())
        }

        handler.post(monitorRunnable)
    }

    private fun createNotification(): android.app.Notification {
        val channelId = "monitor_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "App Monitor", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("ClearSpace Monitoring Active")
            .setContentText("Keeping you focused!")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    private fun checkAppUsageAndBlock() {
        // 1. Load user settings
        val sharedPref = getSharedPreferences("ClearSpacePrefs", Context.MODE_PRIVATE)
        val isEnabled = sharedPref.getBoolean("isTargetEnabled", false)
        val timeLimitMinutes = sharedPref.getInt("timeLimit", 10)

        // 2. Load the specific app chosen by the user in the popup
        val targetAppPackage = sharedPref.getString("targetAppPackage", "")

        // 3. Stop monitoring if switch is OFF or no app is selected
        if (!isEnabled || targetAppPackage.isNullOrEmpty()) {
            isBlocking = false
            sessionStartTime = 0L
            lastKnownApp = ""
            return
        }

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10000

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var currentApp = ""

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentApp = event.packageName
            }
        }

        if (currentApp.isNotEmpty()) {
            lastKnownApp = currentApp
        }

        // 4. Check if the currently open app matches the USER-CHOSEN app
        if (lastKnownApp == targetAppPackage) {
            if (sessionStartTime == 0L) {
                sessionStartTime = System.currentTimeMillis()
                Log.d("AppMonitor", "Target app session started.")
            }

            val currentSessionTimeMs = System.currentTimeMillis() - sessionStartTime
            // limitMs = (minutes * 60 seconds * 1000 ms)
            val limitMs = timeLimitMinutes * 60 * 1000L

            if (currentSessionTimeMs >= limitMs && !isBlocking) {
                isBlocking = true
                Log.d("AppMonitor", "Session limit reached! Blocking.")

                val overlayIntent = Intent(this, OverlayService::class.java)
                startService(overlayIntent)
            }
        }
        else if (lastKnownApp != "com.example.clearspace" && lastKnownApp != "") {
            isBlocking = false
            sessionStartTime = 0L
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(monitorRunnable)
    }
}