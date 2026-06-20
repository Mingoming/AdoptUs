package com.example.adoptus.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

internal fun <T> profileForRecovery(
    documentExists: Boolean,
    profileFactory: () -> T
): T? = if (documentExists) null else profileFactory()

internal suspend fun createProfileIfMissing(
    db: FirebaseFirestore,
    uid: String,
    profileFactory: () -> Map<String, Any>
): Boolean {
    val document = db.collection("users").document(uid)
    return db.runTransaction { transaction ->
        val profile = profileForRecovery(
            documentExists = transaction.get(document).exists(),
            profileFactory = profileFactory
        )
        if (profile == null) {
            false
        } else {
            transaction.set(document, profile)
            true
        }
    }.await()
}
