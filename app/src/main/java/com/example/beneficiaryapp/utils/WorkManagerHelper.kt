package com.example.beneficiaryapp.utils

import android.content.Context
import androidx.work.*
import com.example.beneficiaryapp.work.SyncWorker
import java.util.concurrent.TimeUnit

object WorkManagerHelper {

    fun scheduleSyncWorker(context: Context) {
        val syncConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(syncConstraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "beneficiary_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}