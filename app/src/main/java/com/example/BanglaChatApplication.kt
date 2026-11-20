package com.example

import android.app.Application
import com.example.data.BanglaChatRepository
import com.example.data.PreferencesRepository
import com.example.service.BanglaChatMessagingService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

class BanglaChatApplication : Application() {

    val firestore: FirebaseFirestore by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val databaseId = getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(databaseId).apply {
            try {
                firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
            } catch (_: IllegalStateException) {
                // Firestore settings already initialized
            }
        }
    }

    val preferencesRepository: PreferencesRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        PreferencesRepository(this)
    }

    val chatRepository: BanglaChatRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        BanglaChatRepository(firestore)
    }

    override fun onCreate() {
        super.onCreate()
        BanglaChatMessagingService.createMessageNotificationChannel(this)
    }
}
