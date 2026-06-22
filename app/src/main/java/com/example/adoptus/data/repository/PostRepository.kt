package com.example.adoptus.data.repository

import com.example.adoptus.data.model.Adoption
import com.example.adoptus.data.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PostRepository {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val postsCollection = db.collection("posts")

    // ── Feed ────────────────────────────────────────────────────────────────

    // Ambil semua post real-time, urut dari terbaru
    // callbackFlow mengubah Firestore listener menjadi Flow
    fun getFeedPosts(): Flow<Result<List<Post>>> = callbackFlow {
        val listener = postsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                // Filter status di sisi app — hindari butuh composite index Firestore
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Post.fromMap(doc.id, it) }
                }?.filter { it.status == "available" } ?: emptyList()
                trySend(Result.success(posts))
            }
        // Hentikan listener saat Flow tidak lagi diobserve
        awaitClose { listener.remove() }
    }

    // Ambil post secara terpaginasi (pagination)
    suspend fun getFeedPostsPaginated(
        pageSize: Long,
        lastDocument: com.google.firebase.firestore.DocumentSnapshot?
    ): Result<Pair<List<Post>, com.google.firebase.firestore.DocumentSnapshot?>> {
        return try {
            var query = postsCollection
                .whereEqualTo("status", "available")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(pageSize)

            if (lastDocument != null) {
                query = query.startAfter(lastDocument)
            }

            val snapshot = query.get().await()
            val posts = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { Post.fromMap(doc.id, it) }
            }
            val lastVisible = snapshot.documents.lastOrNull()
            Result.success(Pair(posts, lastVisible))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Ambil post milik user tertentu
    fun getUserPosts(uid: String): Flow<Result<List<Post>>> = callbackFlow {
        val listener = postsCollection
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Post.fromMap(doc.id, it) }
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(Result.success(posts))
            }
        awaitClose { listener.remove() }
    }

    // Ambil post milik user yang sedang login (untuk Profile)
    fun getMyPosts(): Flow<Result<List<Post>>> {
        val uid = auth.currentUser?.uid ?: return callbackFlow {
            trySend(Result.failure(Exception("User not logged in")))
            close()
        }
        return getUserPosts(uid)
    }

    // Ambil satu post by ID (untuk PetDetail)
    suspend fun getPostById(postId: String): Result<Post> {
        return try {
            val doc = postsCollection.document(postId).get().await()
            val post = doc.data?.let { Post.fromMap(doc.id, it) }
                ?: return Result.failure(Exception("Post not found"))
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Upload ───────────────────────────────────────────────────────────────

    // ── Likes ────────────────────────────────────────────────────────────────

    // Toggle like state on a post
    suspend fun toggleLike(postId: String, userId: String): Result<Boolean> {
        return try {
            val postRef = postsCollection.document(postId)
            val postLikeRef = postRef.collection("likes").document(userId)
            val userLikeRef = db.collection("users").document(userId).collection("likedPosts").document(postId)

            val isLiked = db.runTransaction { transaction ->
                val likeDoc = transaction.get(postLikeRef)
                val exists = likeDoc.exists()
                if (exists) {
                    transaction.delete(postLikeRef)
                    transaction.delete(userLikeRef)
                    transaction.update(postRef, "likesCount", com.google.firebase.firestore.FieldValue.increment(-1))
                    false
                } else {
                    transaction.set(postLikeRef, mapOf("likedAt" to com.google.firebase.Timestamp.now()))
                    transaction.set(userLikeRef, mapOf("likedAt" to com.google.firebase.Timestamp.now()))
                    transaction.update(postRef, "likesCount", com.google.firebase.firestore.FieldValue.increment(1))
                    true
                }
            }.await()
            Result.success(isLiked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get list of postIds liked by user
    suspend fun getLikedPostIds(userId: String): Result<Set<String>> {
        return try {
            val snapshot = db.collection("users").document(userId)
                .collection("likedPosts").get().await()
            val ids = snapshot.documents.map { it.id }.toSet()
            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Upload ───────────────────────────────────────────────────────────────

    suspend fun createPost(post: Post, cachedUser: com.example.adoptus.data.model.User? = null): Result<String> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            val ownerUsername: String
            val ownerPhotoUrl: String
            val ownerWhatsapp: String

            if (cachedUser != null && cachedUser.id == uid) {
                ownerUsername = cachedUser.username
                ownerPhotoUrl = cachedUser.photoUrl
                ownerWhatsapp = cachedUser.whatsapp
            } else {
                // Fetch owner info from users collection
                val userDoc = db.collection("users").document(uid).get().await()
                ownerUsername = userDoc.getString("username") ?: ""
                ownerPhotoUrl = userDoc.getString("photoUrl") ?: userDoc.getString("photo_url") ?: ""
                ownerWhatsapp = userDoc.getString("whatsapp") ?: ""
            }

            val docRef = postsCollection.document()
            val postWithId = post.copy(
                postId = docRef.id,
                userId = uid,
                ownerUsername = ownerUsername,
                ownerPhotoUrl = ownerPhotoUrl,
                ownerWhatsapp = ownerWhatsapp
            )
            docRef.set(postWithId.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Update status ────────────────────────────────────────────────────────

    suspend fun updatePostStatus(postId: String, status: String): Result<Unit> {
        return try {
            postsCollection.document(postId)
                .update("status", status)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            postsCollection.document(postId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Search ───────────────────────────────────────────────────────────────

    // Filter berdasarkan jenis hewan (untuk SearchFragment nanti)
    fun getPostsByType(petType: String): Flow<Result<List<Post>>> = callbackFlow {
        val listener = postsCollection
            .whereEqualTo("status", "available")
            .whereEqualTo("petType", petType)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Post.fromMap(doc.id, it) }
                } ?: emptyList()
                trySend(Result.success(posts))
            }
        awaitClose { listener.remove() }
    }

    // ── Adoptions ────────────────────────────────────────────────────────────

    private val adoptionsCollection = db.collection("adoptions")

    // Cek apakah user sudah punya pending adoption untuk post tertentu
    suspend fun checkPendingAdoption(postId: String, adopterId: String): Result<Boolean> {
        return try {
            val snapshot = adoptionsCollection
                .whereEqualTo("postId", postId)
                .whereEqualTo("adopterId", adopterId)
                .whereEqualTo("status", "pending")
                .get()
                .await()
            Result.success(!snapshot.isEmpty)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Ajukan adopsi baru
    suspend fun applyForAdoption(postId: String, petName: String, ownerId: String, adopterId: String): Result<String> {
        return try {
            // Fetch adopter name from user doc
            val adopterDoc = db.collection("users").document(adopterId).get().await()
            val adopterName = adopterDoc.getString("fullName") ?: adopterDoc.getString("full_name") ?: "Adopter"

            val docRef = adoptionsCollection.document()
            val adoption = Adoption(
                adoptionId = docRef.id,
                postId = postId,
                petName = petName,
                adopterId = adopterId,
                adopterName = adopterName,
                ownerId = ownerId,
                status = "pending",
                createdAt = com.google.firebase.Timestamp.now(),
                updatedAt = com.google.firebase.Timestamp.now()
            )
            docRef.set(adoption.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Ambil request adopsi masuk untuk pemilik post secara real-time
    fun getIncomingAdoptions(ownerId: String): Flow<Result<List<Adoption>>> = callbackFlow {
        val listener = adoptionsCollection
            .whereEqualTo("ownerId", ownerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val adoptions = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Adoption.fromMap(doc.id, it) }
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(Result.success(adoptions))
            }
        awaitClose { listener.remove() }
    }

    // Update status pengajuan adopsi (Approve/Reject)
    suspend fun updateAdoptionStatus(adoptionId: String, postId: String, newStatus: String): Result<Unit> {
        return try {
            db.runBatch { batch ->
                val adoptionRef = adoptionsCollection.document(adoptionId)
                batch.update(adoptionRef, "status", newStatus)
                batch.update(adoptionRef, "updatedAt", com.google.firebase.Timestamp.now())

                if (newStatus == "approved") {
                    val postRef = postsCollection.document(postId)
                    batch.update(postRef, "status", "adopted")
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
