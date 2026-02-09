package com.example.beneficiaryapp.utils

import java.text.SimpleDateFormat
import java.util.*

object ValidationUtils {

    // Name validation: Only letters and spaces, 2-50 characters
    fun isValidName(name: String): Boolean {
        return try {
            name.matches(Regex("^[A-Za-z\\s]{2,50}$"))
        } catch (e: Exception) {
            false
        }
    }

    // Age validation: 16-60 years
    fun isValidAge(age: String): Boolean {
        return try {
            val ageInt = age.toInt()
            ageInt in 16..60
        } catch (e: Exception) {
            false
        }
    }

    // CNIC validation: Pakistani format #####-#######-#
    fun isValidCNIC(cnic: String): Boolean {
        return try {
            cnic.matches(Regex("^\\d{5}-\\d{7}-\\d{1}$"))
        } catch (e: Exception) {
            false
        }
    }

    // Alternative CNIC validation (without dashes)
    fun isValidCNICSimple(cnic: String): Boolean {
        return try {
            cnic.matches(Regex("^\\d{13}$"))
        } catch (e: Exception) {
            false
        }
    }

    // Date validation: dd-MMM-yyyy format
    fun isValidDate(date: String): Boolean {
        return try {
            date.matches(Regex("^\\d{2}-[A-Za-z]{3}-\\d{4}$"))
        } catch (e: Exception) {
            false
        }
    }

    // SAFE Phone number validation: Will not crash
    fun isValidPhone(phone: String): Boolean {
        return try {
            if (phone.isBlank()) return false

            // Remove all non-digit characters
            val cleanedPhone = phone.replace(Regex("[^0-9+]"), "")

            // Check for various Pakistani phone formats
            when {
                // Format 1: 03XX XXXXXXX (11 digits starting with 03)
                cleanedPhone.matches(Regex("^03\\d{9}$")) -> true

                // Format 2: 923XXXXXXXXX (12 digits starting with 923)
                cleanedPhone.matches(Regex("^923\\d{9}$")) -> true

                // Format 3: +923XXXXXXXXX (13 digits with +92)
                cleanedPhone.matches(Regex("^\\+923\\d{9}$")) -> true

                // Format 4: 00923XXXXXXXXX (14 digits with 0092)
                cleanedPhone.matches(Regex("^00923\\d{9}$")) -> true

                // Format 5: 3XXXXXXXXXX (10 digits starting with 3)
                cleanedPhone.matches(Regex("^3\\d{9}$")) -> true

                else -> false
            }
        } catch (e: Exception) {
            false // Return false instead of crashing
        }
    }

