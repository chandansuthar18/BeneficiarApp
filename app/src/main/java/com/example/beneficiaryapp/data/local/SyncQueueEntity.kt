package com.example.beneficiaryapp.data.local

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Keep
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "job_id")
    val jobId: Long = 0,
    val operation: String, // CREATE, UPDATE, DELETE
    @ColumnInfo(name = "data_type")
    val dataType: String, // BENEFICIARY, etc.
    @ColumnInfo(name = "data_id")
    val dataId: String,
    @ColumnInfo(name = "data_json")
    val dataJson: String,
    val priority: Int = 1,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0  // Changed from retryCount to attempts
)