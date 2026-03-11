package com.example.clearspace

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 1. Execute when the device broadcasts that it has finished booting (BOOT_COMPLETED)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted! Starting ClearSpace monitor...")

            // 2. Check SharedPreferences to see if the user left the monitoring switch ON
            val sharedPref = context.getSharedPreferences("ClearSpacePrefs", Context.MODE_PRIVATE)
            val isEnabled = sharedPref.getBoolean("isTargetEnabled", false)

            // 3. Start the AppMonitorService automatically only if it was enabled
            if (isEnabled) {
                val serviceIntent = Intent(context, AppMonitorService::class.java)

                // Android 8.0 (Oreo) and above require startForegroundService
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}