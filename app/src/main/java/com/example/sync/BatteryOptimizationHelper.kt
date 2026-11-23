package com.example.sync

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

object BatteryOptimizationHelper {
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } catch (_: Throwable) {
            false
        }
    }

    fun createBatteryOptimizationSettingsIntent(context: Context): Intent {
        val settingsIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pm = context.packageManager
        if (settingsIntent.resolveActivity(pm) != null) {
            return settingsIntent
        }
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
