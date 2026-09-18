package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.TimetableRepository
import java.time.LocalDate

class NextClassNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repo = TimetableRepository.getInstance(context)

        // 次の授業通知が無効の場合はアラームを解除して終了
        if (!repo.isNextClassNotificationEnabled.value) {
            NotificationHelper.cancelNextClassAlarms(context)
            return
        }

        val action = intent.action
        if (action == NotificationHelper.ACTION_NEXT_CLASS_NOTIFICATION) {
            val period = intent.getIntExtra(NotificationHelper.EXTRA_PERIOD, 1)
            val dateEpochDay = intent.getLongExtra(
                NotificationHelper.EXTRA_DATE_EPOCH_DAY,
                LocalDate.now().toEpochDay()
            )
            val date = LocalDate.ofEpochDay(dateEpochDay)

            // 当日分のみ通知（休日はNotificationHelper内で除外）
            if (date == LocalDate.now()) {
                NotificationHelper.showNextClassNotification(context, period, date)
            }
        }

        // 以降の限や翌日のアラームを確実にセット
        NotificationHelper.scheduleNextClassAlarms(context)
    }
}
