package com.example.adoptus.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adoptus.data.model.Post
import com.example.adoptus.data.model.User
import com.example.adoptus.data.repository.AuthRepository
import com.example.adoptus.data.repository.PostRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ProfileUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val posts: List<Post> = emptyList(),
    val profileError: String? = null,
    val postsError: String? = null
)

class ProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val postRepository = PostRepository()
    private val db = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var targetUserId: String? = null

    fun loadProfile(userId: String?) {
        targetUserId = userId
        val uid = userId ?: authRepository.getCurrentUser()?.uid
        if (uid == null) {
            _uiState.update {
                it.copy(isLoading = false, profileError = "Not logged in")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, profileError = null) }
            
            val userResult = if (uid == authRepository.getCurrentUser()?.uid) {
                authRepository.getCurrentUserProfile()
            } else {
                try {
                    val doc = db.collection("users").document(uid).get().await()
                    val user = User.fromMap(doc.id, doc.data.orEmpty())
                    Result.success(user)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            userResult.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(isLoading = false, user = user, profileError = null)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profileError = error.message ?: "Failed to load profile"
                        )
                    }
                }
            )
        }

        viewModelScope.launch {
            postRepository.getUserPosts(uid).collect { result ->
                result.fold(
                    onSuccess = { posts ->
                        _uiState.update {
                            it.copy(posts = posts, postsError = null)
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(postsError = error.message ?: "Failed to load posts")
                        }
                    }
                )
            }
        }
    }

    fun refreshProfile() {
        loadProfile(targetUserId)
    }
}

internal fun User.profileDisplayName(): String =
    fullName.ifBlank { username }

internal fun User.profileLocation(): String =
    city.trim()

