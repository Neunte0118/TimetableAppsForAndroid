package com.example.sync

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import java.util.concurrent.TimeUnit

object BackgroundSyncScheduler {
    const val SYNC_JOB_ID = 4100
    const val SYNC_INTERVAL_MINUTES = 60L
    val SYNC_INTERVAL_MILLIS: Long = TimeUnit.MINUTES.toMillis(SYNC_INTERVAL_MINUTES)

    /**
     * Schedules a periodic background sync every 60 minutes when network connectivity is available.
     * Avoids rescheduling if an identical periodic job is already pending so the current cadence is preserved.
     */
    fun schedulePeriodicSync(context: Context, forceReschedule: Boolean = false): Boolean {
        return try {
            val appContext = context.applicationContext
            val scheduler = appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
                ?: return false

            if (!forceReschedule) {
                val existing = scheduler.getPendingJob(SYNC_JOB_ID)
                if (existing != null && existing.intervalMillis == SYNC_INTERVAL_MILLIS) {
                    return true
                }
            }

            val componentName = ComponentName(appContext, TimetableSyncJobService::class.java)
            val jobInfo = JobInfo.Builder(SYNC_JOB_ID, componentName)
                .setPeriodic(SYNC_INTERVAL_MILLIS)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .build()

            scheduler.schedule(jobInfo) == JobScheduler.RESULT_SUCCESS
        } catch (_: Throwable) {
            false
        }
    }

    fun isPeriodicSyncScheduled(context: Context): Boolean {
        return try {
            val scheduler = context.applicationContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
                ?: return false
            scheduler.getPendingJob(SYNC_JOB_ID) != null
        } catch (_: Throwable) {
            false
        }
    }

    fun cancelPeriodicSync(context: Context) {
        try {
            val scheduler = context.applicationContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
            scheduler?.cancel(SYNC_JOB_ID)
        } catch (_: Throwable) {
        }
    }
}
