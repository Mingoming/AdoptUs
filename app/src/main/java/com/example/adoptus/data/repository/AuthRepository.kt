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

    // Cache user profile locally
    fun cacheUserProfile(context: android.content.Context, user: User) {
        val prefs = context.getSharedPreferences("adoptus_user_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("id", user.id)
            putString("username", user.username)
            putString("fullName", user.fullName)
            putString("photoUrl", user.photoUrl)
            putString("bio", user.bio)
            putString("city", user.city)
            putString("whatsapp", user.whatsapp)
            putString("role", user.role)
            apply()
        }
    }

    // Get cached user profile
    fun getCachedUserProfile(context: android.content.Context): User? {
        val prefs = context.getSharedPreferences("adoptus_user_prefs", android.content.Context.MODE_PRIVATE)
        val id = prefs.getString("id", null) ?: return null
        return User(
            id = id,
            username = prefs.getString("username", "") ?: "",
            fullName = prefs.getString("fullName", "") ?: "",
            photoUrl = prefs.getString("photoUrl", "") ?: "",
            bio = prefs.getString("bio", "") ?: "",
            city = prefs.getString("city", "") ?: "",
            whatsapp = prefs.getString("whatsapp", "") ?: "",
            role = prefs.getString("role", "user") ?: "user"
        )
    }

    // Clear user cache
    fun clearUserCache(context: android.content.Context) {
        val prefs = context.getSharedPreferences("adoptus_user_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun logout() = auth.signOut()
}
