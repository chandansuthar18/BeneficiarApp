package com.example.beneficiaryapp.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context

@Database(
    entities = [
        LocalBeneficiaryEntity::class,
        LocalChildEntity::class,
        SyncQueueEntity::class  // Make sure this is included
    ],
    version = 2,  // Increment version since we changed schema
    exportSchema = false
)
@TypeConverters(Converters::class)  // If you have converters
abstract class AppDatabase : RoomDatabase() {
    abstract fun beneficiaryDao(): BeneficiaryDao
    abstract fun childDao(): ChildDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "beneficiary_database"
                )
                    .fallbackToDestructiveMigration()  // Use this during development
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}