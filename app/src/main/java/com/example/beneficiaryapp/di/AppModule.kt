package com.example.beneficiaryapp.di

import android.content.Context
import com.example.beneficiaryapp.data.local.AppDatabase
import com.example.beneficiaryapp.data.repository.BeneficiaryRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context) // Changed from getDatabase to getInstance
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        val database = FirebaseDatabase.getInstance()
        // Enable persistence for offline support
        database.setPersistenceEnabled(true)
        return database
    }

    @Provides
    @Singleton
    fun provideBeneficiaryRepository(
        appDatabase: AppDatabase,
        @ApplicationContext context: Context,
        gson: Gson,
        firebaseAuth: FirebaseAuth,
        firebaseDatabase: FirebaseDatabase
    ): BeneficiaryRepository {
        return BeneficiaryRepository(
            appDatabase = appDatabase,
            context = context,
            gson = gson,
            firebaseAuth = firebaseAuth,
            firebaseDatabase = firebaseDatabase
        )
    }
}