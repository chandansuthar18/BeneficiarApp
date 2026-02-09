package com.example.beneficiaryapp.data.dto

data class ChildDTO(
    val name: String,
    val gender: String,
    val proofUrls: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "gender" to gender,
            "proofUrls" to proofUrls
        )
    }
}