    // Alternative: More lenient phone validation
    fun isValidPhoneLenient(phone: String): Boolean {
        return try {
            if (phone.isBlank()) return false

            // Remove all non-digit characters
            val cleanedPhone = phone.replace(Regex("[^0-9+]"), "")

            // Check if it's a valid length (10-14 digits for Pakistani numbers)
            when (cleanedPhone.length) {
                10 -> cleanedPhone.startsWith("3")  // Mobile: 3XXXXXXXXX
                11 -> cleanedPhone.startsWith("03") // Mobile: 03XXXXXXXXX
                12 -> cleanedPhone.startsWith("923") // International: 923XXXXXXXXX
                13 -> cleanedPhone.startsWith("+923") // International: +923XXXXXXXXX
                14 -> cleanedPhone.startsWith("00923") // International: 00923XXXXXXXXX
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    // SAFE Get phone error message - won't crash
    fun getPhoneError(phone: String): String? {
        return try {
            when {
                phone.isEmpty() -> "Phone is required"
                !isValidPhone(phone) -> {
                    val cleaned = phone.replace(Regex("[^0-9+]"), "")
                    val length = cleaned.length

                    "Invalid Pakistani phone number. Valid formats:\n" +
                            "• 03XX-XXXXXXX (11 digits)\n" +
                            "• 923XXXXXXXXX (12 digits)\n" +
                            "• +923XXXXXXXXX (13 digits)\n" +
                            "• 00923XXXXXXXXX (14 digits)\n" +
                            "You entered: $length digits"
                }
                else -> null
            }
        } catch (e: Exception) {
            "Please enter a valid phone number"
        }
    }

    // SAFE Helper function to format phone number
    fun formatPhoneNumber(phone: String): String {
        return try {
            val cleaned = phone.replace(Regex("[^0-9]"), "")

            when {
                cleaned.matches(Regex("^03\\d{9}$")) ->
                    cleaned.replace(Regex("^(\\d{4})(\\d{7})$"), "$1-$2")

                cleaned.matches(Regex("^923\\d{9}$")) ->
                    cleaned.replace(Regex("^(\\d{3})(\\d{4})(\\d{7})$"), "$1-$2-$3")

                else -> phone // Return original if format not recognized
            }
        } catch (e: Exception) {
            phone // Return original if any error
        }
    }

    // SAFE Parse phone number to international format
    fun toInternationalFormat(phone: String): String {
        return try {
            val cleaned = phone.replace(Regex("[^0-9+]"), "")

            when {
                cleaned.startsWith("+") -> cleaned
                cleaned.startsWith("0092") -> "+92${cleaned.drop(4)}"
                cleaned.startsWith("92") && cleaned.length == 12 -> "+$cleaned"
                cleaned.startsWith("03") -> "+92${cleaned.drop(1)}"
                cleaned.startsWith("3") && cleaned.length == 10 -> "+92$cleaned"
                else -> phone
            }
        } catch (e: Exception) {
            phone
        }
    }

    // Address validation: Minimum 10 characters
    fun isValidAddress(address: String): Boolean {
        return try {
            address.length >= 10
        } catch (e: Exception) {
            false
        }
    }

    // District/Taluka/Union Council validation: Not empty
    fun isValidLocation(location: String): Boolean {
        return try {
            location.isNotBlank() && location.length >= 2
        } catch (e: Exception) {
            false
        }
    }

    // Issue date should be before expire date
    fun areDatesValid(issueDate: String, expireDate: String): Boolean {
        return try {
            if (!isValidDate(issueDate) || !isValidDate(expireDate)) return false

            val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
            val issue = dateFormat.parse(issueDate)
            val expire = dateFormat.parse(expireDate)
            issue != null && expire != null && issue.before(expire)
        } catch (e: Exception) {
            false
        }
    }

    // Delivery date validation: Should be within last 2 years
    fun isValidDeliveryDate(date: String): Boolean {
        return try {
            if (!isValidDate(date)) return false

            val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
            val deliveryDate = dateFormat.parse(date)
            val twoYearsAgo = Calendar.getInstance().apply {
                add(Calendar.YEAR, -2)
            }.time
            deliveryDate != null && deliveryDate.after(twoYearsAgo)
        } catch (e: Exception) {
            false
        }
    }

    // Child name validation
    fun isValidChildName(name: String): Boolean {
        return try {
            name.matches(Regex("^[A-Za-z\\s]{2,30}$"))
        } catch (e: Exception) {
            false
        }
    }

    // SAFE Get error messages
    fun getNameError(name: String): String? {
        return try {
            when {
                name.isEmpty() -> "Name is required"
                !isValidName(name) -> "Enter valid name (2-50 letters)"
                else -> null
            }
        } catch (e: Exception) {
            "Please enter a valid name"
        }
    }

    fun getAgeError(age: String): String? {
        return try {
            when {
                age.isEmpty() -> "Age is required"
                !isValidAge(age) -> "Age must be between 16-60"
                else -> null
            }
        } catch (e: Exception) {
            "Please enter a valid age"
        }
    }

    fun getCNICError(cnic: String): String? {
        return try {
            when {
                cnic.isEmpty() -> "CNIC is required"
                !isValidCNIC(cnic) && !isValidCNICSimple(cnic) ->
                    "Enter valid CNIC (XXXXX-XXXXXXX-X or 13 digits)"
                else -> null
            }
        } catch (e: Exception) {
            "Please enter a valid CNIC"
        }
    }

    fun getAddressError(address: String, fieldName: String): String? {
        return try {
            when {
                address.isEmpty() -> "$fieldName is required"
                !isValidAddress(address) -> "$fieldName must be at least 10 characters"
                else -> null
            }
        } catch (e: Exception) {
            "Please enter a valid $fieldName"
        }
    }

    fun getLocationError(location: String, fieldName: String): String? {
        return try {
            when {
                location.isEmpty() -> "$fieldName is required"
                !isValidLocation(location) -> "Enter valid $fieldName"
                else -> null
            }
        } catch (e: Exception) {
            "Please enter a valid $fieldName"
        }
    }

    fun getDateError(date: String, fieldName: String): String? {
        return try {
            when {
                date.isEmpty() -> "$fieldName is required"
                !isValidDate(date) -> "Format: dd-MMM-yyyy (e.g., 14-Sep-1999)"
                else -> null
            }
        } catch (e: Exception) {
            "Please enter a valid $fieldName"
        }
    }

    fun getDateRangeError(issueDate: String, expireDate: String): String? {
        return try {
            if (!areDatesValid(issueDate, expireDate))
                "Issue date must be before expire date"
            else null
        } catch (e: Exception) {
            "Please check the dates"
        }
    }

    fun getDeliveryDateError(date: String): String? {
        return try {
            when {
                date.isEmpty() -> "Delivery date is required"
                !isValidDate(date) -> "Format: dd-MMM-yyyy"
                !isValidDeliveryDate(date) -> "Delivery date should be within last 2 years"
                else -> null
            }
        } catch (e: Exception) {
            "Please enter a valid delivery date"
        }
    }

    fun getChildNameError(name: String): String? {
        return try {
            when {
                name.isEmpty() -> "Child name is required"
                !isValidChildName(name) -> "Enter valid child name (2-30 letters)"
                else -> null
            }
        } catch (e: Exception) {
            "Please enter a valid child name"
        }
    }

    // Even safer phone validation for TextField input
    fun validatePhoneInput(input: String): ValidationResult {
        return try {
            if (input.isEmpty()) {
                ValidationResult(false, "Phone number is required")
            } else if (!isValidPhone(input)) {
                ValidationResult(false, "Please enter a valid Pakistani phone number")
            } else {
                ValidationResult(true, null)
            }
        } catch (e: Exception) {
            ValidationResult(false, "Invalid phone number format")
        }
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String?
    )
}