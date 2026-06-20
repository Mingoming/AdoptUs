package com.example.adoptus.data.repository

import com.example.adoptus.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        username: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth
                .createUserWithEmailAndPassword(email, password)
                .await()

            val uid = result.user!!.uid

            val userData = User.newDocumentMap(
                uid = uid,
                username = username,
                fullName = fullName,
                photoUrl = "",
                createdAt = FieldValue.serverTimestamp(),
                updatedAt = FieldValue.serverTimestamp()
            )

            db.collection("users")
                .document(uid)
                .set(userData)
                .await()

            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth
                .signInWithEmailAndPassword(email, password)
                .await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user!!

            // Simpan ke Firestore kalau user baru
            val docRef = db.collection("users").document(user.uid)
            val doc = docRef.get().await()
            if (!doc.exists()) {
                val username = User.normalizeUsername(
                    value = user.displayName
                        ?: user.email?.substringBefore("@")
                        ?: "",
                    uid = user.uid
                )
                val userData = User.newDocumentMap(
                    uid = user.uid,
                    username = username,
                    fullName = user.displayName
                        ?.trim()
                        .orEmpty()
                        .ifBlank { username }
                        .take(80),
                    photoUrl = user.photoUrl?.toString().orEmpty().take(2048),
                    createdAt = FieldValue.serverTimestamp(),
                    updatedAt = FieldValue.serverTimestamp()
                )
                docRef.set(userData).await()
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun logout() = auth.signOut()
}
