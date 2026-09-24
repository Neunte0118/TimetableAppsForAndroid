package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.TimetableRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar

class TimetableWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        scheduleAlarms(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (appWidgetId in appWidgetIds) {
            editor.remove("offset_$appWidgetId")
        }
        editor.apply()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        when (intent.action) {
            ACTION_PREV_DAY -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val currentOffset = getWidgetDateOffset(context, appWidgetId)
                    setWidgetDateOffset(context, appWidgetId, currentOffset - 1)
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
            ACTION_NEXT_DAY -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val currentOffset = getWidgetDateOffset(context, appWidgetId)
                    setWidgetDateOffset(context, appWidgetId, currentOffset + 1)
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
            ACTION_RESET_TODAY -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    setWidgetDateOffset(context, appWidgetId, 0)
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
            ACTION_REFRESH_WIDGET -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "時間割を更新中...", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        val repo = TimetableRepository.getInstance(context)
                        repo.syncCsvData()
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "時間割を更新しました", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("TimetableWidget", "Refresh error", e)
                    } finally {
                        updateAllWidgets(context)
                        EventMemoWidgetProvider.updateAllWidgets(context)
                    }
                }
            }
            ACTION_MIDNIGHT_UPDATE,
            Intent.ACTION_DATE_CHANGED -> {
                // 0:00 リセット: ナビゲーションオフセットをクリアして全ウィジェット更新
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val editor = prefs.edit()
                for (key in prefs.all.keys) {
                    if (key.startsWith("offset_")) {
                        editor.remove(key)
                    }
                }
                editor.apply()
                updateAllWidgets(context)
                scheduleAlarms(context)
            }
            ACTION_AFTERNOON_UPDATE,
            Intent.ACTION_TIME_CHANGED,
            "android.intent.action.TIME_SET",
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                updateAllWidgets(context)
                scheduleAlarms(context)
            }
        }
    }

    companion object {
        const val PREFS_NAME = "timetable_widget_prefs"
        const val ACTION_REFRESH_WIDGET = "com.example.widget.ACTION_REFRESH_TIMETABLE_WIDGET"
        const val ACTION_PREV_DAY = "com.example.widget.ACTION_PREV_DAY"
        const val ACTION_NEXT_DAY = "com.example.widget.ACTION_NEXT_DAY"
        const val ACTION_RESET_TODAY = "com.example.widget.ACTION_RESET_TODAY"
        const val ACTION_MIDNIGHT_UPDATE = "com.example.widget.ACTION_MIDNIGHT_UPDATE"
        const val ACTION_AFTERNOON_UPDATE = "com.example.widget.ACTION_AFTERNOON_UPDATE"
        private const val MIDNIGHT_ALARM_REQ_CODE = 1002
        private const val AFTERNOON_ALARM_REQ_CODE = 1003

        fun getWidgetDateOffset(context: Context, appWidgetId: Int): Long {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getLong("offset_$appWidgetId", 0L)
        }

        fun setWidgetDateOffset(context: Context, appWidgetId: Int, offset: Long) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong("offset_$appWidgetId", offset).apply()
        }

        /**
         * 15:15を過ぎたら明日の日付を基準とし、0:00に今日の日付へリセット
         */
        fun getBaseDate(): LocalDate {
            val now = LocalTime.now()
            val cutoff = LocalTime.of(15, 15)
            return if (!now.isBefore(cutoff)) {
                LocalDate.now().plusDays(1)
            } else {
                LocalDate.now()
            }
        }

        private fun hasValidSubject(subject: String?): Boolean {
            val s = subject?.trim() ?: ""
            return s.isNotBlank() && s != "-" && s != "なし" && s != "授業なし" && s != "空き"
        }

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val componentName = ComponentName(context, TimetableWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                    }
                }
            } catch (_: Exception) {
            }
        }

        fun scheduleAlarms(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

                // 1. 午前0時のリセット更新アラーム (0:00:02)
                val midnightIntent = Intent(context, TimetableWidgetProvider::class.java).apply {
                    action = ACTION_MIDNIGHT_UPDATE
                }
                val midnightPending = PendingIntent.getBroadcast(
                    context,
                    MIDNIGHT_ALARM_REQ_CODE,
                    midnightIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val midnightCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 2)
                    set(Calendar.MILLISECOND, 0)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        midnightCal.timeInMillis,
                        midnightPending
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        midnightCal.timeInMillis,
                        midnightPending
                    )
                }

                // 2. 15:15の明日切り替え更新アラーム (15:15:02)
                val afternoonIntent = Intent(context, TimetableWidgetProvider::class.java).apply {
                    action = ACTION_AFTERNOON_UPDATE
                }
                val afternoonPending = PendingIntent.getBroadcast(
                    context,
                    AFTERNOON_ALARM_REQ_CODE,
                    afternoonIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val afternoonCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 15)
                    set(Calendar.MINUTE, 15)
                    set(Calendar.SECOND, 2)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        afternoonCal.timeInMillis,
                        afternoonPending
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        afternoonCal.timeInMillis,
                        afternoonPending
                    )
                }
            } catch (_: Exception) {
            }
        }

        fun scheduleMidnightUpdate(context: Context) {
            scheduleAlarms(context)
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val repo = TimetableRepository.getInstance(context)
                val views = RemoteViews(context.packageName, R.layout.widget_timetable)

                // Launch App on clicking root or date
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val mainPending = PendingIntent.getActivity(
                    context,
                    appWidgetId * 100 + 4,
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root_layout, mainPending)
                views.setOnClickPendingIntent(R.id.widget_title_date, mainPending)
                views.setOnClickPendingIntent(R.id.widget_setup_required_container, mainPending)

                // 1. 利用規約の同意、クラス選択が終わるまではウィジェットの時間割は表示しない
                val isSetupCompleted = repo.hasCompletedTermsAndClass()
                if (!isSetupCompleted) {
                    views.setTextViewText(R.id.widget_title_date, "-")
                    views.setViewVisibility(R.id.widget_setup_required_container, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_no_timetable_container, View.GONE)
                    views.setViewVisibility(R.id.widget_periods_container, View.GONE)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    return
                }

                views.setViewVisibility(R.id.widget_setup_required_container, View.GONE)

                // 2. 日付の算出 (15:15以降は明日を基準とし、0:00にリセット)
                val selectedClass = repo.selectedClass.value
                val offset = getWidgetDateOffset(context, appWidgetId)
                val targetDate = getBaseDate().plusDays(offset)
                val today = LocalDate.now()
                val schedule = repo.getDaySchedule(selectedClass, targetDate, today)

                // Header Date String: "今日", "明日", "昨日", or "M/d"
                val dateStr = when (targetDate) {
                    today -> "今日"
                    today.plusDays(1) -> "明日"
                    today.minusDays(1) -> "昨日"
                    else -> "${targetDate.monthValue}/${targetDate.dayOfMonth}"
                }
                views.setTextViewText(R.id.widget_title_date, dateStr)

                // Navigation Buttons Intents
                val prevIntent = Intent(context, TimetableWidgetProvider::class.java).apply {
                    action = ACTION_PREV_DAY
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse("widget://timetable/$appWidgetId/prev")
                }
                val prevPending = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 100 + 1,
                    prevIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_prev_day, prevPending)

                val nextIntent = Intent(context, TimetableWidgetProvider::class.java).apply {
                    action = ACTION_NEXT_DAY
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse("widget://timetable/$appWidgetId/next")
                }
                val nextPending = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 100 + 2,
                    nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_next_day, nextPending)

                val homeIntent = Intent(context, TimetableWidgetProvider::class.java).apply {
                    action = ACTION_RESET_TODAY
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse("widget://timetable/$appWidgetId/home")
                }
                val homePending = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 100 + 3,
                    homeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_home, homePending)

                // 3. その時間の時間割が存在しない場合、5限なども行ごと削除 (View.GONE)
                // ただし、1限: 国語, 2限: なし, 3限: 数学 のように間に空きがある場合は2限にハイフン(-)を表示
                val periodContainers = listOf(
                    R.id.widget_p1_container,
                    R.id.widget_p2_container,
                    R.id.widget_p3_container,
                    R.id.widget_p4_container,
                    R.id.widget_p5_container
                )
                val periodNums = listOf(
                    R.id.widget_p1_num,
                    R.id.widget_p2_num,
                    R.id.widget_p3_num,
                    R.id.widget_p4_num,
                    R.id.widget_p5_num
                )
                val periodSubjects = listOf(
                    R.id.widget_p1_subject,
                    R.id.widget_p2_subject,
                    R.id.widget_p3_subject,
                    R.id.widget_p4_subject,
                    R.id.widget_p5_subject
                )
                val periodRooms = listOf(
                    R.id.widget_p1_room,
                    R.id.widget_p2_room,
                    R.id.widget_p3_room,
                    R.id.widget_p4_room,
                    R.id.widget_p5_room
                )

                val lastPeriodWithSubject = (1..5).indexOfLast { p ->
                    val cell = schedule.periods.find { it.period == p }
                    hasValidSubject(cell?.subject)
                } + 1

                val isColorEnabled = repo.isSubjectColorEnabled.value
                val colorGroups = repo.subjectColorGroups.value
                val defaultSubjectColor = context.getColor(R.color.widget_text_primary)

                if (lastPeriodWithSubject == 0) {
                    views.setViewVisibility(R.id.widget_periods_container, View.GONE)
                    views.setViewVisibility(R.id.widget_no_timetable_container, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_no_timetable_container, View.GONE)
                    views.setViewVisibility(R.id.widget_periods_container, View.VISIBLE)

                    for (i in 0 until 5) {
                        val periodNum = i + 1
                        if (periodNum > lastPeriodWithSubject) {
                            // その時間の時間割が存在しない（最終限を超えている）場合は5限等も含め行ごと削除
                            views.setViewVisibility(periodContainers[i], View.GONE)
                        } else {
                            views.setViewVisibility(periodContainers[i], View.VISIBLE)
                            val cell = schedule.periods.find { it.period == periodNum } ?: schedule.periods.getOrNull(i)

                            views.setTextViewText(periodNums[i], "$periodNum")
                            if (hasValidSubject(cell?.subject)) {
                                val subjectName = cell!!.subject.trim()
                                views.setTextViewText(periodSubjects[i], subjectName)

                                val roomText = cell.classroom.ifBlank { "" }
                                views.setTextViewText(periodRooms[i], roomText)

                                val defaultRoomColor = context.getColor(R.color.widget_text_muted)
                                val examRoomColor = context.getColor(R.color.widget_text_exam_room)

                                if (cell.isUnselectedElective) {
                                    views.setTextColor(periodSubjects[i], context.getColor(R.color.widget_text_unselected))
                                    views.setTextColor(periodRooms[i], defaultRoomColor)
                                } else if (cell.isExam) {
                                    views.setTextColor(periodSubjects[i], context.getColor(R.color.widget_text_exam))
                                    views.setTextColor(periodRooms[i], examRoomColor)
                                } else if (isColorEnabled) {
                                    val colorLong = com.example.model.SubjectColorDefaults.getColorForSubject(subjectName, colorGroups)
                                    views.setTextColor(periodSubjects[i], colorLong.toInt())
                                    views.setTextColor(periodRooms[i], defaultRoomColor)
                                } else if (repo.isHighlightChangedPeriods.value && cell.isChanged) {
                                    views.setTextColor(periodSubjects[i], context.getColor(R.color.widget_text_changed))
                                    views.setTextColor(periodRooms[i], defaultRoomColor)
                                } else {
                                    views.setTextColor(periodSubjects[i], defaultSubjectColor)
                                    views.setTextColor(periodRooms[i], defaultRoomColor)
                                }
                            } else {
                                // 間の空きコマにはハイフンを表示
                                views.setTextViewText(periodSubjects[i], "-")
                                views.setTextColor(periodSubjects[i], defaultSubjectColor)
                                views.setTextViewText(periodRooms[i], "")
                            }

                            val bgRes = when {
                                cell?.isUnselectedElective == true -> R.drawable.widget_period_cell_unselected_bg
                                cell?.isExam == true -> R.drawable.widget_period_cell_exam_bg
                                repo.isHighlightChangedPeriods.value && cell?.isChanged == true -> R.drawable.widget_period_cell_changed_bg
                                else -> R.drawable.widget_period_cell_bg
                            }
                            views.setInt(periodContainers[i], "setBackgroundResource", bgRes)
                        }
                    }
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e("TimetableWidget", "Error updating widget $appWidgetId", e)
            }
        }
    }
}
