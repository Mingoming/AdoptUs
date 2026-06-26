package com.example.adoptus.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adoptus.data.model.Post
import com.example.adoptus.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PetDetailState {
    object Loading : PetDetailState()
    data class Success(val post: Post) : PetDetailState()
    data class Error(val message: String) : PetDetailState()
}

class PetDetailViewModel(
    private val repository: PostRepository = PostRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<PetDetailState>(PetDetailState.Loading)
    val state: StateFlow<PetDetailState> = _state.asStateFlow()

    fun loadPost(postId: String, forceRefresh: Boolean = false) {
        val currentPost = (_state.value as? PetDetailState.Success)?.post
        if (!forceRefresh && currentPost?.postId == postId) return

        if (postId.isBlank()) {
            _state.value = PetDetailState.Error("Post ID is missing")
            return
        }

        viewModelScope.launch {
            _state.value = PetDetailState.Loading
            repository.getPostById(postId).fold(
                onSuccess = { post ->
                    _state.value = PetDetailState.Success(post)
                },
                onFailure = { error ->
                    _state.value = PetDetailState.Error(
                        error.message ?: "Failed to load post"
                    )
                }
            )
        }
    }
}

internal fun Post.detailBreedAge(): String {
    val breedLabel = breed.ifBlank { petType.ifBlank { "Unknown breed" } }
    return "$breedLabel | $ageDisplay"
}

internal fun Post.detailFee(): String =
    if (isFree) "Free" else "Rp $adoptionFee"

internal fun Post.hasDetailImage(): Boolean =
    mediaType == "image" && mediaUrl.isNotBlank()

internal fun Post.hasDetailVideo(): Boolean =
    mediaType == "video" && mediaUrl.isNotBlank()
