package com.example.adoptus.data.repository

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

    // Ambil post milik user yang sedang login (untuk Profile)
    fun getMyPosts(): Flow<Result<List<Post>>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: run {
            trySend(Result.failure(Exception("User not logged in")))
            close()
            return@callbackFlow
        }
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

    suspend fun createPost(post: Post): Result<String> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(Exception("User not logged in"))

            val docRef = postsCollection.document()
            val postWithId = post.copy(
                postId = docRef.id,
                userId = uid
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
}
