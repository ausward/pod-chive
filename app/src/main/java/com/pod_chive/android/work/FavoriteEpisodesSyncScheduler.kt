package com.pod_chive.android.work

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context

object FavoriteEpisodesSyncScheduler {

    private const val JOB_ID = 41001

    fun schedule(context: Context) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val componentName = ComponentName(context, FavoriteEpisodesSyncJobService::class.java)

        val jobInfo = JobInfo.Builder(JOB_ID, componentName)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true)
            .setPeriodic(2L)
//            .setPeriodic(60 * 60 * 1000L)
            .build()

        scheduler.schedule(jobInfo)
    }
}
