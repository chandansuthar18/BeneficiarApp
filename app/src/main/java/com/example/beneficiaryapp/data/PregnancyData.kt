package com.example.beneficiaryapp.data

import androidx.room.PrimaryKey

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