package com.example.adoptus.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adoptus.data.model.Post
import com.example.adoptus.data.model.User
import com.example.adoptus.data.repository.AuthRepository
import com.example.adoptus.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeMyPosts()
    }

    fun refreshProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, profileError = null) }
            authRepository.getCurrentUserProfile().fold(
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
    }

    private fun observeMyPosts() {
        viewModelScope.launch {
            postRepository.getMyPosts().collect { result ->
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
}

internal fun User.profileDisplayName(): String =
    fullName.ifBlank { username }

internal fun User.profileLocation(): String =
    city.trim()
