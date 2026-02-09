package com.example.beneficiaryapp.viewmodels

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.beneficiaryapp.data.BeneficiaryStatus
import com.example.beneficiaryapp.data.ChildData
import com.example.beneficiaryapp.data.repository.BeneficiaryRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class BeneficiaryViewModel @Inject constructor(
    private val beneficiaryRepository: BeneficiaryRepository,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "BeneficiaryViewModel"
        private const val PREF_NAME = "beneficiary_temp"
        private const val KEY_BASIC_INFO = "basic_info"
    }

    // Personal Information
    val name = mutableStateOf("")
    val age = mutableStateOf("")
    val cnic = mutableStateOf("")
    val dateOfBirth = mutableStateOf("")
    val gender = mutableStateOf("")

    // Contact Information
    val phoneNumber = mutableStateOf("")
    val temporaryAddress = mutableStateOf("")
    val permanentAddress = mutableStateOf("")

    // Location Information
    val district = mutableStateOf("")
    val taluka = mutableStateOf("")
    val unionCouncil = mutableStateOf("")

    // Document Information
    val issueDate = mutableStateOf("")
    val expireDate = mutableStateOf("")

    // Beneficiary Status
    val beneficiaryStatus = mutableStateOf<BeneficiaryStatus?>(null)

    // Pregnancy Data
    val pregnancyWeek = mutableStateOf("")
    val gravida = mutableStateOf("")
    val para = mutableStateOf("")

    // Lactating Data
    val deliveryDate = mutableStateOf("")
    val childrenData = mutableStateOf<List<ChildData>>(emptyList())

    // Proof Images
    val proofImages = mutableStateOf<List<String>>(emptyList())

    // Loading states
    val isLoading = mutableStateOf(false)
    val saveResult = mutableStateOf<Result<String>?>(null)

    private val dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy")

    // Format date for display
    fun formatDate(date: LocalDate): String {
        return try {
            date.format(dateFormatter)
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting date: ${e.message}")
            ""
        }
    }

    // Save basic info to SharedPreferences before navigation
    fun saveBasicInfoToStorage() {
        try {
            val basicInfo = mapOf(
                "name" to name.value,
                "age" to age.value,
                "cnic" to cnic.value,
                "dateOfBirth" to dateOfBirth.value,
                "gender" to gender.value,
                "phoneNumber" to phoneNumber.value,
                "temporaryAddress" to temporaryAddress.value,
                "permanentAddress" to permanentAddress.value,
                "district" to district.value,
                "taluka" to taluka.value,
                "unionCouncil" to unionCouncil.value,
                "issueDate" to issueDate.value,
                "expireDate" to expireDate.value,
                "beneficiaryStatus" to beneficiaryStatus.value?.name
            )

            val json = gson.toJson(basicInfo)
            val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            sharedPref.edit().putString(KEY_BASIC_INFO, json).apply()
            Log.d(TAG, "✅ Basic info saved to storage")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving basic info to storage: ${e.message}")
        }
    }

    // Load basic info from SharedPreferences
    fun loadBasicInfoFromStorage() {
        try {
            val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val json = sharedPref.getString(KEY_BASIC_INFO, null)

            if (json != null) {
                val basicInfo = gson.fromJson(json, Map::class.java)
                name.value = basicInfo["name"] as? String ?: ""
                age.value = basicInfo["age"] as? String ?: ""
                cnic.value = basicInfo["cnic"] as? String ?: ""
                dateOfBirth.value = basicInfo["dateOfBirth"] as? String ?: ""
                gender.value = basicInfo["gender"] as? String ?: ""
                phoneNumber.value = basicInfo["phoneNumber"] as? String ?: ""
                temporaryAddress.value = basicInfo["temporaryAddress"] as? String ?: ""
                permanentAddress.value = basicInfo["permanentAddress"] as? String ?: ""
                district.value = basicInfo["district"] as? String ?: ""
                taluka.value = basicInfo["taluka"] as? String ?: ""
                unionCouncil.value = basicInfo["unionCouncil"] as? String ?: ""
                issueDate.value = basicInfo["issueDate"] as? String ?: ""
                expireDate.value = basicInfo["expireDate"] as? String ?: ""

                val status = basicInfo["beneficiaryStatus"] as? String
                beneficiaryStatus.value = when (status) {
                    "PREGNANT" -> BeneficiaryStatus.PREGNANT
                    "LACTATING" -> BeneficiaryStatus.LACTATING
                    else -> null
                }
                Log.d(TAG, "✅ Basic info loaded from storage")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading basic info from storage: ${e.message}")
        }
    }

    // Clear stored basic info
    fun clearStoredBasicInfo() {
        try {
            val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            sharedPref.edit().remove(KEY_BASIC_INFO).apply()
            Log.d(TAG, "✅ Stored basic info cleared")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing stored basic info: ${e.message}")
        }
    }

    // Save complete beneficiary data (all in one step)
    fun saveCompleteBeneficiaryData(isPregnancy: Boolean) {
        viewModelScope.launch {
            isLoading.value = true
            saveResult.value = null

            try {
                Log.d(TAG, "=== Starting complete beneficiary save ===")
                Log.d(TAG, "Is Pregnancy: $isPregnancy")

                // Load basic info from storage first
                loadBasicInfoFromStorage()

                // Log all fields for debugging
                logAllFields()

                // Validate basic information
                if (!isBasicInfoFilled()) {
                    val missingFields = getMissingFields()
                    Log.e(TAG, "Missing fields: $missingFields")
                    saveResult.value = Result.failure(
                        Exception("Please fill all required basic information. Missing: $missingFields")
                    )
                    isLoading.value = false
                    return@launch
                }

                // Validate beneficiary status
                val status = beneficiaryStatus.value ?: run {
                    saveResult.value = Result.failure(Exception("Please select beneficiary status"))
                    isLoading.value = false
                    return@launch
                }

                // Validate mode-specific data
                if (isPregnancy) {
                    if (pregnancyWeek.value.isBlank() || gravida.value.isBlank() || para.value.isBlank()) {
                        saveResult.value = Result.failure(Exception("Please fill all pregnancy details"))
                        isLoading.value = false
                        return@launch
                    }
                } else {
                    if (deliveryDate.value.isBlank()) {
                        saveResult.value = Result.failure(Exception("Please enter delivery date"))
                        isLoading.value = false
                        return@launch
                    }
                }

                Log.d(TAG, "📊 Preparing data for save...")
                Log.d(TAG, "Beneficiary Status: ${status.name}")
                Log.d(TAG, "Is Pregnancy Mode: $isPregnancy")

                // Use repository to save data (offline-first)
                val result = withContext(Dispatchers.IO) {
                    beneficiaryRepository.saveBeneficiaryData(
                        name = name.value.trim(),
                        age = age.value.trim(),
                        cnic = cnic.value.trim(),
                        dateOfBirth = dateOfBirth.value.trim(),
                        gender = gender.value.trim(),
                        phoneNumber = phoneNumber.value.trim(),
                        temporaryAddress = temporaryAddress.value.trim(),
                        permanentAddress = permanentAddress.value.trim(),
                        district = district.value.trim(),
                        taluka = taluka.value.trim(),
                        unionCouncil = unionCouncil.value.trim(),
                        issueDate = issueDate.value.trim(),
                        expireDate = expireDate.value.trim(),
                        beneficiaryStatus = status,
                        pregnancyWeek = if (isPregnancy) pregnancyWeek.value.trim() else "",
                        gravida = if (isPregnancy) gravida.value.trim() else "",
                        para = if (isPregnancy) para.value.trim() else "",
                        deliveryDate = if (!isPregnancy) deliveryDate.value.trim() else "",
                        children = if (!isPregnancy) childrenData.value else emptyList(),
                        proofUris = proofImages.value
                    )
                }

                if (result.isSuccess) {
                    val beneficiaryId = result.getOrThrow()
                    Log.d(TAG, "✅ Data saved successfully! ID: $beneficiaryId")

                    // Clear stored basic info after successful save
                    clearStoredBasicInfo()

                    // Check if online and synced
                    val isOnline = beneficiaryRepository.isNetworkAvailable()
                    if (isOnline) {
                        // Wait a moment for sync to complete
                        delay(1000) // Small delay to allow sync
                        saveResult.value = Result.success("✅ Data saved and synced to cloud!")
                    } else {
                        saveResult.value = Result.success("📱 Data saved locally. Will sync when online.")
                    }

                    // Don't clear form immediately - let user see success message
                    // clearFormData() // Remove this line or call it later

                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "❌ Save failed: ${error?.message}", error)
                    saveResult.value = Result.failure(error ?: Exception("Unknown error"))
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception during save: ${e.message}", e)
                saveResult.value = Result.failure(e)
            } finally {
                isLoading.value = false
            }
        }
    }

    // Log all fields for debugging
    private fun logAllFields() {
        Log.d(TAG, "=== Field Values ===")
        Log.d(TAG, "Name: '${name.value}'")
        Log.d(TAG, "Age: '${age.value}'")
        Log.d(TAG, "CNIC: '${cnic.value}'")
        Log.d(TAG, "Date of Birth: '${dateOfBirth.value}'")
        Log.d(TAG, "Gender: '${gender.value}'")
        Log.d(TAG, "Phone: '${phoneNumber.value}'")
        Log.d(TAG, "Temp Address: '${temporaryAddress.value}'")
        Log.d(TAG, "Perm Address: '${permanentAddress.value}'")
        Log.d(TAG, "District: '${district.value}'")
        Log.d(TAG, "Taluka: '${taluka.value}'")
        Log.d(TAG, "Union Council: '${unionCouncil.value}'")
        Log.d(TAG, "Issue Date: '${issueDate.value}'")
        Log.d(TAG, "Expire Date: '${expireDate.value}'")
        Log.d(TAG, "Beneficiary Status: ${beneficiaryStatus.value?.name}")
        Log.d(TAG, "Pregnancy Week: '${pregnancyWeek.value}'")
        Log.d(TAG, "Gravida: '${gravida.value}'")
        Log.d(TAG, "Para: '${para.value}'")
        Log.d(TAG, "Delivery Date: '${deliveryDate.value}'")
        Log.d(TAG, "Children Count: ${childrenData.value.size}")
        Log.d(TAG, "Proof Images Count: ${proofImages.value.size}")
        Log.d(TAG, "=== End Field Values ===")
    }

    // Get missing fields for error message
    private fun getMissingFields(): String {
        val missing = mutableListOf<String>()
        if (name.value.isBlank()) missing.add("Name")
        if (age.value.isBlank()) missing.add("Age")
        if (cnic.value.isBlank()) missing.add("CNIC")
        if (dateOfBirth.value.isBlank()) missing.add("Date of Birth")
        if (gender.value.isBlank()) missing.add("Gender")
        if (phoneNumber.value.isBlank()) missing.add("Phone Number")
        if (temporaryAddress.value.isBlank()) missing.add("Temporary Address")
        if (permanentAddress.value.isBlank()) missing.add("Permanent Address")
        if (district.value.isBlank()) missing.add("District")
        if (taluka.value.isBlank()) missing.add("Taluka")
        if (unionCouncil.value.isBlank()) missing.add("Union Council")
        if (issueDate.value.isBlank()) missing.add("Issue Date")
        if (expireDate.value.isBlank()) missing.add("Expire Date")
        if (beneficiaryStatus.value == null) missing.add("Beneficiary Status")
        return missing.joinToString(", ")
    }

    // Clear all form data
    fun clearFormData() {
        Log.d(TAG, "🧹 Clearing form data")

        name.value = ""
        age.value = ""
        cnic.value = ""
        dateOfBirth.value = ""
        gender.value = ""
        phoneNumber.value = ""
        temporaryAddress.value = ""
        permanentAddress.value = ""
        district.value = ""
        taluka.value = ""
        unionCouncil.value = ""
        issueDate.value = ""
        expireDate.value = ""
        beneficiaryStatus.value = null
        pregnancyWeek.value = ""
        gravida.value = ""
        para.value = ""
        deliveryDate.value = ""
        childrenData.value = emptyList()
        proofImages.value = emptyList()
        saveResult.value = null

        // Also clear stored data
        clearStoredBasicInfo()

        Log.d(TAG, "✅ Form data cleared")
    }

    // Child management functions
    fun addChild(child: ChildData) {
        val updatedList = childrenData.value.toMutableList()
        updatedList.add(child)
        childrenData.value = updatedList
        Log.d(TAG, "👶 Child added: ${child.name}. Total: ${childrenData.value.size}")
    }

    fun removeChild(index: Int) {
        if (index in childrenData.value.indices) {
            val updatedList = childrenData.value.toMutableList()
            val removed = updatedList.removeAt(index)
            childrenData.value = updatedList
            Log.d(TAG, "🗑️ Child removed: ${removed.name}. Remaining: ${childrenData.value.size}")
        }
    }

    fun updateChild(index: Int, child: ChildData) {
        if (index in childrenData.value.indices) {
            val updatedList = childrenData.value.toMutableList()
            updatedList[index] = child
            childrenData.value = updatedList
            Log.d(TAG, "✏️ Child updated: ${child.name}")
        }
    }

    // Set proof images
    fun setProofImages(images: List<String>) {
        proofImages.value = images
        Log.d(TAG, "📸 ${images.size} proof images set")
    }

    // Set beneficiary status
    fun setBeneficiaryStatus(status: BeneficiaryStatus) {
        beneficiaryStatus.value = status
        Log.d(TAG, "🎯 Status set to: ${status.name}")
    }

    // Validation
    fun isBasicInfoFilled(): Boolean {
        return name.value.isNotBlank() &&
                age.value.isNotBlank() &&
                cnic.value.isNotBlank() &&
                dateOfBirth.value.isNotBlank() &&
                gender.value.isNotBlank() &&
                phoneNumber.value.isNotBlank() &&
                temporaryAddress.value.isNotBlank() &&
                permanentAddress.value.isNotBlank() &&
                district.value.isNotBlank() &&
                taluka.value.isNotBlank() &&
                unionCouncil.value.isNotBlank() &&
                issueDate.value.isNotBlank() &&
                expireDate.value.isNotBlank() &&
                beneficiaryStatus.value != null
    }

    // Sync pending data
    fun syncPendingData() {
        viewModelScope.launch {
            Log.d(TAG, "🔄 Manually triggering sync of pending data...")
            try {
                withContext(Dispatchers.IO) {
                    beneficiaryRepository.syncAllPendingData()
                }
                Log.d(TAG, "✅ Sync initiated")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error initiating sync: ${e.message}")
            }
        }
    }
}