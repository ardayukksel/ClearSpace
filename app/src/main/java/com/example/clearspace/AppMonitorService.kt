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
        const val KEY_IS_LOCKED = "isLocked"
        const val KEY_CHALLENGE_ACTIVE = "isChallengeActive"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 1000L

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
            Log.w(TAG, "Required permissions missing. Cannot enforce block properly.")
            resetSessionOnly()
            stopOverlayIfNeeded()
            return
        }

        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val isEnabled = sharedPref.getBoolean(KEY_TARGET_ENABLED, false)
        val timeLimitMinutes = sharedPref.getInt(KEY_TIME_LIMIT, 10).coerceAtLeast(1)
        val targetAppPackage = sharedPref.getString(KEY_TARGET_APP_PACKAGE, "") ?: ""
        val isLocked = sharedPref.getBoolean(KEY_IS_LOCKED, false)
        val isChallengeActive = sharedPref.getBoolean(KEY_CHALLENGE_ACTIVE, false)

        if (!isEnabled || targetAppPackage.isBlank()) {
            Log.d(TAG, "Monitoring disabled or no target app selected.")
            resetAllTrackingState()
            stopOverlayIfNeeded()
            return
        }

        val currentForegroundApp = getCurrentForegroundApp()

        // IMPORTANT:
        // UsageEvents may return blank when no new foreground event occurred.
        // So we keep the last known meaningful foreground app.
        if (currentForegroundApp.isNotBlank()) {
            lastKnownApp = currentForegroundApp
        }

        val effectiveForegroundApp = lastKnownApp
        val ownPackage = packageName

        Log.d(
            TAG,
            "Current=$currentForegroundApp, Effective=$effectiveForegroundApp, Target=$targetAppPackage, Locked=$isLocked, Challenge=$isChallengeActive"
        )

        if (effectiveForegroundApp.isBlank()) {
            Log.d(TAG, "No known foreground app yet.")
            return
        }

        // =========================================================
        // 1. HARD ENFORCEMENT WHEN LOCKED
        // =========================================================
        if (isLocked) {
            if (effectiveForegroundApp == targetAppPackage) {
                Log.d(TAG, "Blocked target app detected while locked. Enforcing overlay.")
                ensureOverlayShowing()
                return
            }

            if (isChallengeActive && effectiveForegroundApp != ownPackage) {
                Log.d(TAG, "User left challenge flow while still locked. Relaunching challenge.")
                launchChallengeActivity()
                return
            }

            // While locked, do not wipe lock state just because user navigated away.
            resetSessionOnly()
            return
        }

        // =========================================================
        // 2. NORMAL SESSION TRACKING
        // =========================================================
        if (effectiveForegroundApp == targetAppPackage) {
            if (sessionStartTime == 0L) {
                sessionStartTime = System.currentTimeMillis()
                Log.d(TAG, "Target app session started for package: $targetAppPackage")
            }

            val currentSessionTimeMs = System.currentTimeMillis() - sessionStartTime
            val limitMs = timeLimitMinutes * 60 * 1000L

            Log.d(
                TAG,
                "Target app active. Elapsed=${currentSessionTimeMs / 1000}s, Limit=${limitMs / 1000}s"
            )

            if (currentSessionTimeMs >= limitMs) {
                Log.d(TAG, "Session limit reached. Locking app and launching overlay.")

                sharedPref.edit()
                    .putBoolean(KEY_IS_LOCKED, true)
                    .putBoolean(KEY_CHALLENGE_ACTIVE, false)
                    .apply()

                ensureOverlayShowing()
                return
            }
        } else {
            if (sessionStartTime != 0L) {
                Log.d(TAG, "User left target app before lock. Resetting session timer.")
            }

            resetSessionOnly()
            stopOverlayIfNeeded()
        }
    }

    private fun ensureOverlayShowing() {
        if (!OverlayService.isRunning) {
            val overlayIntent = Intent(this, OverlayService::class.java)
            startService(overlayIntent)
        }
    }

    private fun launchChallengeActivity() {
        val challengeIntent = Intent(this, ChallengeActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }
        startActivity(challengeIntent)
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

    private fun resetSessionOnly() {
        sessionStartTime = 0L
    }

    private fun resetAllTrackingState() {
        sessionStartTime = 0L
        lastKnownApp = ""

        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit()
            .putBoolean(KEY_IS_LOCKED, false)
            .putBoolean(KEY_CHALLENGE_ACTIVE, false)
            .apply()
    }

    private fun stopOverlayIfNeeded() {
        stopService(Intent(this, OverlayService::class.java))
    }

    private fun stopMonitoring() {
        handler.removeCallbacks(monitorRunnable)
        resetAllTrackingState()
        stopOverlayIfNeeded()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}