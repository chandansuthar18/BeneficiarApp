package com.example.beneficiaryapp.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BeneficiaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeneficiary(beneficiary: LocalBeneficiaryEntity)

    @Update
    suspend fun updateBeneficiary(beneficiary: LocalBeneficiaryEntity)

    @Query("DELETE FROM beneficiaries WHERE id = :beneficiaryId")
    suspend fun deleteBeneficiary(beneficiaryId: String)

    @Query("DELETE FROM beneficiaries")
    suspend fun deleteAllBeneficiaries()

    @Query("SELECT * FROM beneficiaries WHERE id = :beneficiaryId")
    suspend fun getBeneficiaryById(beneficiaryId: String): LocalBeneficiaryEntity?

    @Query("SELECT * FROM beneficiaries WHERE user_id = :userId ORDER BY created_at DESC")
    fun getBeneficiariesByUser(userId: String): Flow<List<LocalBeneficiaryEntity>>

    @Query("SELECT * FROM beneficiaries WHERE is_synced = 0 AND sync_attempts < 3")
    suspend fun getUnsyncedBeneficiaries(): List<LocalBeneficiaryEntity>

    @Query("UPDATE beneficiaries SET is_synced = :isSynced WHERE id = :beneficiaryId")
    suspend fun updateSyncStatus(beneficiaryId: String, isSynced: Boolean)

    @Query("UPDATE beneficiaries SET sync_attempts = sync_attempts + 1, last_sync_attempt = :timestamp WHERE id = :beneficiaryId")
    suspend fun incrementSyncAttempt(beneficiaryId: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM beneficiaries WHERE is_synced = 0")
    suspend fun getUnsyncedCount(): Int

    @Query("SELECT * FROM beneficiaries ORDER BY created_at DESC")
    fun getAllBeneficiaries(): Flow<List<LocalBeneficiaryEntity>>
}