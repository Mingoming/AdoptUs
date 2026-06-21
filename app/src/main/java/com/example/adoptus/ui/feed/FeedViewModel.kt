package com.example.adoptus.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adoptus.data.model.Post
import com.example.adoptus.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.google.firebase.auth.FirebaseAuth

class FeedViewModel : ViewModel() {

    private val repository = PostRepository()
    private val likedPostIds = mutableSetOf<String>()

    // State untuk list post di feed
    sealed class FeedState {
        object Loading                             : FeedState()
        data class Success(val posts: List<Post>)  : FeedState()
        data class Error(val message: String)      : FeedState()
        object Empty                               : FeedState()
    }

    private val _feedState = MutableStateFlow<FeedState>(FeedState.Loading)
    val feedState: StateFlow<FeedState> = _feedState.asStateFlow()

    init {
        loadFeed()
    }

    fun loadFeed() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        viewModelScope.launch {
            _feedState.value = FeedState.Loading
            
            if (currentUid != null) {
                repository.getLikedPostIds(currentUid).fold(
                    onSuccess = { ids ->
                        likedPostIds.clear()
                        likedPostIds.addAll(ids)
                    },
                    onFailure = {
                        // Toleransi kegagalan load likes, biarkan kosong
                    }
                )
            }

            repository.getFeedPosts().collect { result ->
                result.fold(
                    onSuccess = { posts ->
                        val mappedPosts = posts.map { post ->
                            post.copy(isLikedByCurrentUser = likedPostIds.contains(post.postId))
                        }
                        _feedState.value = if (mappedPosts.isEmpty()) FeedState.Empty
                                           else FeedState.Success(mappedPosts)
                    },
                    onFailure = {
                        _feedState.value = FeedState.Error(
                            it.message ?: "Gagal memuat feed"
                        )
                    }
                )
            }
        }
    }

    fun toggleLike(post: Post) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            val wasLiked = likedPostIds.contains(post.postId)
            if (wasLiked) {
                likedPostIds.remove(post.postId)
            } else {
                likedPostIds.add(post.postId)
            }

            // Optimistic UI Update
            val currentState = _feedState.value
            if (currentState is FeedState.Success) {
                val updatedPosts = currentState.posts.map {
                    if (it.postId == post.postId) {
                        it.copy(
                            isLikedByCurrentUser = !wasLiked,
                            likesCount = it.likesCount + (if (wasLiked) -1 else 1)
                        )
                    } else {
                        it
                    }
                }
                _feedState.value = FeedState.Success(updatedPosts)
            }

            // Persist ke Firestore
            val result = repository.toggleLike(post.postId, currentUid)
            result.onFailure {
                // Revert jika gagal
                if (wasLiked) {
                    likedPostIds.add(post.postId)
                } else {
                    likedPostIds.remove(post.postId)
                }

                val revertState = _feedState.value
                if (revertState is FeedState.Success) {
                    val revertedPosts = revertState.posts.map {
                        if (it.postId == post.postId) {
                            it.copy(
                                isLikedByCurrentUser = wasLiked,
                                likesCount = it.likesCount + (if (wasLiked) 1 else -1)
                            )
                        } else {
                            it
                        }
                    }
                    _feedState.value = FeedState.Success(revertedPosts)
                }
            }
        }
    }

    fun refresh() = loadFeed()
}
