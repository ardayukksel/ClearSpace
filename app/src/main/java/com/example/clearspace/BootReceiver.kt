package com.example.clearspace

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted! Starting ClearSpace monitor...")

            val sharedPref = context.getSharedPreferences(AppMonitorService.PREFS_NAME, Context.MODE_PRIVATE)
            val isEnabled = sharedPref.getBoolean(AppMonitorService.KEY_TARGET_ENABLED, false)

            if (isEnabled) {
                val serviceIntent = Intent(context, AppMonitorService::class.java).apply {
                    action = AppMonitorService.ACTION_START_MONITORING
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}