package com.example.beneficiaryapp.data.dto

import androidx.annotation.Keep
import java.util.*

@Keep
data class BeneficiaryDTO(
    val id: String,
    val userId: String,
    val name: String,
    val age: String,
    val cnic: String,
    val dateOfBirth: String,
    val gender: String,
    val phoneNumber: String,
    val temporaryAddress: String,
    val permanentAddress: String,
    val district: String,
    val taluka: String,
    val unionCouncil: String,
    val issueDate: String,
    val expireDate: String,
    val beneficiaryStatus: String,
    val pregnancyWeek: String = "",
    val gravida: String = "",
    val para: String = "",
    val deliveryDate: String = "",
    val children: List<ChildDTO> = emptyList(),
    val imageUrls: List<String> = emptyList(),
    val timestamp: Long,
    val createdAt: String,
    val isSynced: Boolean = true,
    val syncAttempts: Int = 0
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "name" to name,
            "age" to age,
            "cnic" to cnic,
            "dateOfBirth" to dateOfBirth,
            "gender" to gender,
            "phoneNumber" to phoneNumber,
            "temporaryAddress" to temporaryAddress,
            "permanentAddress" to permanentAddress,
            "district" to district,
            "taluka" to taluka,
            "unionCouncil" to unionCouncil,
            "issueDate" to issueDate,
            "expireDate" to expireDate,
            "beneficiaryStatus" to beneficiaryStatus,
            "pregnancyWeek" to pregnancyWeek,
            "gravida" to gravida,
            "para" to para,
            "deliveryDate" to deliveryDate,
            "children" to children.map { it.toMap() },
            "imageUrls" to imageUrls,
            "timestamp" to timestamp,
            "createdAt" to createdAt,
            "isSynced" to isSynced,
            "syncAttempts" to syncAttempts
        )
    }
}