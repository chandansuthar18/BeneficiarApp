package com.example.beneficiaryapp.data

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "lactating_data")
data class LactatingData(
    @PrimaryKey(autoGenerate = true)
    val lactatingId: Int = 0,
    val beneficiaryId: String = "",
    val deliveryDate: String = "",
    val proofUris: List<String> = emptyList(),
    val isSynced: Boolean = false
)