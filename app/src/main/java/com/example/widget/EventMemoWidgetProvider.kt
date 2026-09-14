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
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.TimetableRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class EventMemoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        scheduleMidnightUpdate(context)
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
            editor.remove("em_offset_$appWidgetId")
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
            ACTION_EM_PREV_DAY -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val currentOffset = getWidgetDateOffset(context, appWidgetId)
                    setWidgetDateOffset(context, appWidgetId, currentOffset - 1)
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
            ACTION_EM_NEXT_DAY -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val currentOffset = getWidgetDateOffset(context, appWidgetId)
                    setWidgetDateOffset(context, appWidgetId, currentOffset + 1)
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
            ACTION_EM_RESET_TODAY -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    setWidgetDateOffset(context, appWidgetId, 0)
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
            ACTION_EM_REFRESH_WIDGET -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "時間割・行事・メモを更新中...", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        val repo = TimetableRepository.getInstance(context)
                        repo.syncCsvData()
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "更新が完了しました", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("EventMemoWidget", "Refresh error", e)
                    } finally {
                        updateAllWidgets(context)
                        TimetableWidgetProvider.updateAllWidgets(context)
                    }
                }
            }
            ACTION_EM_MIDNIGHT_UPDATE,
            Intent.ACTION_DATE_CHANGED -> {
                // 0:00 リセット: ナビゲーションオフセットをクリアして全ウィジェット更新
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val editor = prefs.edit()
                for (key in prefs.all.keys) {
                    if (key.startsWith("em_offset_")) {
                        editor.remove(key)
                    }
                }
                editor.apply()
                updateAllWidgets(context)
                scheduleAlarms(context)
            }
            ACTION_EM_AFTERNOON_UPDATE,
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
        const val PREFS_NAME = "event_memo_widget_prefs"
        const val ACTION_EM_REFRESH_WIDGET = "com.example.widget.ACTION_REFRESH_EVENT_MEMO_WIDGET"
        const val ACTION_EM_PREV_DAY = "com.example.widget.ACTION_EM_PREV_DAY"
        const val ACTION_EM_NEXT_DAY = "com.example.widget.ACTION_EM_NEXT_DAY"
        const val ACTION_EM_RESET_TODAY = "com.example.widget.ACTION_EM_RESET_TODAY"
        const val ACTION_EM_MIDNIGHT_UPDATE = "com.example.widget.ACTION_EM_MIDNIGHT_UPDATE"
        const val ACTION_EM_AFTERNOON_UPDATE = "com.example.widget.ACTION_EM_AFTERNOON_UPDATE"
        private const val MIDNIGHT_ALARM_REQ_CODE = 2002
        private const val AFTERNOON_ALARM_REQ_CODE = 2003

        fun getWidgetDateOffset(context: Context, appWidgetId: Int): Long {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getLong("em_offset_$appWidgetId", 0L)
        }

        fun setWidgetDateOffset(context: Context, appWidgetId: Int, offset: Long) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong("em_offset_$appWidgetId", offset).apply()
        }

        /**
         * 15:15を過ぎたら明日の日付を基準とし、0:00に今日の日付へリセット
         */
        fun getBaseDate(): LocalDate {
            val now = java.time.LocalTime.now()
            val cutoff = java.time.LocalTime.of(15, 15)
            return if (!now.isBefore(cutoff)) {
                LocalDate.now().plusDays(1)
            } else {
                LocalDate.now()
            }
        }

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
                val componentName = ComponentName(context, EventMemoWidgetProvider::class.java)
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
                val midnightIntent = Intent(context, EventMemoWidgetProvider::class.java).apply {
                    action = ACTION_EM_MIDNIGHT_UPDATE
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
                val afternoonIntent = Intent(context, EventMemoWidgetProvider::class.java).apply {
                    action = ACTION_EM_AFTERNOON_UPDATE
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
                val isSetupCompleted = repo.hasCompletedTermsAndClass()
                val offset = getWidgetDateOffset(context, appWidgetId)
                val targetDate = getBaseDate().plusDays(offset)
                val today = LocalDate.now()

                val views = RemoteViews(context.packageName, R.layout.widget_event_memo)

                // Launch App on clicking root, date, or setup container
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val mainPending = PendingIntent.getActivity(
                    context,
                    appWidgetId * 100 + 14,
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_event_memo_root, mainPending)
                views.setOnClickPendingIntent(R.id.widget_em_title_date, mainPending)
                views.setOnClickPendingIntent(R.id.widget_em_setup_required_container, mainPending)

                if (!isSetupCompleted) {
                    views.setTextViewText(R.id.widget_em_title_date, "-")
                    views.setViewVisibility(R.id.widget_em_setup_required_container, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_em_content_container, View.GONE)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    return
                }

                views.setViewVisibility(R.id.widget_em_setup_required_container, View.GONE)
                views.setViewVisibility(R.id.widget_em_content_container, View.VISIBLE)

                val selectedClass = repo.selectedClass.value
                val schedule = repo.getDaySchedule(selectedClass, targetDate, today)

                // 1. Header: Date format e.g. "今日", "明日", "昨日", or "M/d"
                val dateStr = when (targetDate) {
                    today -> "今日"
                    today.plusDays(1) -> "明日"
                    today.minusDays(1) -> "昨日"
                    else -> "${targetDate.monthValue}/${targetDate.dayOfMonth}"
                }
                views.setTextViewText(R.id.widget_em_title_date, dateStr)

                // Previous Day Button Intent (<)
                val prevIntent = Intent(context, EventMemoWidgetProvider::class.java).apply {
                    action = ACTION_EM_PREV_DAY
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse("widget://eventmemo/$appWidgetId/prev")
                }
                val prevPending = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 100 + 11,
                    prevIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_em_btn_prev, prevPending)

                // Next Day Button Intent (>)
                val nextIntent = Intent(context, EventMemoWidgetProvider::class.java).apply {
                    action = ACTION_EM_NEXT_DAY
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse("widget://eventmemo/$appWidgetId/next")
                }
                val nextPending = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 100 + 12,
                    nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_em_btn_next, nextPending)

                // Home Button Intent (今日・15:15以降は明日にリセット)
                val homeIntent = Intent(context, EventMemoWidgetProvider::class.java).apply {
                    action = ACTION_EM_RESET_TODAY
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse("widget://eventmemo/$appWidgetId/home")
                }
                val homePending = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 100 + 13,
                    homeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_em_btn_home, homePending)

                // 2. Set Event & Memo content
                val eventText = if (schedule.event.isNotBlank()) schedule.event.trim() else "なし"
                views.setTextViewText(R.id.widget_em_event_content, eventText)

                val memoText = if (schedule.memo.isNotBlank()) schedule.memo.trim() else "なし"
                views.setTextViewText(R.id.widget_em_memo_content, memoText)

                // キャッシュが空の場合、バックグラウンドで最新CSVを取得して自動反映
                if (repo.csvSyncManager.events.isEmpty() && !repo.csvSyncManager.syncStatus.value.isSyncing) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            repo.syncCsvData()
                        } catch (_: Exception) {
                        }
                    }
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e("EventMemoWidget", "Error updating widget $appWidgetId", e)
            }
        }
    }
}
