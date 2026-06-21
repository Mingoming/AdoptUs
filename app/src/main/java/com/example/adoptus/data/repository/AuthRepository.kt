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

            val user = result.user
                ?: throw IllegalStateException("Firebase Auth returned no user")

            try {
                val userData = User(
                    id = user.uid,
                    username = username,
                    fullName = fullName,
                    createdAt = FieldValue.serverTimestamp()
                ).toMap()

                db.collection("users")
                    .document(user.uid)
                    .set(userData)
                    .await()
            } catch (profileError: Exception) {
                try {
                    user.delete().await()
                } catch (rollbackError: Exception) {
                    profileError.addSuppressed(rollbackError)
                }
                return Result.failure(profileError)
            }

            Result.success(user)
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
                    user.displayName ?: user.email?.substringBefore("@") ?: "",
                    user.uid
                )
                val userData = User(
                    id = user.uid,
                    username = username,
                    fullName = user.displayName?.trim().orEmpty().ifBlank { username },
                    photoUrl = user.photoUrl?.toString().orEmpty(),
                    createdAt = FieldValue.serverTimestamp()
                ).toMap()
                docRef.set(userData).await()
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun getCurrentUserProfile(): Result<User> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))
            val document = db.collection("users").document(uid).get().await()
            val data = document.data
                ?: return Result.failure(Exception("User profile not found"))

            Result.success(User.fromMap(document.id, data))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() = auth.signOut()
}
