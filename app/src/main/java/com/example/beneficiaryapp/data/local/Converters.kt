package com.example.beneficiaryapp.data.local

import androidx.room.TypeConverter
import com.example.beneficiaryapp.data.BeneficiaryStatus
import com.example.beneficiaryapp.data.ChildData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class Converters {
    private val gson = Gson()

    // ================ DATE CONVERTERS ================
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // ================ BeneficiaryStatus enum ================
    @TypeConverter
    fun fromBeneficiaryStatus(status: BeneficiaryStatus): String {
        return status.name
    }

    @TypeConverter
    fun toBeneficiaryStatus(status: String): BeneficiaryStatus {
        return BeneficiaryStatus.valueOf(status)

    }

    // ================ List<ChildData> ================
    @TypeConverter
    fun fromChildrenList(children: List<ChildData>): String {
        return gson.toJson(children)
    }

    @TypeConverter
    fun toChildrenList(childrenString: String): List<ChildData> {
        return if (childrenString.isEmpty()) {
            emptyList()
        } else {
            try {
                val type = object : TypeToken<List<ChildData>>() {}.type
                gson.fromJson(childrenString, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // ================ List<String> ================
    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toStringList(string: String): List<String> {
        return if (string.isEmpty()) {
            emptyList()
        } else {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(string, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // ================ List<LocalChildEntity> ================
    @TypeConverter
    fun fromLocalChildrenList(children: List<LocalChildEntity>): String {
        return gson.toJson(children)
    }

    @TypeConverter
    fun toLocalChildrenList(childrenString: String): List<LocalChildEntity> {
        return if (childrenString.isEmpty()) {
            emptyList()
        } else {
            try {
                val type = object : TypeToken<List<LocalChildEntity>>() {}.type
                gson.fromJson(childrenString, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}