package com.example.beneficiaryapp.data

import androidx.annotation.Keep

@Keep
data class ChildData(
    var name: String = "",
    var gender: String = "",
    var proofUris: List<String> = emptyList()
)