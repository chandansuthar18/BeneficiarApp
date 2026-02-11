package com.example.beneficiaryapp.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.beneficiaryapp.data.BeneficiaryStatus
import com.example.beneficiaryapp.data.ChildData
import com.example.beneficiaryapp.data.dto.BeneficiaryDTO
import com.example.beneficiaryapp.data.dto.ChildDTO
import com.example.beneficiaryapp.data.local.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BeneficiaryRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val context: Context,
    private val gson: Gson,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseDatabase: FirebaseDatabase
) {
    private val TAG = "BeneficiaryRepository"

    private val beneficiaryDao: BeneficiaryDao = appDatabase.beneficiaryDao()
    private val childDao: ChildDao = appDatabase.childDao()
    private val syncQueueDao: SyncQueueDao = appDatabase.syncQueueDao()

    // Check network connectivity
    fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (e: Exception) {
            Log.e(TAG, "Network check error: ${e.message}")
            false
        }
    }

    // Save beneficiary data (offline-first)
    suspend fun saveBeneficiaryData(
        name: String,
        age: String,
        cnic: String,
        dateOfBirth: String,
        gender: String,
        phoneNumber: String,
        temporaryAddress: String,
        permanentAddress: String,
        district: String,
        taluka: String,
        unionCouncil: String,
        issueDate: String,
        expireDate: String,
        beneficiaryStatus: BeneficiaryStatus,
        pregnancyWeek: String = "",
        gravida: String = "",
        para: String = "",
        deliveryDate: String = "",
        children: List<ChildData> = emptyList(),
        proofUris: List<String> = emptyList()
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "RELEASE: 📝 Starting saveBeneficiaryData...")

                // Generate unique ID
                val beneficiaryId = generateBeneficiaryId()
                Log.d(TAG, "RELEASE: Generated beneficiary ID: $beneficiaryId")

                // Convert children to JSON
                val childrenJson = if (children.isNotEmpty()) {
                    gson.toJson(children)
                } else ""

                // Convert proof URIs to comma-separated string
                val proofUrisString = if (proofUris.isNotEmpty()) {
                    proofUris.joinToString(",")
                } else ""

                // Create local entity
                val localBeneficiary = LocalBeneficiaryEntity(
                    id = beneficiaryId,
                    userId = "ALL_USERS", // Use a constant
                    name = name,
                    age = age,
                    cnic = cnic,
                    dateOfBirth = dateOfBirth,
                    gender = gender,
                    phoneNumber = phoneNumber,
                    temporaryAddress = temporaryAddress,
                    permanentAddress = permanentAddress,
                    district = district,
                    taluka = taluka,
                    unionCouncil = unionCouncil,
                    issueDate = issueDate,
                    expireDate = expireDate,
                    beneficiaryStatus = beneficiaryStatus.name,
                    pregnancyWeek = pregnancyWeek,
                    gravida = gravida,
                    para = para,
                    deliveryDate = deliveryDate,
                    childrenData = childrenJson,
                    proofUris = proofUrisString,
                    isSynced = false,
                    createdAt = System.currentTimeMillis()
                )

                // Save to local database
                beneficiaryDao.insertBeneficiary(localBeneficiary)
                Log.d(TAG, "RELEASE: ✅ Saved to local database: $beneficiaryId")

                // VERIFY it was saved
                val savedBeneficiary = beneficiaryDao.getBeneficiaryById(beneficiaryId)
                if (savedBeneficiary == null) {
                    Log.e(TAG, "RELEASE: ❌ ERROR: Beneficiary NOT FOUND after save!")
                } else {
                    Log.d(TAG, "RELEASE: ✅ Verified: Found beneficiary ${savedBeneficiary.id} in Room")
                }

                // Save children data if any
                if (children.isNotEmpty()) {
                    val childEntities = children.map { child ->
                        LocalChildEntity(
                            beneficiaryId = beneficiaryId,
                            name = child.name,
                            gender = child.gender,
                            proofUris = child.proofUris.joinToString(",")
                        )
                    }
                    childDao.insertChildren(childEntities)
                    Log.d(TAG, "RELEASE: ✅ Saved ${children.size} children for beneficiary: $beneficiaryId")
                }

                // Try to sync immediately if online
                if (isNetworkAvailable()) {
                    Log.d(TAG, "RELEASE: 🌐 Network available, attempting sync...")
                    try {
                        syncToFirebase(beneficiaryId)
                    } catch (e: Exception) {
                        Log.e(TAG, "RELEASE: ❌ Immediate sync failed: ${e.message}")
                        // Add to sync queue
                        addToSyncQueue(localBeneficiary, "CREATE")
                    }
                } else {
                    // Add to sync queue for later
                    addToSyncQueue(localBeneficiary, "CREATE")
                    Log.d(TAG, "RELEASE: 📱 Added to sync queue (offline). ID: $beneficiaryId")
                }

                Result.success(beneficiaryId)
            } catch (e: Exception) {
                Log.e(TAG, "RELEASE: ❌ Error saving beneficiary: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Sync to Firebase
    private suspend fun syncToFirebase(beneficiaryId: String) {
        try {
            Log.d(TAG, "RELEASE: 🔄 Syncing beneficiary: $beneficiaryId")

            // Get beneficiary from local DB
            val beneficiary = beneficiaryDao.getBeneficiaryById(beneficiaryId)
            if (beneficiary == null) {
                Log.e(TAG, "RELEASE: Beneficiary not found in local DB: $beneficiaryId")
                return
            }

            // Get children data
            val children = childDao.getChildrenByBeneficiary(beneficiaryId)
            val childDTOs = children.map { child ->
                ChildDTO(
                    name = child.name,
                    gender = child.gender,
                    proofUrls = if (child.proofUris.isNotBlank()) {
                        child.proofUris.split(",").filter { it.isNotBlank() }
                    } else emptyList()
                )
            }

            // Create DTO
            val beneficiaryDTO = BeneficiaryDTO(
                id = beneficiary.id,
                userId = beneficiary.userId,
                name = beneficiary.name,
                age = beneficiary.age,
                cnic = beneficiary.cnic,
                dateOfBirth = beneficiary.dateOfBirth,
                gender = beneficiary.gender,
                phoneNumber = beneficiary.phoneNumber,
                temporaryAddress = beneficiary.temporaryAddress,
                permanentAddress = beneficiary.permanentAddress,
                district = beneficiary.district,
                taluka = beneficiary.taluka,
                unionCouncil = beneficiary.unionCouncil,
                issueDate = beneficiary.issueDate,
                expireDate = beneficiary.expireDate,
                beneficiaryStatus = beneficiary.beneficiaryStatus,
                pregnancyWeek = beneficiary.pregnancyWeek,
                gravida = beneficiary.gravida,
                para = beneficiary.para,
                deliveryDate = beneficiary.deliveryDate,
                children = childDTOs,
                imageUrls = if (beneficiary.proofUris.isNotBlank()) {
                    beneficiary.proofUris.split(",").filter { it.isNotBlank() }
                } else emptyList(),
                timestamp = beneficiary.createdAt,
                createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date(beneficiary.createdAt))
            )

            // Upload to Firebase under "beneficiaries" collection
            val databaseRef = firebaseDatabase.reference
            databaseRef.child("beneficiaries").child(beneficiaryId).setValue(beneficiaryDTO.toMap()).await()
            Log.d(TAG, "RELEASE: ✅ Uploaded to Firebase: beneficiaries/$beneficiaryId")

            // Mark as synced
            beneficiaryDao.updateSyncStatus(beneficiaryId, true)
            Log.d(TAG, "RELEASE: ✅ Marked as synced in local DB: $beneficiaryId")

            // Remove from sync queue
            syncQueueDao.deleteJobs(beneficiaryId, "BENEFICIARY")
            Log.d(TAG, "RELEASE: ✅ Removed from sync queue: $beneficiaryId")

        } catch (e: Exception) {
            Log.e(TAG, "RELEASE: ❌ Failed to sync beneficiary: $beneficiaryId", e)
            beneficiaryDao.incrementSyncAttempt(beneficiaryId)
            throw e
        }
    }

    // Add to sync queue helper
    private suspend fun addToSyncQueue(beneficiary: LocalBeneficiaryEntity, operation: String) {
        val beneficiaryJson = gson.toJson(beneficiary)
        syncQueueDao.insertSyncJob(
            SyncQueueEntity(
                operation = operation,
                dataType = "BENEFICIARY",
                dataId = beneficiary.id,
                dataJson = beneficiaryJson,
                priority = 1,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    // Get ALL beneficiaries - Shows all data
    fun getBeneficiariesForCurrentUser(): Flow<List<LocalBeneficiaryEntity>> {
        Log.d(TAG, "RELEASE: Getting ALL beneficiaries (not filtering by user)")

        // Return ALL beneficiaries without user filter
        return beneficiaryDao.getAllBeneficiaries()
            .map { entities ->
                Log.d(TAG, "RELEASE: Room returned ${entities.size} total entities")
                if (entities.isEmpty()) {
                    Log.d(TAG, "RELEASE: Room database is EMPTY!")
                } else {
                    entities.forEachIndexed { index, entity ->
                        Log.d(TAG, "RELEASE: Entity $index - ID: ${entity.id}, Name: ${entity.name}")
                    }
                }
                entities
            }
    }

    // Get all beneficiaries (alias for same function)
    fun getAllBeneficiaries(): Flow<List<LocalBeneficiaryEntity>> {
        return beneficiaryDao.getAllBeneficiaries()
    }

    // Static method for debugging
    suspend fun getAllBeneficiariesStatic(): List<LocalBeneficiaryEntity> {
        return beneficiaryDao.getAllBeneficiariesStatic()
    }

    // Get beneficiary by ID
    suspend fun getBeneficiaryById(beneficiaryId: String): LocalBeneficiaryEntity? {
        return beneficiaryDao.getBeneficiaryById(beneficiaryId)
    }

    // Get children by beneficiary ID
    suspend fun getChildrenByBeneficiary(beneficiaryId: String): List<LocalChildEntity> {
        return childDao.getChildrenByBeneficiary(beneficiaryId)
    }

    // Sync all pending data
    suspend fun syncAllPendingData() {
        Log.d(TAG, "RELEASE: 🔄 Starting syncAllPendingData...")

        if (!isNetworkAvailable()) {
            Log.d(TAG, "RELEASE: No network available for sync")
            return
        }

        // Get unsynced beneficiaries
        val unsyncedBeneficiaries = beneficiaryDao.getUnsyncedBeneficiaries()
        Log.d(TAG, "RELEASE: Found ${unsyncedBeneficiaries.size} unsynced beneficiaries")

        for (beneficiary in unsyncedBeneficiaries) {
            Log.d(TAG, "RELEASE: Syncing beneficiary: ${beneficiary.id} - ${beneficiary.name}")
            try {
                syncToFirebase(beneficiary.id)
            } catch (e: Exception) {
                Log.e(TAG, "RELEASE: Failed to sync ${beneficiary.id}: ${e.message}")
            }
        }

        // Process sync queue
        val pendingJobs = syncQueueDao.getPendingJobs(50)
        Log.d(TAG, "RELEASE: Found ${pendingJobs.size} pending jobs in queue")

        for (job in pendingJobs) {
            try {
                when (job.operation) {
                    "CREATE" -> {
                        if (job.dataType == "BENEFICIARY") {
                            try {
                                val beneficiary = gson.fromJson(job.dataJson, LocalBeneficiaryEntity::class.java)
                                beneficiaryDao.insertBeneficiary(beneficiary)
                                syncToFirebase(job.dataId)
                                syncQueueDao.deleteJobById(job.jobId)
                                Log.d(TAG, "RELEASE: Processed CREATE job for ${job.dataId}")
                            } catch (e: Exception) {
                                Log.e(TAG, "RELEASE: Error parsing beneficiary from queue: ${e.message}")
                                syncQueueDao.incrementAttempts(job.jobId)
                            }
                        }
                    }
                    "DELETE" -> {
                        if (job.dataType == "BENEFICIARY") {
                            try {
                                deleteFromFirebase(job.dataId)
                                syncQueueDao.deleteJobById(job.jobId)
                                Log.d(TAG, "RELEASE: Processed DELETE job for ${job.dataId}")
                            } catch (e: Exception) {
                                Log.e(TAG, "RELEASE: Error deleting from Firebase: ${e.message}")
                                syncQueueDao.incrementAttempts(job.jobId)
                            }
                        }
                    }
                    "UPDATE" -> {
                        if (job.dataType == "BENEFICIARY") {
                            try {
                                val beneficiary = gson.fromJson(job.dataJson, LocalBeneficiaryEntity::class.java)
                                beneficiaryDao.updateBeneficiary(beneficiary)
                                syncToFirebase(job.dataId)
                                syncQueueDao.deleteJobById(job.jobId)
                                Log.d(TAG, "RELEASE: Processed UPDATE job for ${job.dataId}")
                            } catch (e: Exception) {
                                Log.e(TAG, "RELEASE: Error updating beneficiary from queue: ${e.message}")
                                syncQueueDao.incrementAttempts(job.jobId)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "RELEASE: Error processing sync job ${job.jobId}: ${e.message}")
                syncQueueDao.incrementAttempts(job.jobId)
            }
        }

        Log.d(TAG, "RELEASE: ✅ Sync completed")
    }

    // TEST METHODS FOR DEBUGGING
//    suspend fun testRoomDatabase(): Boolean {
//        return withContext(Dispatchers.IO) {
//            try {
//                Log.d(TAG, "RELEASE: Testing Room database...")
//
//                // Try to insert a test record
//                val testEntity = LocalBeneficiaryEntity(
//                    id = "TEST-RELEASE-123",
//                    userId = "TEST_USER",
//                    name = "Test User Release",
//                    age = "25",
//                    cnic = "1234567890123",
//                    dateOfBirth = "1998-01-01",
//                    gender = "Male",
//                    phoneNumber = "03001234567",
//                    temporaryAddress = "Test Address",
//                    permanentAddress = "Test Permanent",
//                    district = "Test District",
//                    taluka = "Test Taluka",
//                    unionCouncil = "Test UC",
//                    issueDate = "2024-01-01",
//                    expireDate = "2024-12-31",
//                    beneficiaryStatus = "PREGNANT",
//                    createdAt = System.currentTimeMillis()
//                )
//
//                beneficiaryDao.insertBeneficiary(testEntity)
//                Log.d(TAG, "RELEASE: ✅ Test record inserted")
//
//                // Try to retrieve it
//                val retrieved = beneficiaryDao.getBeneficiaryById("TEST-RELEASE-123")
//                if (retrieved != null) {
//                    Log.d(TAG, "RELEASE: ✅ Test record retrieved: ${retrieved.name}")
//                    true
//                } else {
//                    Log.e(TAG, "RELEASE: ❌ Test record NOT FOUND!")
//                    false
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "RELEASE: Room test failed: ${e.message}", e)
////                false
//            }
//        }
//    }

    fun debugDatabaseLocation() {
        val databasePath = context.getDatabasePath("beneficiary_database").absolutePath
        Log.d(TAG, "RELEASE: Database location: $databasePath")

        val databaseFile = File(databasePath)
        Log.d(TAG, "RELEASE: Database exists: ${databaseFile.exists()}")
        Log.d(TAG, "RELEASE: Database size: ${databaseFile.length()} bytes")

        // List all database files
        val dbDir = File(databasePath).parentFile
        if (dbDir.exists() && dbDir.isDirectory) {
            val dbFiles = dbDir.listFiles { file -> file.name.contains("beneficiary_database") }
            Log.d(TAG, "RELEASE: Found ${dbFiles?.size ?: 0} database files:")
            dbFiles?.forEach { file ->
                Log.d(TAG, "RELEASE:   - ${file.name} (${file.length()} bytes)")
            }
        }
    }

    // Update beneficiary
    suspend fun updateBeneficiary(
        id: String,
        name: String,
        age: String,
        cnic: String,
        phoneNumber: String,
        temporaryAddress: String,
        permanentAddress: String,
        issueDate: String,
        expireDate: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Get existing beneficiary
                val existing = beneficiaryDao.getBeneficiaryById(id)
                if (existing == null) {
                    return@withContext Result.failure(Exception("Beneficiary not found"))
                }

                // Update entity
                val updated = existing.copy(
                    name = name,
                    age = age,
                    cnic = cnic,
                    phoneNumber = phoneNumber,
                    temporaryAddress = temporaryAddress,
                    permanentAddress = permanentAddress,
                    issueDate = issueDate,
                    expireDate = expireDate,
                    isSynced = false
                )

                // Save to database
                beneficiaryDao.updateBeneficiary(updated)

                // Add to sync queue
                addToSyncQueue(updated, "UPDATE")

                // Try to sync immediately if online
                if (isNetworkAvailable()) {
                    syncToFirebase(id)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating beneficiary: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Delete beneficiary
    suspend fun deleteBeneficiary(beneficiaryId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "RELEASE: 🗑️ Deleting beneficiary: $beneficiaryId")

                // First, check if beneficiary exists
                val beneficiary = beneficiaryDao.getBeneficiaryById(beneficiaryId)
                if (beneficiary == null) {
                    Log.e(TAG, "RELEASE: Beneficiary not found for deletion: $beneficiaryId")
                    return@withContext Result.failure(Exception("Beneficiary not found"))
                }

                // Delete from local database
                beneficiaryDao.deleteBeneficiary(beneficiaryId)
                childDao.deleteChildrenByBeneficiary(beneficiaryId)

                // Remove from sync queue
                syncQueueDao.deleteJobs(beneficiaryId, "BENEFICIARY")

                Log.d(TAG, "RELEASE: ✅ Deleted from local database: $beneficiaryId")

                // Delete from Firebase if online
                if (isNetworkAvailable()) {
                    deleteFromFirebase(beneficiaryId)
                } else {
                    // Add delete operation to sync queue for later
                    addToSyncQueueForDeletion(beneficiaryId)
                    Log.d(TAG, "RELEASE: 📱 Added delete operation to sync queue: $beneficiaryId")
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "RELEASE: ❌ Error deleting beneficiary: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Delete from Firebase
    private suspend fun deleteFromFirebase(beneficiaryId: String) {
        try {
            val databaseRef = firebaseDatabase.reference
            databaseRef.child("beneficiaries").child(beneficiaryId).removeValue().await()
            Log.d(TAG, "RELEASE: ✅ Deleted from Firebase: beneficiaries/$beneficiaryId")
        } catch (firebaseError: Exception) {
            Log.e(TAG, "RELEASE: ❌ Error deleting from Firebase, but local delete successful", firebaseError)
        }
    }

    // Add to sync queue for deletion
    private suspend fun addToSyncQueueForDeletion(beneficiaryId: String) {
        try {
            syncQueueDao.insertSyncJob(
                SyncQueueEntity(
                    operation = "DELETE",
                    dataType = "BENEFICIARY",
                    dataId = beneficiaryId,
                    dataJson = "",
                    priority = 1,
                    createdAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "RELEASE: Error adding delete to sync queue: ${e.message}")
        }
    }

    // Update beneficiary entity
    suspend fun updateBeneficiary(beneficiary: LocalBeneficiaryEntity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Update in local database
                beneficiaryDao.updateBeneficiary(beneficiary)

                // Mark as unsynced for re-sync
                beneficiaryDao.updateSyncStatus(beneficiary.id, false)

                // Add to sync queue
                addToSyncQueue(beneficiary, "UPDATE")

                // Try to sync immediately if online
                if (isNetworkAvailable()) {
                    syncToFirebase(beneficiary.id)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating beneficiary: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Clear all local data
    suspend fun clearAllLocalData(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                beneficiaryDao.deleteAllBeneficiaries()
                childDao.deleteAllChildren()
                syncQueueDao.deleteAllJobs()
                Log.d(TAG, "RELEASE: Cleared all local data")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing local data: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Get unsynced count
    suspend fun getUnsyncedCount(): Int {
        return beneficiaryDao.getUnsyncedCount()
    }

    // Generate unique ID
    private fun generateBeneficiaryId(): String {
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        val random = (1000..9999).random()
        return "BEN-$timestamp-$random"
    }
}