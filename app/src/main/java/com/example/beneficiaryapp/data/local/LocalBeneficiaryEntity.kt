package com.example.beneficiaryapp.data.local

import androidx.room.*

@Entity(tableName = "beneficiaries")
data class LocalBeneficiaryEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    val name: String,
    val age: String,
    val cnic: String,

    @ColumnInfo(name = "date_of_birth")
    val dateOfBirth: String,

    val gender: String,

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "temporary_address")
    val temporaryAddress: String,

    @ColumnInfo(name = "permanent_address")
    val permanentAddress: String,

    val district: String,
    val taluka: String,

    @ColumnInfo(name = "union_council")
    val unionCouncil: String,

    @ColumnInfo(name = "issue_date")
    val issueDate: String,

    @ColumnInfo(name = "expire_date")
    val expireDate: String,

    @ColumnInfo(name = "beneficiary_status")
    val beneficiaryStatus: String,

    @ColumnInfo(name = "pregnancy_week")
    val pregnancyWeek: String = "",

    val gravida: String = "",
    val para: String = "",

    @ColumnInfo(name = "delivery_date")
    val deliveryDate: String = "",

    @ColumnInfo(name = "children_data")
    val childrenData: String = "",

    @ColumnInfo(name = "proof_uris")
    val proofUris: String = "",

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "sync_attempts")
    val syncAttempts: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_sync_attempt")
    val lastSyncAttempt: Long = 0L
)