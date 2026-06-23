package com.example.adoptus.ui.addpost

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

sealed class AddPostState {
    object Idle : AddPostState()
    object Loading : AddPostState()
    object Success : AddPostState()
    data class Error(val message: String) : AddPostState()
}

class AddPostViewModel(
    private val postRepository: PostRepository = PostRepository(),
    private val authRepository: AuthRepository = AuthRepository(),
    private val mediaRepository: PostMediaRepository = PostMediaRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<AddPostState>(AddPostState.Idle)
    val state: StateFlow<AddPostState> = _state.asStateFlow()

    fun createPost(
        context: Context,
        post: Post,
        selectedMediaUri: Uri?
    ) {
        _state.value = AddPostState.Loading
        viewModelScope.launch {
            try {
                val uid = authRepository.getCurrentUser()?.uid
                    ?: throw IllegalStateException("Not logged in")

                // Cek cache lokal SharedPreferences terlebih dahulu
                val cachedUser = authRepository.getCachedUserProfile(context)

                val userCity = if (cachedUser != null && cachedUser.id == uid && cachedUser.city.isNotBlank()) {
                    cachedUser.city
                } else {
                    // Fallback ke Firestore jika cache kosong
                    authRepository.getUserProfile(uid).fold(
                        onSuccess = { user ->
                            user.city.trim().ifBlank { "Indonesia (Lokasi belum diatur)" }
                        },
                        onFailure = {
                            "Indonesia (Lokasi belum diatur)"
                        }
                    )
                }

                val uploadedMedia = selectedMediaUri?.let { mediaUri ->
                    mediaRepository.uploadMedia(
                        contentResolver = context.contentResolver,
                        mediaUri = mediaUri,
                        uid = uid
                    ).fold(
                        onSuccess = { it },
                        onFailure = { throw it }
                    )
                }

                val postWithMedia = post.copy(
                    city = userCity,
                    mediaUrl = uploadedMedia?.publicUrl.orEmpty(),
                    mediaType = uploadedMedia?.mediaType ?: "image"
                )

                postRepository.createPost(postWithMedia, cachedUser).fold(
                    onSuccess = {
                        _state.value = AddPostState.Success
                    },
                    onFailure = { postError ->
                        uploadedMedia?.let { uploaded ->
                            mediaRepository.deleteMedia(uploaded.path)
                                .exceptionOrNull()
                                ?.let(postError::addSuppressed)
                        }
                        throw postError
                    }
                )
            } catch (error: Exception) {
                _state.value = AddPostState.Error(error.message ?: "Upload failed")
            }
        }
    }
}
