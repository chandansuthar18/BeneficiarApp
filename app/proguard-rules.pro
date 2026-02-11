# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.TypeConverter
-keep @androidx.room.Entity class ** { *; }

# Room DAOs
-keep class * extends androidx.room.Dao { *; }

# Room specific
-keepclassmembers class * {
    @androidx.room.* *;
}

# Keep all classes in your data package
-keep class com.example.beneficiaryapp.data.** { *; }
-keep class com.example.beneficiaryapp.data.local.** { *; }
-keep class com.example.beneficiaryapp.data.dto.** { *; }
-keep class com.example.beneficiaryapp.data.repository.** { *; }

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep Hilt classes
-keep class com.example.beneficiaryapp.Hilt_* { *; }
-keep @javax.inject.Inject class * { *; }

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }