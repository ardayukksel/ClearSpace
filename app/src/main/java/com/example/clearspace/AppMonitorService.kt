package com.example.clearspace

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import android.os.SystemClock
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
        private const val ACTION_RESTART_MONITORING = "com.example.clearspace.action.RESTART_MONITORING"

        const val PREFS_NAME = "ClearSpacePrefs"
        const val KEY_TARGET_ENABLED = "isTargetEnabled"
        const val KEY_TIME_LIMIT = "timeLimit"
        const val KEY_TARGET_APP_NAME = "targetAppName"
        const val KEY_TARGET_APP_PACKAGE = "targetAppPackage"
        const val KEY_IS_LOCKED = "isLocked"
        const val KEY_CHALLENGE_ACTIVE = "isChallengeActive"
        const val KEY_CHALLENGE_TRANSITION_UNTIL = "challengeTransitionUntil"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 250L

    private var sessionStartTime = 0L
    private var lastKnownApp = ""
    private var isStoppingIntentionally = false
    private var lastChallengeLaunchAt = 0L

    private lateinit var stateManager: ClearSpaceStateManager

    private val monitorRunnable = object : Runnable {
        override fun run() {
            checkAppUsageAndBlock()
            handler.postDelayed(this, checkInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        stateManager = ClearSpaceStateManager(this)
        startAsForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_MONITORING -> {
                Log.d(TAG, "Stopping monitoring service intentionally.")
                isStoppingIntentionally = true
                cancelScheduledRestart()
                stopMonitoring(clearState = true)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START_MONITORING,
            ACTION_RESTART_MONITORING,
            null -> {
                Log.d(TAG, "Starting monitoring service. Action=${intent?.action}")
                isStoppingIntentionally = false
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

        val isEnabled = stateManager.isMonitoringEnabled()
        val timeLimitMinutes = stateManager.getTimeLimitMinutes()
        val targetAppPackage = stateManager.getTargetAppPackage()
        val isLocked = stateManager.isLocked()
        val isChallengeActive = stateManager.isChallengeActive()
        val isChallengeTransitioning = isInChallengeTransitionWindow()
        val ownPackage = packageName

        if (!isEnabled || targetAppPackage.isBlank()) {
            Log.d(TAG, "Monitoring disabled or no target app selected.")
            resetAllTrackingState()
            stopOverlayIfNeeded()
            stopSelf()
            return
        }

        val currentForegroundApp = getCurrentForegroundApp()

        if (currentForegroundApp.isNotBlank()) {
            lastKnownApp = currentForegroundApp
        }

        val effectiveForegroundApp = when {
            currentForegroundApp.isNotBlank() -> currentForegroundApp
            lastKnownApp.isNotBlank() -> lastKnownApp
            else -> ""
        }

        Log.d(
            TAG,
            "Current=$currentForegroundApp, Effective=$effectiveForegroundApp, Target=$targetAppPackage, Locked=$isLocked, Challenge=$isChallengeActive, Transitioning=$isChallengeTransitioning"
        )

        if (effectiveForegroundApp.isBlank()) {
            return
        }

        if (isLocked) {
            resetSessionOnly()

            if (isChallengeTransitioning) {
                stopOverlayIfNeeded()
                return
            }

            if (isChallengeActive) {
                stopOverlayIfNeeded()

                if (effectiveForegroundApp != ownPackage) {
                    relaunchChallengeIfNeeded()
                }

                return
            }

            // Locked but challenge not started yet:
            // keep overlay enforced globally over any app/home screen,
            // and if user somehow gets back into the target app, keep re-enforcing.
            if (!OverlayService.isRunning) {
                ensureOverlayShowing()
            }

            // If the user gets back to the target app or any other app while locked,
            // keep the overlay alive globally.
            if (effectiveForegroundApp != ownPackage || effectiveForegroundApp == targetAppPackage) {
                ensureOverlayShowing()
            }

            return
        }

        if (effectiveForegroundApp == targetAppPackage) {
            if (sessionStartTime == 0L) {
                sessionStartTime = System.currentTimeMillis()
                Log.d(TAG, "Target app session started for package: $targetAppPackage")
            }

            val currentSessionTimeMs = System.currentTimeMillis() - sessionStartTime
            val limitMs = timeLimitMinutes.coerceAtLeast(1) * 60 * 1000L

            Log.d(TAG, "Target app active. Elapsed=${currentSessionTimeMs / 1000}s, Limit=${limitMs / 1000}s")

            if (currentSessionTimeMs >= limitMs) {
                Log.d(TAG, "Session limit reached. Locking app and launching overlay.")
                stateManager.setLockAndChallenge(locked = true, challengeActive = false)
                clearChallengeTransitionWindow()
                ensureOverlayShowing()
                return
            }
        } else {
            if (sessionStartTime != 0L) {
                Log.d(TAG, "User left target app before lock. Resetting session timer.")
            }

            resetSessionOnly()

            if (!isLocked) {
                stopOverlayIfNeeded()
            }
        }
    }

    private fun ensureOverlayShowing() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isChallengeActive = prefs.getBoolean(KEY_CHALLENGE_ACTIVE, false)
        val transitionUntil = prefs.getLong(KEY_CHALLENGE_TRANSITION_UNTIL, 0L)
        val inTransitionWindow = System.currentTimeMillis() < transitionUntil

        if (isChallengeActive || inTransitionWindow) {
            Log.d(TAG, "Not showing overlay because challenge flow is already active/transitioning.")
            return
        }

        if (!OverlayService.isRunning) {
            val overlayIntent = Intent(this, OverlayService::class.java)
            startService(overlayIntent)
        }
    }

    private fun relaunchChallengeIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastChallengeLaunchAt < 700L) {
            return
        }

        lastChallengeLaunchAt = now
        launchChallengeActivity()
    }

    private fun launchChallengeActivity() {
        val challengeIntent = Intent(this, ChallengeActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
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

    private fun isInChallengeTransitionWindow(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val until = prefs.getLong(KEY_CHALLENGE_TRANSITION_UNTIL, 0L)
        return System.currentTimeMillis() < until
    }

    private fun clearChallengeTransitionWindow() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_CHALLENGE_TRANSITION_UNTIL, 0L).apply()
    }

    private fun scheduleServiceRestart() {
        val isEnabled = stateManager.isMonitoringEnabled()

        if (!isEnabled) {
            Log.d(TAG, "Monitoring is disabled. Not scheduling restart.")
            return
        }

        Log.d(TAG, "Scheduling monitor service restart.")

        val restartIntent = Intent(this, AppMonitorService::class.java).apply {
            action = ACTION_RESTART_MONITORING
        }

        val pendingIntent = PendingIntent.getService(
            this,
            2001,
            restartIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 1000L,
            pendingIntent
        )
    }

    private fun cancelScheduledRestart() {
        val restartIntent = Intent(this, AppMonitorService::class.java).apply {
            action = ACTION_RESTART_MONITORING
        }

        val pendingIntent = PendingIntent.getService(
            this,
            2001,
            restartIntent,
            PendingIntent.FLAG_NO_CREATE or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        pendingIntent?.let {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(it)
            it.cancel()
            Log.d(TAG, "Cancelled scheduled monitor restart.")
        }
    }

    private fun resetSessionOnly() {
        sessionStartTime = 0L
    }

    private fun resetAllTrackingState() {
        sessionStartTime = 0L
        lastKnownApp = ""
        lastChallengeLaunchAt = 0L
        clearChallengeTransitionWindow()
        stateManager.resetLockState()
    }

    private fun stopOverlayIfNeeded() {
        stopService(Intent(this, OverlayService::class.java))
    }

    private fun stopMonitoring(clearState: Boolean) {
        handler.removeCallbacks(monitorRunnable)

        if (clearState) {
            resetAllTrackingState()
        } else {
            resetSessionOnly()
        }

        stopOverlayIfNeeded()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "Service task removed.")

        if (!isStoppingIntentionally) {
            scheduleServiceRestart()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed. IntentionalStop=$isStoppingIntentionally")

        handler.removeCallbacks(monitorRunnable)

        if (!isStoppingIntentionally) {
            stopMonitoring(clearState = false)
            scheduleServiceRestart()
        } else {
            stopMonitoring(clearState = true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}