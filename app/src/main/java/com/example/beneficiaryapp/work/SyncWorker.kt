package com.example.beneficiaryapp.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.beneficiaryapp.data.repository.BeneficiaryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: BeneficiaryRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("🔄 Starting background sync...")

            // Sync all pending data
            repository.syncAllPendingData()

            Timber.d("✅ Background sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "❌ Background sync failed")
            Result.retry()
        }
    }
}