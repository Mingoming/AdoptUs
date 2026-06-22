package com.example.adoptus

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

class AdoptUsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Konfigurasi cache offline persistent untuk Firestore secara eksplisit
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings
    }
}
