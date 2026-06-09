package com.example.adoptus.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adoptus.data.model.Post
import com.example.adoptus.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

    private val repository = PostRepository()

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
        viewModelScope.launch {
            _feedState.value = FeedState.Loading
            repository.getFeedPosts().collect { result ->
                result.fold(
                    onSuccess = { posts ->
                        _feedState.value = if (posts.isEmpty()) FeedState.Empty
                                           else FeedState.Success(posts)
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

    fun refresh() = loadFeed()
}
