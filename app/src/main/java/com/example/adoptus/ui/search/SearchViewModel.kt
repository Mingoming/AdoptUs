package com.example.adoptus.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adoptus.data.model.Post
import com.example.adoptus.data.model.User
import com.example.adoptus.data.repository.AuthRepository
import com.example.adoptus.data.repository.PostRepository
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val postRepository: PostRepository = PostRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _explorePosts = MutableStateFlow<List<Post>>(emptyList())
    val explorePosts: StateFlow<List<Post>> = _explorePosts.asStateFlow()

    private val _searchResultsPosts = MutableStateFlow<List<Post>>(emptyList())
    val searchResultsPosts: StateFlow<List<Post>> = _searchResultsPosts.asStateFlow()

    private val _searchResultsUsers = MutableStateFlow<List<User>>(emptyList())
    val searchResultsUsers: StateFlow<List<User>> = _searchResultsUsers.asStateFlow()

    private val _isExploreLoading = MutableStateFlow(false)
    val isExploreLoading: StateFlow<Boolean> = _isExploreLoading.asStateFlow()

    private val _exploreError = MutableStateFlow<String?>(null)
    val exploreError: StateFlow<String?> = _exploreError.asStateFlow()

    private var lastExploreVisible: DocumentSnapshot? = null
    var isExploreLastPage = false
        private set

    fun loadExplorePosts(isRefresh: Boolean = false) {
        if (_isExploreLoading.value || (isExploreLastPage && !isRefresh)) return
        _isExploreLoading.value = true
        _exploreError.value = null

        if (isRefresh) {
            _explorePosts.value = emptyList()
            lastExploreVisible = null
            isExploreLastPage = false
        }

        viewModelScope.launch {
            postRepository.getFeedPostsPaginated(15, lastExploreVisible).fold(
                onSuccess = { (posts, lastDoc) ->
                    _isExploreLoading.value = false
                    lastExploreVisible = lastDoc
                    if (posts.size < 15) {
                        isExploreLastPage = true
                    }
                    _explorePosts.value = _explorePosts.value + posts
                },
                onFailure = { e ->
                    _isExploreLoading.value = false
                    _exploreError.value = e.message ?: "Failed to load explore posts"
                }
            )
        }
    }

    fun performSearch(query: String) {
        val lowerQuery = query.lowercase().trim()
        if (lowerQuery.isEmpty()) return

        viewModelScope.launch {
            postRepository.getFeedPosts().collect { result ->
                result.fold(
                    onSuccess = { allPosts ->
                        val filteredPosts = allPosts.filter {
                            it.petName.lowercase().contains(lowerQuery) ||
                                    it.breed.lowercase().contains(lowerQuery) ||
                                    it.petType.lowercase().contains(lowerQuery)
                        }
                        _searchResultsPosts.value = filteredPosts
                    },
                    onFailure = {
                        _searchResultsPosts.value = emptyList()
                    }
                )
            }
        }

        viewModelScope.launch {
            authRepository.getAllUsers().fold(
                onSuccess = { allUsers ->
                    val filteredUsers = allUsers.filter {
                        it.username.lowercase().contains(lowerQuery) ||
                                it.fullName.lowercase().contains(lowerQuery)
                    }
                    _searchResultsUsers.value = filteredUsers
                },
                onFailure = {
                    _searchResultsUsers.value = emptyList()
                }
            )
        }
    }
}
