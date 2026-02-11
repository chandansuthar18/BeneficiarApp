package com.example.beneficiaryapp.data.local

import androidx.annotation.Keep
import androidx.room.*

@Dao
@Keep
interface ChildDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChildren(children: List<LocalChildEntity>)

    @Query("SELECT * FROM children WHERE beneficiary_id = :beneficiaryId")
    suspend fun getChildrenByBeneficiary(beneficiaryId: String): List<LocalChildEntity>

    @Query("DELETE FROM children WHERE beneficiary_id = :beneficiaryId")
    suspend fun deleteChildrenByBeneficiary(beneficiaryId: String)

    @Query("DELETE FROM children")
    suspend fun deleteAllChildren()
}