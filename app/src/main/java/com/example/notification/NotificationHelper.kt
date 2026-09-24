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

    const val CHANNEL_ID_NEXT_CLASS = "next_class_notification_channel"
    const val CHANNEL_NAME_NEXT_CLASS = "次の授業通知"
    const val NOTIFICATION_ID_NEXT_CLASS = 2000

    const val ACTION_NEXT_CLASS_NOTIFICATION = "com.example.ACTION_NEXT_CLASS_NOTIFICATION"
    const val ACTION_SCHEDULE_NEXT_CLASS_MAINTENANCE = "com.example.ACTION_SCHEDULE_NEXT_CLASS_MAINTENANCE"
    const val EXTRA_PERIOD = "extra_period"
    const val EXTRA_DATE_EPOCH_DAY = "extra_date_epoch_day"

    data class PeriodTime(
        val period: Int,
        val startHour: Int,
        val startMinute: Int,
        val endHour: Int,
        val endMinute: Int
    )

    val PERIOD_TIMES = listOf(
        PeriodTime(1, 8, 30, 9, 35),
        PeriodTime(2, 9, 45, 10, 50),
        PeriodTime(3, 11, 0, 12, 5),
        PeriodTime(4, 12, 55, 14, 0),
        PeriodTime(5, 14, 10, 15, 15)
    )

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "毎朝今日の時間割と移動教室、行事予定をお知らせします"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val nextClassChannel = NotificationChannel(CHANNEL_ID_NEXT_CLASS, CHANNEL_NAME_NEXT_CLASS, importance).apply {
                description = "各授業が始まる前に次の科目を通知します"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
            notificationManager?.createNotificationChannel(nextClassChannel)
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

        val repo = TimetableRepository.getInstance(context)
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

        val eventText = if (schedule.event.isNotBlank()) schedule.event.trim() else "なし"
        val memoText = if (schedule.memo.isNotBlank()) schedule.memo.trim() else "なし"

        // 行事の上に改行
        bodyBuilder.append("\n行事：$eventText")
        bodyBuilder.append("\nメモ：$memoText")

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
        } else if (schedule.event.isNotBlank()) {
            "行事: ${schedule.event}"
        } else {
            "予定はありません"
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

    /**
     * Checks whether the specified date is a weekend or registered holiday.
     */
    fun isHoliday(date: LocalDate, repo: TimetableRepository): Boolean {
        if (date.dayOfWeek == java.time.DayOfWeek.SATURDAY || date.dayOfWeek == java.time.DayOfWeek.SUNDAY) {
            return true
        }
        if (repo.csvSyncManager.isHoliday(date)) {
            return true
        }
        return repo.csvSyncManager.getHolidayName(date) != null
    }

    /**
     * Builds and sends the next class notification.
     * Top notification: 次は {subject} です。
     * Detailed expanded text: {period} 限の科目は {subject} です。
     * If the subject is empty or it is a holiday, no notification is sent.
     */
    fun showNextClassNotification(
        context: Context,
        period: Int,
        date: LocalDate = LocalDate.now(),
        isTest: Boolean = false
    ): Boolean {
        createNotificationChannel(context)

        val repo = TimetableRepository.getInstance(context)

        // 休日の場合は通知しない
        if (!isTest && isHoliday(date, repo)) {
            return false
        }

        val selectedClass = repo.selectedClass.value
        val schedule = repo.getDaySchedule(selectedClass, date, date)
        val periodSchedule = schedule.periods.find { it.period == period }
        var subject = periodSchedule?.subject?.trim() ?: ""

        if (isTest && subject.isBlank()) {
            subject = "数学"
        }

        // 次の科目がない場合は通知しない
        if (subject.isBlank() || subject == "なし" || subject == "-") {
            return false
        }

        val title = "次は ${subject} です。"
        val detailText = "${period} 限の科目は ${subject} です。"

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            period,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIconRes = R.drawable.ic_stat_notification
        val largeIcon = try {
            android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_img)
        } catch (_: Throwable) {
            null
        }

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_NEXT_CLASS)
            .setSmallIcon(smallIconRes)
            .setContentTitle(title)
            .setContentText(detailText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detailText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        if (largeIcon != null) {
            notificationBuilder.setLargeIcon(largeIcon)
        }

        val notification = notificationBuilder.build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_NEXT_CLASS, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * Schedules alarms for all remaining periods of the day and a maintenance check for tomorrow.
     */
    fun scheduleNextClassAlarms(context: Context) {
        createNotificationChannel(context)

        val repo = TimetableRepository.getInstance(context)
        if (!repo.isNextClassNotificationEnabled.value) {
            cancelNextClassAlarms(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val leadMinutes = repo.nextClassLeadMinutes.value
        val today = LocalDate.now()
        val now = java.time.LocalDateTime.now()

        // 1. 当日の各限のアラーム（休日以外）
        if (!isHoliday(today, repo)) {
            val schedule = repo.getDaySchedule(repo.selectedClass.value, today, today)
            for (pt in PERIOD_TIMES) {
                val periodStartTime = java.time.LocalDateTime.of(today, java.time.LocalTime.of(pt.startHour, pt.startMinute))
                val notifyTime = periodStartTime.minusMinutes(leadMinutes.toLong())

                if (notifyTime.isAfter(now)) {
                    val subject = schedule.periods.find { it.period == pt.period }?.subject?.trim() ?: ""
                    if (subject.isNotBlank() && subject != "なし" && subject != "-") {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, notifyTime.year)
                            set(Calendar.MONTH, notifyTime.monthValue - 1)
                            set(Calendar.DAY_OF_MONTH, notifyTime.dayOfMonth)
                            set(Calendar.HOUR_OF_DAY, notifyTime.hour)
                            set(Calendar.MINUTE, notifyTime.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val intent = Intent(context, NextClassNotificationReceiver::class.java).apply {
                            action = ACTION_NEXT_CLASS_NOTIFICATION
                            putExtra(EXTRA_PERIOD, pt.period)
                            putExtra(EXTRA_DATE_EPOCH_DAY, today.toEpochDay())
                        }
                        val requestCode = 3000 + pt.period
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        setExactAlarm(alarmManager, cal.timeInMillis, pendingIntent)
                    }
                }
            }
        }

        // 2. 翌朝（07:00）に当日の時間割アラームを仕込むメンテナンスアラーム
        val tomorrowCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val maintIntent = Intent(context, NextClassNotificationReceiver::class.java).apply {
            action = ACTION_SCHEDULE_NEXT_CLASS_MAINTENANCE
        }
        val maintPendingIntent = PendingIntent.getBroadcast(
            context,
            3000,
            maintIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        setExactAlarm(alarmManager, tomorrowCal.timeInMillis, maintPendingIntent)
    }

    private fun setExactAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: Exception) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } catch (_: Exception) {
            }
        }
    }

    fun cancelNextClassAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (p in 0..5) {
            val intent = Intent(context, NextClassNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                3000 + p,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
