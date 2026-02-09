// BeneficiaryListViewModel.kt
package com.example.beneficiaryapp.viewmodels

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.beneficiaryapp.data.BeneficiaryStatus
import com.example.beneficiaryapp.data.repository.BeneficiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BeneficiaryUI(
    val id: String,
    val name: String,
    val cnic: String,
    val age: String,
    val phoneNumber: String,
    val status: BeneficiaryStatus,
    val statusDetails: String,
    val dateOfBirth: String,
    val gender: String,
    val district: String,
    val taluka: String,
    val unionCouncil: String,
    val temporaryAddress: String,
    val permanentAddress: String,
    val issueDate: String,
    val expireDate: String,
    val pregnancyWeek: String?,
    val gravida: String?,
    val para: String?,
    val deliveryDate: String?,
    val childrenCount: Int,
    val imageUrls: List<String>,
    val createdAt: Long
)

@HiltViewModel
class BeneficiaryListViewModel @Inject constructor(
    private val repository: BeneficiaryRepository
) : ViewModel() {

    private val _beneficiaries = MutableStateFlow<List<BeneficiaryUI>>(emptyList())
    val beneficiaries: StateFlow<List<BeneficiaryUI>> = _beneficiaries.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterStatus = MutableStateFlow<BeneficiaryStatus?>(null)
    val filterStatus: StateFlow<BeneficiaryStatus?> = _filterStatus.asStateFlow()

    init {
        loadBeneficiaries()
        observeBeneficiaries()
    }

    fun clearDeleteResult() {
        _deleteResult.value = null
    }
    private fun observeBeneficiaries() {
        viewModelScope.launch {
            repository.getBeneficiariesForCurrentUser()
                .map { entities ->
                    entities.map { entity ->
                        BeneficiaryUI(
                            id = entity.id,
                            name = entity.name,
                            cnic = entity.cnic,
                            age = entity.age,
                            phoneNumber = entity.phoneNumber,
                            status = BeneficiaryStatus.valueOf(entity.beneficiaryStatus),
                            statusDetails = getStatusDetails(entity),
                            dateOfBirth = entity.dateOfBirth,
                            gender = entity.gender,
                            district = entity.district,
                            taluka = entity.taluka,
                            unionCouncil = entity.unionCouncil,
                            temporaryAddress = entity.temporaryAddress,
                            permanentAddress = entity.permanentAddress,
                            issueDate = entity.issueDate,
                            expireDate = entity.expireDate,
                            pregnancyWeek = entity.pregnancyWeek.takeIf { it.isNotBlank() },
                            gravida = entity.gravida.takeIf { it.isNotBlank() },
                            para = entity.para.takeIf { it.isNotBlank() },
                            deliveryDate = entity.deliveryDate.takeIf { it.isNotBlank() },
                            childrenCount = if (entity.childrenData.isNotBlank()) {
                                entity.childrenData.split("|").size
                            } else 0,
                            imageUrls = if (entity.proofUris.isNotBlank()) {
                                entity.proofUris.split(",").filter { it.isNotBlank() }
                            } else emptyList(),
                            createdAt = entity.createdAt
                        )
                    }
                }
                .combine(searchQuery) { beneficiaries, query ->
                    if (query.isBlank()) beneficiaries else {
                        beneficiaries.filter { beneficiary ->
                            beneficiary.name.contains(query, ignoreCase = true) ||
                                    beneficiary.cnic.contains(query, ignoreCase = true) ||
                                    beneficiary.phoneNumber.contains(query, ignoreCase = true)
                        }
                    }
                }
                .combine(filterStatus) { beneficiaries, status ->
                    if (status == null) beneficiaries else {
                        beneficiaries.filter { it.status == status }
                    }
                }
                .collect { filteredBeneficiaries ->
                    _beneficiaries.value = filteredBeneficiaries.sortedByDescending { it.createdAt }
                }
        }
    }

    private fun getStatusDetails(entity: com.example.beneficiaryapp.data.local.LocalBeneficiaryEntity): String {
        return when (BeneficiaryStatus.valueOf(entity.beneficiaryStatus)) {
            BeneficiaryStatus.PREGNANT -> {
                if (entity.pregnancyWeek.isNotBlank()) {
                    "${entity.pregnancyWeek} weeks | G${entity.gravida}P${entity.para}"
                } else "Pregnant"
            }
            BeneficiaryStatus.LACTATING -> {
                if (entity.deliveryDate.isNotBlank()) {
                    "Delivery: ${entity.deliveryDate}"
                } else "Lactating"
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterStatus(status: BeneficiaryStatus?) {
        _filterStatus.value = status
    }

    fun loadBeneficiaries() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.syncAllPendingData()
            } catch (e: Exception) {
                // Handle error silently
            } finally {
                _isLoading.value = false
            }
        }
    }
    // In BeneficiaryListViewModel.kt, update the delete function:
    private val _deleteResult = MutableStateFlow<Result<Unit>?>(null)
    val deleteResult: StateFlow<Result<Unit>?> = _deleteResult.asStateFlow()

    fun deleteBeneficiary(beneficiaryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _deleteResult.value = null

            try {
                val result = repository.deleteBeneficiary(beneficiaryId)
                _deleteResult.value = result

                if (result.isSuccess) {
                    // Remove from local state immediately for better UX
                    _beneficiaries.value = _beneficiaries.value.filter { it.id != beneficiaryId }
                    Log.d(TAG, "✅ Beneficiary removed from UI: $beneficiaryId")
                }
            } catch (e: Exception) {
                _deleteResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun refreshData() {
        loadBeneficiaries()
    }
}