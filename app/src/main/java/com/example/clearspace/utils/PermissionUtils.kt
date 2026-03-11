package com.example.clearspace.utils

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.Process

object PermissionUtils {

    // 1. Check if the Usage Stats permission is granted
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // 2. Redirect user to the Settings screen to grant Usage Stats permission
    fun requestUsageStatsPermission(context: Context) {
        if (!hasUsageStatsPermission(context)) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    // 3. Check if the Overlay (System Alert Window) permission is granted
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    // 4. Redirect user to the Settings screen to grant Overlay permission
    fun requestOverlayPermission(context: Context) {
        if (!hasOverlayPermission(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}