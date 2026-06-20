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
        return registerWithProfile(
            createAuthUser = {
                auth.createUserWithEmailAndPassword(email, password)
                    .await()
                    .user
                    ?: throw IllegalStateException("Firebase Auth returned no user")
            },
            writeProfile = { user ->
                db.collection("users")
                    .document(user.uid)
                    .set(newProfileDocument(user, fullName, username))
                    .await()
            },
            rollbackAuthUser = { user ->
                user.delete().await()
            }
        )
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth
                .signInWithEmailAndPassword(email, password)
                .await()
            val user = result.user
                ?: return Result.failure(IllegalStateException("Firebase Auth returned no user"))
            ensureProfileExists(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user!!

            ensureProfileExists(user)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun logout() = auth.signOut()

    private suspend fun ensureProfileExists(user: FirebaseUser) {
        createProfileIfMissing(
            db = db,
            uid = user.uid
        ) {
            val username = recoveryUsername(user)
            newProfileDocument(
                user = user,
                fullName = user.displayName.orEmpty().ifBlank { username },
                username = username
            )
        }
    }

    private fun newProfileDocument(
        user: FirebaseUser,
        fullName: String,
        username: String
    ): Map<String, Any> = User.newDocumentMap(
        uid = user.uid,
        username = User.normalizeUsername(username, user.uid),
        fullName = fullName.trim().ifBlank { username }.take(80),
        photoUrl = user.photoUrl?.toString().orEmpty().take(2048),
        createdAt = FieldValue.serverTimestamp(),
        updatedAt = FieldValue.serverTimestamp()
    )

    private fun recoveryUsername(user: FirebaseUser): String =
        User.normalizeUsername(
            value = user.displayName
                ?: user.email?.substringBefore("@")
                ?: "",
            uid = user.uid
        )
}
