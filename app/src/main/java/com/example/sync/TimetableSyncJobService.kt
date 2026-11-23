package com.example.sync

import android.app.job.JobParameters
import android.app.job.JobService
import com.example.data.TimetableRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class TimetableSyncJobService : JobService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<Int, Job>()

    override fun onStartJob(params: JobParameters?): Boolean {
        val jobParams = params ?: return false
        val jobId = jobParams.jobId

        activeJobs.remove(jobId)?.cancel()
        val job = serviceScope.launch {
            var needsReschedule = false
            try {
                val repo = TimetableRepository.getInstance(applicationContext)
                val result = repo.syncCsvData(force = true)
                needsReschedule = result.isFailure
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
                needsReschedule = true
            } finally {
                activeJobs.remove(jobId)
            }
            jobFinished(jobParams, needsReschedule)
        }
        activeJobs[jobId] = job
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        val jobParams = params ?: return true
        activeJobs.remove(jobParams.jobId)?.cancel()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        activeJobs.clear()
        serviceScope.cancel()
    }
}
