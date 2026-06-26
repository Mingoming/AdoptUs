package com.example.adoptus.ui.editpost

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adoptus.data.model.Post
import com.example.adoptus.data.repository.AuthRepository
import com.example.adoptus.data.repository.PostMediaRepository
import com.example.adoptus.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class EditPostState {
    object Idle : EditPostState()
    object Loading : EditPostState()
    data class PostLoaded(val post: Post) : EditPostState()
    object SaveSuccess : EditPostState()
    data class Error(val message: String) : EditPostState()
}

class EditPostViewModel(
    private val postRepository: PostRepository = PostRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val mediaRepository: PostMediaRepository = PostMediaRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<EditPostState>(EditPostState.Idle)
    val state: StateFlow<EditPostState> = _state.asStateFlow()

    fun loadPost(postId: String) {
        if (postId.isBlank()) {
            _state.value = EditPostState.Error("Post ID is missing")
            return
        }
        _state.value = EditPostState.Loading
        viewModelScope.launch {
            postRepository.getPostById(postId).fold(
                onSuccess = { post ->
                    _state.value = EditPostState.PostLoaded(post)
                },
                onFailure = { error ->
                    _state.value = EditPostState.Error(error.message ?: "Failed to load post")
                }
            )
        }
    }

    fun updatePost(
        context: Context,
        post: Post,
        newMediaUri: Uri?,
        oldMediaUrl: String
    ) {
        _state.value = EditPostState.Loading
        viewModelScope.launch {
            try {
                var postToUpdate = post
                if (newMediaUri != null) {
                    val uid = authRepository.getCurrentUser()?.uid
                        ?: throw IllegalStateException("Not logged in")

                    val uploadedMedia = mediaRepository.uploadMedia(
                        contentResolver = context.contentResolver,
                        mediaUri = newMediaUri,
                        uid = uid
                    ).fold(
                        onSuccess = { it },
                        onFailure = { throw it }
                    )

                    // Hapus media lama jika berhasil upload media baru
                    if (oldMediaUrl.isNotBlank()) {
                        val oldPath = extractPathFromUrl(oldMediaUrl)
                        if (oldPath != null) {
                            mediaRepository.deleteMedia(oldPath)
                        }
                    }

                    postToUpdate = post.copy(
                        mediaUrl = uploadedMedia.publicUrl,
                        mediaType = uploadedMedia.mediaType
                    )
                }

                postRepository.updatePost(postToUpdate).fold(
                    onSuccess = {
                        _state.value = EditPostState.SaveSuccess
                    },
                    onFailure = { error ->
                        throw error
                    }
                )
            } catch (e: Exception) {
                _state.value = EditPostState.Error(e.message ?: "Save failed")
            }
        }
    }

    private fun extractPathFromUrl(url: String): String? {
        val keyword = "/adoptus-post-images/"
        val index = url.indexOf(keyword)
        return if (index != -1) {
            url.substring(index + keyword.length)
        } else {
            null
        }
    }
}
