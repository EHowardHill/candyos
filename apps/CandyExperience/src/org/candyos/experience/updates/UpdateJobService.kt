package org.candyos.experience.updates

import android.app.job.JobParameters
import android.app.job.JobService

/**
 * The daily JobService entry point: run one UpdateEngine pass off the main
 * thread and report job completion when the pass finishes. JobScheduler
 * holds a wakelock for the duration of onStartJob + jobFinished, which is
 * all the engine needs.
 */
class UpdateJobService : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        UpdateEngine.checkAsync(this) {
            jobFinished(params, false)
        }
        // Reschedule defensively in case the periodic job got dropped
        // (e.g. after a package reinstall).
        UpdatesScheduler.ensureScheduled(this)
        return true // still running on the engine's executor
    }

    override fun onStopJob(params: JobParameters): Boolean {
        // Let the engine finish its current pass; the next periodic window
        // retries anything that did not complete.
        return false
    }
}