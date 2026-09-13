package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.TimetableRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

object NotificationHelper {
    const val CHANNEL_ID = "daily_timetable_channel"
    const val CHANNEL_NAME = "毎日の時間割通知"
    const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "毎朝今日の時間割と移動教室、行事予定をお知らせします"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Schedules the daily alarm for timetable notification at the given hour and minute.
     */
    fun scheduleDailyNotification(context: Context, hour: Int = 7, minute: Int = 0) {
        createNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyTimetableReceiver::class.java).apply {
            action = "com.example.ACTION_DAILY_TIMETABLE"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } catch (_: Exception) {
            }
        }
    }

    fun cancelDailyNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyTimetableReceiver::class.java).apply {
            action = "com.example.ACTION_DAILY_TIMETABLE"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Check if notification permission is granted on Android 13+
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Builds and displays the today's timetable notification.
     */
    fun showTodayTimetableNotification(context: Context, isTest: Boolean = false): Boolean {
        createNotificationChannel(context)

        val repo = TimetableRepository(context)
        val selectedClass = repo.selectedClass.value
        val today = LocalDate.now()
        val schedule = repo.getDaySchedule(selectedClass, today, today)

        val testPrefix = if (isTest) "[テスト配信] " else ""
        val header = "${testPrefix}${today.format(DateTimeFormatter.ofPattern("M月d日（E）の時間割", Locale.JAPANESE))}"

        val bodyBuilder = StringBuilder()

        // 授業コマ（なしの場合は書かない）
        val activePeriods = schedule.periods.filter { it.subject.isNotBlank() }.sortedBy { it.period }
        if (activePeriods.isNotEmpty()) {
            activePeriods.forEach { p ->
                val room = if (p.classroom.isNotBlank()) " [${p.classroom}]" else ""
                bodyBuilder.append("${p.period}限: ${p.subject}$room\n")
            }
        }

        // 行事の上に改行
        bodyBuilder.append("\n行事：${schedule.event}")
        bodyBuilder.append("\nメモ：${schedule.memo}")

        val fullNotificationContent = bodyBuilder.toString().trim()

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIconRes = R.drawable.ic_stat_notification
        val largeIcon = try {
            android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_img)
        } catch (_: Throwable) {
            null
        }

        val summaryText = if (activePeriods.isNotEmpty()) {
            activePeriods.joinToString(", ") { it.subject }
        } else {
            "授業の予定はありません"
        }

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIconRes)
            .setContentTitle(header)
            .setContentText(summaryText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullNotificationContent))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        if (largeIcon != null) {
            notificationBuilder.setLargeIcon(largeIcon)
        }

        val notification = notificationBuilder.build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
