package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.TimetableRepository

class DailyTimetableReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repo = TimetableRepository(context)

        // Only show if enabled
        if (repo.isDailyNotificationEnabled.value) {
            NotificationHelper.showTodayTimetableNotification(context)

            // Reschedule for next day
            val hour = repo.notificationHour.value
            val minute = repo.notificationMinute.value
            NotificationHelper.scheduleDailyNotification(context, hour, minute)
        }

        // Always refresh widgets for the new day
        try {
            com.example.widget.TimetableWidgetProvider.updateAllWidgets(context)
        } catch (_: Exception) {
        }
    }
}
