// BeneficiaryListViewModel.kt
package com.example.beneficiaryapp.viewmodels

import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.beneficiaryapp.data.BeneficiaryStatus
import com.example.beneficiaryapp.data.repository.BeneficiaryRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Keep
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

    private val _deleteResult = MutableStateFlow<Result<Unit>?>(null)
    val deleteResult: StateFlow<Result<Unit>?> = _deleteResult.asStateFlow()

    init {
        Log.d("BENEFICIARY_DEBUG", "📱 Release: ViewModel initialized")
        Log.d("BENEFICIARY_DEBUG", "📱 Build Type: RELEASE")

        viewModelScope.launch {
//            // Test Room database first
//            Log.d("BENEFICIARY_DEBUG", "📱 Testing Room database...")
//            val roomWorking = repository.testRoomDatabase()
//            Log.d("BENEFICIARY_DEBUG", "📱 Room test result: $roomWorking")

            // Check database file location
            repository.debugDatabaseLocation()

            // Then load beneficiaries
            loadBeneficiaries()
            observeBeneficiaries()

            // Test direct Firebase connection
            testDirectFirebaseRead()
        }
    }

    private fun observeBeneficiaries() {
        viewModelScope.launch {
            Log.d("BENEFICIARY_DEBUG", "📱 Release: Starting to observe beneficiaries...")

            repository.getBeneficiariesForCurrentUser()
                .onStart {
                    Log.d("BENEFICIARY_DEBUG", "📱 Release: Flow collection started")
                }
                .onEach { entities ->
                    Log.d("BENEFICIARY_DEBUG", "📱 Release: Flow emitted ${entities.size} entities")
                }
                .map { entities ->
                    Log.d("BENEFICIARY_DEBUG", "📱 Release: Processing ${entities.size} entities from Room")

                    if (entities.isEmpty()) {
                        Log.w("BENEFICIARY_DEBUG", "📱 Release: ⚠️ No entities received from Room!")

                        // Check if this is a fresh app install
                        val totalCount = repository.getAllBeneficiariesStatic().size
                        Log.d("BENEFICIARY_DEBUG", "📱 Release: Total in Room (static): $totalCount")
                    } else {
                        entities.forEachIndexed { index, entity ->
                            Log.d("BENEFICIARY_DEBUG",
                                "📱 Release: Entity $index: " +
                                        "ID=${entity.id}, " +
                                        "Name=${entity.name}, " +
                                        "Status=${entity.beneficiaryStatus}, " +
                                        "Created=${entity.createdAt}, " +
                                        "UserID=${entity.userId}"
                            )
                        }
                    }

                    entities.map { entity ->
                        val status = try {
                            BeneficiaryStatus.valueOf(entity.beneficiaryStatus)
                        } catch (e: Exception) {
                            Log.e("BENEFICIARY_DEBUG",
                                "📱 Release: Error parsing status '${entity.beneficiaryStatus}': ${e.message}"
                            )
                            // Try to map common status strings
                            when (entity.beneficiaryStatus.uppercase()) {
                                "PREGNANT" -> BeneficiaryStatus.PREGNANT
                                "LACTATING" -> BeneficiaryStatus.LACTATING
                                else -> {
                                    Log.w("BENEFICIARY_DEBUG", "📱 Release: Unknown status, defaulting to PREGNANT")
                                    BeneficiaryStatus.PREGNANT
                                }
                            }
                        }

                        BeneficiaryUI(
                            id = entity.id,
                            name = entity.name,
                            cnic = entity.cnic,
                            age = entity.age,
                            phoneNumber = entity.phoneNumber,
                            status = status,
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
                                // Try different delimiters
                                when {
                                    entity.childrenData.contains("|") -> entity.childrenData.split("|").size
                                    entity.childrenData.contains(",") -> entity.childrenData.split(",").size
                                    entity.childrenData.contains(";") -> entity.childrenData.split(";").size
                                    else -> 1 // Assume 1 child if data exists but no delimiter
                                }
                            } else 0,
                            imageUrls = if (entity.proofUris.isNotBlank()) {
                                entity.proofUris.split(",").filter { it.isNotBlank() }
                            } else emptyList(),
                            createdAt = entity.createdAt
                        )
                    }
                }
                .combine(searchQuery) { beneficiaries, query ->
                    Log.d("BENEFICIARY_DEBUG", "📱 Release: Filtering with query '$query'")
                    if (query.isBlank()) beneficiaries else {
                        beneficiaries.filter { beneficiary ->
                            beneficiary.name.contains(query, ignoreCase = true) ||
                                    beneficiary.cnic.contains(query, ignoreCase = true) ||
                                    beneficiary.phoneNumber.contains(query, ignoreCase = true)
                        }
                    }
                }
                .combine(filterStatus) { beneficiaries, status ->
                    Log.d("BENEFICIARY_DEBUG", "📱 Release: Filtering with status '$status'")
                    if (status == null) beneficiaries else {
                        beneficiaries.filter { it.status == status }
                    }
                }
                .collect { filteredBeneficiaries ->
                    Log.d("BENEFICIARY_DEBUG",
                        "📱 Release: Final UI list has ${filteredBeneficiaries.size} items"
                    )

                    if (filteredBeneficiaries.isNotEmpty()) {
                        filteredBeneficiaries.forEachIndexed { index, beneficiary ->
                            Log.d("BENEFICIARY_DEBUG",
                                "📱 Release: UI Item $index: " +
                                        "${beneficiary.name} (${beneficiary.id}) - " +
                                        "${beneficiary.status} - ${beneficiary.createdAt}"
                            )
                        }
                        _beneficiaries.value = filteredBeneficiaries.sortedByDescending { it.createdAt }
                    } else {
                        Log.w("BENEFICIARY_DEBUG", "📱 Release: ⚠️ No beneficiaries to display in UI!")
                        _beneficiaries.value = emptyList()
                    }
                }
        }
    }

    private fun getStatusDetails(entity: com.example.beneficiaryapp.data.local.LocalBeneficiaryEntity): String {
        return try {
            when (BeneficiaryStatus.valueOf(entity.beneficiaryStatus)) {
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
                else -> entity.beneficiaryStatus
            }
        } catch (e: Exception) {
            // Handle case where status string doesn't match enum
            entity.beneficiaryStatus
        }
    }

    private fun testDirectFirebaseRead() {
        viewModelScope.launch {
            Log.d("FIREBASE_TEST", "📱 Release: Testing direct Firebase read...")

            try {
                val database = Firebase.database
                val ref = database.getReference("beneficiaries")

                ref.get().addOnSuccessListener { snapshot ->
                    Log.d("FIREBASE_TEST",
                        "📱 Release: Direct Firebase read - Children count: ${snapshot.childrenCount}"
                    )

                    if (snapshot.childrenCount == 0L) {
                        Log.w("FIREBASE_TEST", "📱 Release: Firebase database is empty!")
                    } else {
                        snapshot.children.forEachIndexed { index, child ->
                            try {
                                // FIXED: Use GenericTypeIndicator instead of Map::class.java
                                val mapType = object : GenericTypeIndicator<Map<String, Any>>() {}
                                val data = child.getValue(mapType)
                                Log.d("FIREBASE_TEST",
                                    "📱 Release: Firebase child $index - " +
                                            "Key: ${child.key}, " +
                                            "Name: ${data?.get("name")}"
                                )
                            } catch (e: Exception) {
                                Log.e("FIREBASE_TEST",
                                    "📱 Release: Error reading child ${child.key}: ${e.message}"
                                )
                            }
                        }
                    }
                }.addOnFailureListener { exception ->
                    Log.e("FIREBASE_TEST",
                        "📱 Release: Direct Firebase read failed: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                Log.e("FIREBASE_TEST", "📱 Release: Firebase test exception: ${e.message}", e)
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
            Log.d("SYNC_DEBUG", "📱 Release: Starting sync...")
            _isLoading.value = true
            try {
                repository.syncAllPendingData()
                Log.d("SYNC_DEBUG", "📱 Release: Sync completed")
            } catch (e: Exception) {
                Log.e("SYNC_DEBUG", "📱 Release: Sync error: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteBeneficiary(beneficiaryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _deleteResult.value = null

            try {
                Log.d("DELETE_DEBUG", "📱 Release: Deleting beneficiary $beneficiaryId")
                val result = repository.deleteBeneficiary(beneficiaryId)
                _deleteResult.value = result

                if (result.isSuccess) {
                    // Remove from local state immediately for better UX
                    _beneficiaries.value = _beneficiaries.value.filter { it.id != beneficiaryId }
                    Log.d("DELETE_DEBUG", "📱 Release: Beneficiary removed from UI: $beneficiaryId")
                } else {
                    Log.e("DELETE_DEBUG", "📱 Release: Delete failed for $beneficiaryId")
                }
            } catch (e: Exception) {
                Log.e("DELETE_DEBUG", "📱 Release: Delete exception: ${e.message}", e)
                _deleteResult.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearDeleteResult() {
        _deleteResult.value = null
    }

    fun refreshData() {
        Log.d("REFRESH_DEBUG", "📱 Release: Manual refresh triggered")
        loadBeneficiaries()
    }
}