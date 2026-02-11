package com.example.beneficiaryapp.data.local

import androidx.annotation.Keep
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Keep
@Dao
interface SyncQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncJob(job: SyncQueueEntity)

    // Get all pending jobs (since we don't have status column, get all)
    @Query("SELECT * FROM sync_queue ORDER BY priority DESC, created_at ASC LIMIT :limit")
    suspend fun getPendingJobs(limit: Int = 50): List<SyncQueueEntity>

    // Get specific jobs by data ID and type
    @Query("SELECT * FROM sync_queue WHERE data_id = :dataId AND data_type = :dataType")
    suspend fun getJobsByDataId(dataId: String, dataType: String): List<SyncQueueEntity>

    @Query("DELETE FROM sync_queue WHERE data_id = :dataId AND data_type = :dataType")
    suspend fun deleteJobs(dataId: String, dataType: String)

    @Query("DELETE FROM sync_queue")
    suspend fun deleteAllJobs()

    // Update attempts count
    @Query("UPDATE sync_queue SET attempts = attempts + 1 WHERE job_id = :jobId")
    suspend fun incrementAttempts(jobId: Long)

    // Delete by job ID
    @Query("DELETE FROM sync_queue WHERE job_id = :jobId")
    suspend fun deleteJobById(jobId: Long)

    // Count all pending jobs
    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getPendingJobCount(): Int
}