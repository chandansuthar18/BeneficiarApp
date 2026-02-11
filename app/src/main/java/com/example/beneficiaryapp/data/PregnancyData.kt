package com.example.beneficiaryapp.data

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "pregnancy_data")
data class PregnancyData(
    @PrimaryKey(autoGenerate = true)
    val pregnancyId: Int = 0,
    val beneficiaryId: String = "",
    val pregnancyWeek: String = "",
    val gravida: String = "",
    val para: String = "",
    val proofUris: List<String> = emptyList(),
    val isSynced: Boolean = false
)