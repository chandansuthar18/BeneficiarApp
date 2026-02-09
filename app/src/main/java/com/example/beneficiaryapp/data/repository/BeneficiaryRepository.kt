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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
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
                val currentUser = firebaseAuth.currentUser
                if (currentUser == null) {
                    Log.e(TAG, "User not authenticated")
                    return@withContext Result.failure(Exception("User not authenticated"))
                }

                // Generate unique ID
                val beneficiaryId = generateBeneficiaryId()
                Log.d(TAG, "Generated beneficiary ID: $beneficiaryId")

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
                    userId = currentUser.uid,
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
                Log.d(TAG, "✅ Saved to local database: $beneficiaryId")

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
                    Log.d(TAG, "✅ Saved ${children.size} children for beneficiary: $beneficiaryId")
                }

                // Try to sync immediately if online
                if (isNetworkAvailable()) {
                    Log.d(TAG, "🌐 Network available, attempting sync...")
                    syncToFirebase(beneficiaryId)
                } else {
                    // Add to sync queue for later
                    val beneficiaryJson = gson.toJson(localBeneficiary)
                    syncQueueDao.insertSyncJob(
                        SyncQueueEntity(
                            operation = "CREATE",
                            dataType = "BENEFICIARY",
                            dataId = beneficiaryId,
                            dataJson = beneficiaryJson,
                            priority = 1,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    Log.d(TAG, "📱 Added to sync queue (offline). ID: $beneficiaryId")
                }

                Result.success(beneficiaryId)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error saving beneficiary: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Sync to Firebase
    private suspend fun syncToFirebase(beneficiaryId: String) {
        try {
            Log.d(TAG, "🔄 Syncing beneficiary: $beneficiaryId")

            // Get beneficiary from local DB
            val beneficiary = beneficiaryDao.getBeneficiaryById(beneficiaryId)
            if (beneficiary == null) {
                Log.e(TAG, "Beneficiary not found in local DB: $beneficiaryId")
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
            Log.d(TAG, "✅ Uploaded to Firebase: beneficiaries/$beneficiaryId")

            // Also save to user's personal collection
            val currentUser = firebaseAuth.currentUser
            currentUser?.let { user ->
                val userBeneficiaryData = mapOf(
                    "id" to beneficiary.id,
                    "name" to beneficiary.name,
                    "type" to beneficiary.beneficiaryStatus.lowercase(),
                    "createdAt" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(Date(beneficiary.createdAt))
                )
                databaseRef.child("userBeneficiaries").child(user.uid).child(beneficiaryId)
                    .setValue(userBeneficiaryData).await()
                Log.d(TAG, "✅ Saved to user collection: userBeneficiaries/${user.uid}/$beneficiaryId")
            }

            // Mark as synced
            beneficiaryDao.updateSyncStatus(beneficiaryId, true)
            Log.d(TAG, "✅ Marked as synced in local DB: $beneficiaryId")

            // Remove from sync queue
            syncQueueDao.deleteJobs(beneficiaryId, "BENEFICIARY")
            Log.d(TAG, "✅ Removed from sync queue: $beneficiaryId")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to sync beneficiary: $beneficiaryId", e)
            beneficiaryDao.incrementSyncAttempt(beneficiaryId)
        }
    }

    // Get all beneficiaries for current user
    fun getBeneficiariesForCurrentUser(): Flow<List<LocalBeneficiaryEntity>> {
        val userId = firebaseAuth.currentUser?.uid ?: ""
        return beneficiaryDao.getBeneficiariesByUser(userId)
    }

    // Get all beneficiaries (for admin purposes)
    fun getAllBeneficiaries(): Flow<List<LocalBeneficiaryEntity>> {
        return beneficiaryDao.getAllBeneficiaries()
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
    // Update syncAllPendingData method to handle no status field
    suspend fun syncAllPendingData() {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No network available for sync")
            return
        }

        Log.d(TAG, "🔄 Syncing all pending data...")

        // Get unsynced beneficiaries
        val unsyncedBeneficiaries = beneficiaryDao.getUnsyncedBeneficiaries()
        Log.d(TAG, "Found ${unsyncedBeneficiaries.size} unsynced beneficiaries")

        for (beneficiary in unsyncedBeneficiaries) {
            syncToFirebase(beneficiary.id)
        }

        // Process sync queue (get all jobs since we don't have status filter)
        val pendingJobs = syncQueueDao.getPendingJobs(50)
        Log.d(TAG, "Found ${pendingJobs.size} pending jobs in queue")

        for (job in pendingJobs) {
            try {
                when (job.operation) {
                    "CREATE" -> {
                        if (job.dataType == "BENEFICIARY") {
                            // Parse the JSON and sync
                            try {
                                val beneficiary = gson.fromJson(job.dataJson, LocalBeneficiaryEntity::class.java)
                                beneficiaryDao.insertBeneficiary(beneficiary)
                                syncToFirebase(job.dataId)
                                // Delete after successful sync
                                syncQueueDao.deleteJobById(job.jobId)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing beneficiary from queue: ${e.message}")
                                // Increment attempts
                                syncQueueDao.incrementAttempts(job.jobId)
                            }
                        }
                    }
                    "DELETE" -> {
                        // Handle delete operations from queue
                        if (job.dataType == "BENEFICIARY") {
                            try {
                                deleteFromFirebase(job.dataId)
                                // Delete after successful deletion
                                syncQueueDao.deleteJobById(job.jobId)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error deleting from Firebase: ${e.message}")
                                syncQueueDao.incrementAttempts(job.jobId)
                            }
                        }
                    }
                    "UPDATE" -> {
                        // Handle update operations
                        if (job.dataType == "BENEFICIARY") {
                            try {
                                val beneficiary = gson.fromJson(job.dataJson, LocalBeneficiaryEntity::class.java)
                                beneficiaryDao.updateBeneficiary(beneficiary)
                                syncToFirebase(job.dataId)
                                // Delete after successful sync
                                syncQueueDao.deleteJobById(job.jobId)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error updating beneficiary from queue: ${e.message}")
                                syncQueueDao.incrementAttempts(job.jobId)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing sync job ${job.jobId}: ${e.message}")
                syncQueueDao.incrementAttempts(job.jobId)
            }
        }

        Log.d(TAG, "✅ Sync completed")
    }
    // Delete beneficiary
    suspend fun deleteBeneficiary(beneficiaryId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🗑️ Deleting beneficiary: $beneficiaryId")

                // First, check if beneficiary exists
                val beneficiary = beneficiaryDao.getBeneficiaryById(beneficiaryId)
                if (beneficiary == null) {
                    Log.e(TAG, "Beneficiary not found for deletion: $beneficiaryId")
                    return@withContext Result.failure(Exception("Beneficiary not found"))
                }

                // Delete from local database
                beneficiaryDao.deleteBeneficiary(beneficiaryId)
                childDao.deleteChildrenByBeneficiary(beneficiaryId)

                // Remove from sync queue
                syncQueueDao.deleteJobs(beneficiaryId, "BENEFICIARY")

                Log.d(TAG, "✅ Deleted from local database: $beneficiaryId")

                // Delete from Firebase if online
                if (isNetworkAvailable()) {
                    deleteFromFirebase(beneficiaryId)
                } else {
                    // Add delete operation to sync queue for later
                    addToSyncQueueForDeletion(beneficiaryId)
                    Log.d(TAG, "📱 Added delete operation to sync queue: $beneficiaryId")
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error deleting beneficiary: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // Delete from Firebase (separate method for reuse)
    private suspend fun deleteFromFirebase(beneficiaryId: String) {
        try {
            val databaseRef = firebaseDatabase.reference

            // Delete from main beneficiaries collection
            databaseRef.child("beneficiaries").child(beneficiaryId).removeValue().await()
            Log.d(TAG, "✅ Deleted from Firebase: beneficiaries/$beneficiaryId")

            // Also delete from user collection
            val currentUser = firebaseAuth.currentUser
            currentUser?.let { user ->
                databaseRef.child("userBeneficiaries").child(user.uid).child(beneficiaryId)
                    .removeValue().await()
                Log.d(TAG, "✅ Deleted from user collection: userBeneficiaries/${user.uid}/$beneficiaryId")
            }
        } catch (firebaseError: Exception) {
            Log.e(TAG, "❌ Error deleting from Firebase, but local delete successful", firebaseError)
            // Continue even if Firebase delete fails
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
            Log.e(TAG, "Error adding delete to sync queue: ${e.message}")
        }
    }

    // Update beneficiary data
    suspend fun updateBeneficiary(beneficiary: LocalBeneficiaryEntity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Update in local database
                beneficiaryDao.updateBeneficiary(beneficiary)

                // Mark as unsynced for re-sync
                beneficiaryDao.updateSyncStatus(beneficiary.id, false)

                // Add to sync queue
                val beneficiaryJson = gson.toJson(beneficiary)
                syncQueueDao.insertSyncJob(
                    SyncQueueEntity(
                        operation = "UPDATE",
                        dataType = "BENEFICIARY",
                        dataId = beneficiary.id,
                        dataJson = beneficiaryJson,
                        priority = 2,
                        createdAt = System.currentTimeMillis()
                    )
                )

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
                Log.d(TAG, "✅ Cleared all local data")
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