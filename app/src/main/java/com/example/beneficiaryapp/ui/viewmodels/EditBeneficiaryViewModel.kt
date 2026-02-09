package com.example.beneficiaryapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.beneficiaryapp.data.BeneficiaryStatus
import com.example.beneficiaryapp.data.repository.BeneficiaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditBeneficiaryViewModel @Inject constructor(
    private val repository: BeneficiaryRepository
) : ViewModel() {

    // Define UI model
    data class BeneficiaryUI(
        val id: String,
        var name: String = "",
        var age: String = "",
        var cnic: String = "",
        val dateOfBirth: String = "",
        val gender: String = "",
        var phoneNumber: String = "",
        var temporaryAddress: String = "",
        var permanentAddress: String = "",
        val district: String = "",
        val taluka: String = "",
        val unionCouncil: String = "",
        var issueDate: String = "",
        var expireDate: String = "",
        val status: BeneficiaryStatus = BeneficiaryStatus.PREGNANT,
        val pregnancyWeek: Int? = null,
        val gravida: Int? = null,
        val para: Int? = null,
        val deliveryDate: String? = null,
        val statusDetails: String = "",
        val childrenCount: Int = 0,
        val imageUrls: List<String> = emptyList()
    )

    private val _beneficiaryState = MutableStateFlow<BeneficiaryUI?>(null)
    val beneficiaryState: StateFlow<BeneficiaryUI?> = _beneficiaryState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isDeleted = MutableStateFlow(false)
    val isDeleted: StateFlow<Boolean> = _isDeleted.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun loadBeneficiary(beneficiaryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val beneficiary = repository.getBeneficiaryById(beneficiaryId)
                if (beneficiary != null) {
                    _beneficiaryState.value = beneficiary.toUI()
                } else {
                    _errorMessage.value = "Beneficiary not found"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load beneficiary: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Extension function to convert LocalBeneficiaryEntity to BeneficiaryUI
    private fun Any.toUI(): EditBeneficiaryViewModel.BeneficiaryUI {
        return try {
            // This is a placeholder - you need to replace with your actual entity class
            // Assuming your entity has these properties
            val entity = this
            EditBeneficiaryViewModel.BeneficiaryUI(
                id = entity::class.java.getMethod("getId").invoke(entity) as? String ?: "",
                name = entity::class.java.getMethod("getName").invoke(entity) as? String ?: "",
                age = entity::class.java.getMethod("getAge").invoke(entity) as? String ?: "",
                cnic = entity::class.java.getMethod("getCnic").invoke(entity) as? String ?: "",
                dateOfBirth = entity::class.java.getMethod("getDateOfBirth").invoke(entity) as? String ?: "",
                gender = entity::class.java.getMethod("getGender").invoke(entity) as? String ?: "",
                phoneNumber = entity::class.java.getMethod("getPhoneNumber").invoke(entity) as? String ?: "",
                temporaryAddress = entity::class.java.getMethod("getTemporaryAddress").invoke(entity) as? String ?: "",
                permanentAddress = entity::class.java.getMethod("getPermanentAddress").invoke(entity) as? String ?: "",
                district = entity::class.java.getMethod("getDistrict").invoke(entity) as? String ?: "",
                taluka = entity::class.java.getMethod("getTaluka").invoke(entity) as? String ?: "",
                unionCouncil = entity::class.java.getMethod("getUnionCouncil").invoke(entity) as? String ?: "",
                issueDate = entity::class.java.getMethod("getIssueDate").invoke(entity) as? String ?: "",
                expireDate = entity::class.java.getMethod("getExpireDate").invoke(entity) as? String ?: "",
                status = try {
                    BeneficiaryStatus.valueOf(
                        entity::class.java.getMethod("getBeneficiaryStatus").invoke(entity) as? String ?: "PREGNANT"
                    )
                } catch (e: Exception) {
                    BeneficiaryStatus.PREGNANT
                },
                pregnancyWeek = entity::class.java.getMethod("getPregnancyWeek").invoke(entity) as? Int,
                gravida = entity::class.java.getMethod("getGravida").invoke(entity) as? Int,
                para = entity::class.java.getMethod("getPara").invoke(entity) as? Int,
                deliveryDate = entity::class.java.getMethod("getDeliveryDate").invoke(entity) as? String,
                imageUrls = parseImageUris(entity::class.java.getMethod("getProofUris").invoke(entity) as? String ?: "")
            )
        } catch (e: Exception) {
            // Fallback to empty UI
            EditBeneficiaryViewModel.BeneficiaryUI(id = "")
        }
    }

    private fun parseImageUris(proofUris: String): List<String> {
        return if (proofUris.isNotBlank()) {
            proofUris.split(",").filter { uri -> uri.isNotBlank() }
        } else {
            emptyList()
        }
    }

    // Update methods
    fun updateName(name: String) {
        _beneficiaryState.value = _beneficiaryState.value?.copy(name = name)
    }

    fun updateAge(age: String) {
        _beneficiaryState.value = _beneficiaryState.value?.copy(age = age)
    }

    fun updateCNIC(cnic: String) {
        _beneficiaryState.value = _beneficiaryState.value?.copy(cnic = cnic)
    }

    fun updatePhoneNumber(phone: String) {
        _beneficiaryState.value = _beneficiaryState.value?.copy(phoneNumber = phone)
    }

    fun updateTemporaryAddress(address: String) {
        _beneficiaryState.value = _beneficiaryState.value?.copy(temporaryAddress = address)
    }

    fun updatePermanentAddress(address: String) {
        _beneficiaryState.value = _beneficiaryState.value?.copy(permanentAddress = address)
    }

    fun updateIssueDate(date: String) {
        _beneficiaryState.value = _beneficiaryState.value?.copy(issueDate = date)
    }

    fun updateExpireDate(date: String) {
        _beneficiaryState.value = _beneficiaryState.value?.copy(expireDate = date)
    }

    fun saveChanges() {
        val beneficiary = _beneficiaryState.value ?: return

        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                // Update beneficiary in repository
                val result = repository.updateBeneficiary(
                    id = beneficiary.id,
                    name = beneficiary.name,
                    age = beneficiary.age,
                    cnic = beneficiary.cnic,
                    phoneNumber = beneficiary.phoneNumber,
                    temporaryAddress = beneficiary.temporaryAddress,
                    permanentAddress = beneficiary.permanentAddress,
                    issueDate = beneficiary.issueDate,
                    expireDate = beneficiary.expireDate
                )

                if (result.isSuccess) {
                    _successMessage.value = "Beneficiary updated successfully!"
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Update failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteBeneficiary() {
        val beneficiaryId = _beneficiaryState.value?.id ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.deleteBeneficiary(beneficiaryId)
                if (result.isSuccess) {
                    _isDeleted.value = true
                    _successMessage.value = "Beneficiary deleted successfully!"
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Delete failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}