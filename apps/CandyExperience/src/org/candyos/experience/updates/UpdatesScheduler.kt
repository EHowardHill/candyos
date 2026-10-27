package org.candyos.experience.updates

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context

/**
 * Daily-check scheduling on the framework JobScheduler (no androidx =>
 * no WorkManager in this app). The job is persisted, so it survives
 * reboots without a BOOT_COMPLETED receiver of our own, and requires an
 * unmetered network: release apks are whole-app downloads, so the
 * automatic path stays on Wi-Fi. The manual "Check now" in the Updates
 * screen runs on whatever network is up.
 */
object UpdatesScheduler {

    private const val JOB_ID = 1001
    private const val PERIOD_MS = 24 * 60 * 60 * 1000L // 24h

    fun ensureScheduled(context: Context) {
        val js = context.getSystemService(JobScheduler::class.java) ?: return
        if (js.getPendingJob(JOB_ID) != null) return
        val info = JobInfo.Builder(
            JOB_ID,
            ComponentName(context, UpdateJobService::class.java))
            .setPeriodic(PERIOD_MS)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
            .setPersisted(true)
            .setRequiresBatteryNotLow(true)
            .build()
        js.schedule(info)
    }
}