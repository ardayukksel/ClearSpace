package com.example.clearspace

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.clearspace.utils.PermissionUtils

class AppMonitorService : Service() {

    companion object {
        private const val TAG = "AppMonitorService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "monitor_channel"

        const val ACTION_START_MONITORING = "com.example.clearspace.action.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.clearspace.action.STOP_MONITORING"

        const val PREFS_NAME = "ClearSpacePrefs"
        const val KEY_TARGET_ENABLED = "isTargetEnabled"
        const val KEY_TIME_LIMIT = "timeLimit"
        const val KEY_TARGET_APP_NAME = "targetAppName"
        const val KEY_TARGET_APP_PACKAGE = "targetAppPackage"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 2000L

    private var overlayShown = false
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
        startAsForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_MONITORING -> {
                Log.d(TAG, "Stopping monitoring service.")
                stopMonitoring()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START_MONITORING, null -> {
                Log.d(TAG, "Starting monitoring service.")
                handler.removeCallbacks(monitorRunnable)
                handler.post(monitorRunnable)
            }
        }

        return START_STICKY
    }

    private fun startAsForegroundService() {
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Monitor",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ClearSpace Monitoring Active")
            .setContentText("Monitoring selected app usage")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    private fun checkAppUsageAndBlock() {
        if (!PermissionUtils.hasUsageStatsPermission(this) || !PermissionUtils.hasOverlayPermission(this)) {
            Log.w(TAG, "Required permissions missing. Stopping active blocking state.")
            resetTrackingState()
            stopOverlayIfNeeded()
            return
        }

        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = sharedPref.getBoolean(KEY_TARGET_ENABLED, false)
        val timeLimitMinutes = sharedPref.getInt(KEY_TIME_LIMIT, 10).coerceAtLeast(1)
        val targetAppPackage = sharedPref.getString(KEY_TARGET_APP_PACKAGE, "") ?: ""

        if (!isEnabled || targetAppPackage.isBlank()) {
            Log.d(TAG, "Monitoring disabled or no target app selected.")
            resetTrackingState()
            stopOverlayIfNeeded()
            return
        }

        val currentForegroundApp = getCurrentForegroundApp()

        if (currentForegroundApp.isNotBlank()) {
            lastKnownApp = currentForegroundApp
        }

        if (lastKnownApp == targetAppPackage) {
            if (sessionStartTime == 0L) {
                sessionStartTime = System.currentTimeMillis()
                Log.d(TAG, "Target app session started for package: $targetAppPackage")
            }

            val currentSessionTimeMs = System.currentTimeMillis() - sessionStartTime
            val limitMs = timeLimitMinutes * 60 * 1000L

            Log.d(
                TAG,
                "Target app in foreground. Elapsed=${currentSessionTimeMs / 1000}s, Limit=${limitMs / 1000}s"
            )

            if (currentSessionTimeMs >= limitMs && !overlayShown) {
                overlayShown = true
                Log.d(TAG, "Session limit reached. Launching overlay.")

                val overlayIntent = Intent(this, OverlayService::class.java)
                startService(overlayIntent)
            }
        } else {
            // User left the selected app
            if (sessionStartTime != 0L || overlayShown) {
                Log.d(TAG, "User left target app. Resetting session/block state.")
            }

            resetTrackingState()
            stopOverlayIfNeeded()
        }
    }

    private fun getCurrentForegroundApp(): String {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10000

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        var currentApp = ""

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)

            if (
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                currentApp = event.packageName
            }
        }

        return currentApp
    }

    private fun resetTrackingState() {
        sessionStartTime = 0L
        lastKnownApp = ""
        overlayShown = false
    }

    private fun stopOverlayIfNeeded() {
        stopService(Intent(this, OverlayService::class.java))
    }

    private fun stopMonitoring() {
        handler.removeCallbacks(monitorRunnable)
        resetTrackingState()
        stopOverlayIfNeeded()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}