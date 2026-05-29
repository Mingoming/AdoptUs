package com.example.adoptus.data.repository

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

            val userData = hashMapOf(
                "id"         to uid,
                "username"   to username,
                "email"      to email,
                "full_name"  to fullName,
                "photo_url"  to "",
                "role"       to "user",
                "created_at" to FieldValue.serverTimestamp()
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
                val userData = hashMapOf(
                    "id"         to user.uid,
                    "username"   to (user.displayName ?: user.email?.substringBefore("@") ?: ""),
                    "email"      to (user.email ?: ""),
                    "full_name"  to (user.displayName ?: ""),
                    "photo_url"  to (user.photoUrl?.toString() ?: ""),
                    "role"       to "user",
                    "created_at" to FieldValue.serverTimestamp()
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
