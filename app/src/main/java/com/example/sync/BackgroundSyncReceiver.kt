package com.example.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BackgroundSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            BackgroundSyncScheduler.schedulePeriodicSync(context.applicationContext)
        }
    }
}
