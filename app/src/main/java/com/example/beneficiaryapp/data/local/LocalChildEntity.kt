package com.example.beneficiaryapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "children")
data class LocalChildEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "child_id")
    val childId: Long = 0,
    @ColumnInfo(name = "beneficiary_id")
    val beneficiaryId: String,
    val name: String,
    val gender: String,
    @ColumnInfo(name = "proof_uris")
    val proofUris: String = "" // Store as comma-separated string
)