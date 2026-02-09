import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "beneficiaries")
data class BeneficiaryData(
    @PrimaryKey
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val age: String = "",
    val cnic: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val phoneNumber: String = "",
    val temporaryAddress: String = "",
    val permanentAddress: String = "",
    val district: String = "",
    val taluka: String = "",
    val unionCouncil: String = "",
    val issueDate: String = "",
    val expireDate: String = "",
    val beneficiaryStatus: String = "", // "PREGNANT" or "LACTATING"
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val lastUpdated: String = ""
)