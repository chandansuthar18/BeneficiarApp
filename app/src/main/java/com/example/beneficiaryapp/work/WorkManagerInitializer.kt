//package com.example.beneficiaryapp.work
//
//import android.content.Context
//import androidx.startup.Initializer
//import androidx.work.Configuration
//import androidx.work.WorkManager
//
//class WorkManagerInitializer : Initializer<WorkManager> {
//    override fun create(context: Context): WorkManager {
//        // Don't try to get instance or schedule work here
//        // Just initialize WorkManager with default configuration
//        val configuration = Configuration.Builder()
//            .setMinimumLoggingLevel(android.util.Log.INFO)
//            .build()
//
//        WorkManager.initialize(context, configuration)
//
//        return WorkManager.getInstance(context)
//    }
//
//    override fun dependencies(): List<Class<out Initializer<*>>> {
//        return emptyList()
//    }
//}