package com.example.adoptus.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adoptus.MainActivity
import com.example.adoptus.R
import com.example.adoptus.data.model.Post
import com.example.adoptus.data.model.User
import com.example.adoptus.ui.search.SearchPostAdapter
import com.example.adoptus.ui.search.SearchUserAdapter
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore

class SearchFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()

    private val PREFS_NAME = "adoptus_search_prefs"
    private val KEY_HISTORY = "search_history"

    private lateinit var exploreAdapter: SearchPostAdapter
    private lateinit var resultsPostsAdapter: SearchPostAdapter
    private lateinit var resultsAccountsAdapter: SearchUserAdapter
    private lateinit var historyAdapter: com.example.adoptus.ui.search.SearchHistoryAdapter

    private lateinit var rvExploreGrid: RecyclerView
    private lateinit var rvResultsAccounts: RecyclerView
    private lateinit var rvResultsPosts: RecyclerView
    private lateinit var rvSearchHistory: RecyclerView
    private lateinit var tabLayoutFilter: TabLayout
    private lateinit var scrollResults: View
    private lateinit var layoutHistorySection: View
    private lateinit var layoutAccountsSection: View
    private lateinit var layoutPostsSection: View

    private var searchHistory = mutableListOf<String>()

    private val explorePostsList = mutableListOf<Post>()
    private var lastExploreVisible: com.google.firebase.firestore.DocumentSnapshot? = null
    private var isExploreLoading = false
    private var isExploreLastPage = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        val btnBackSearch = view.findViewById<ImageView>(R.id.btnBackSearch)
        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        tabLayoutFilter = view.findViewById(R.id.tabLayoutFilter)
        rvExploreGrid = view.findViewById(R.id.rvExploreGrid)
        scrollResults = view.findViewById(R.id.scrollResults)

        layoutHistorySection = view.findViewById(R.id.layoutHistorySection)
        rvSearchHistory = view.findViewById(R.id.rvSearchHistory)
        val btnClearAllHistory = view.findViewById<View>(R.id.btnClearAllHistory)

        layoutAccountsSection = view.findViewById(R.id.layoutAccountsSection)
        layoutPostsSection = view.findViewById(R.id.layoutPostsSection)
        rvResultsAccounts = view.findViewById(R.id.rvResultsAccounts)
        rvResultsPosts = view.findViewById(R.id.rvResultsPosts)

        // Setup adapters
        exploreAdapter = SearchPostAdapter { post -> navigateToDetail(post.postId) }
        resultsPostsAdapter = SearchPostAdapter { post -> navigateToDetail(post.postId) }
        resultsAccountsAdapter = SearchUserAdapter { user ->
            // Simpan pencarian ke history
            saveQueryToHistory(user.username)
            val bundle = Bundle().apply {
                putString("userId", user.id)
            }
            findNavController().navigate(R.id.action_search_to_profile, bundle)
        }

        // Setup RecyclerViews
        val exploreLayoutManager = GridLayoutManager(context, 3)
        rvExploreGrid.layoutManager = exploreLayoutManager
        rvExploreGrid.adapter = exploreAdapter

        rvExploreGrid.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val totalItemCount = exploreLayoutManager.itemCount
                val lastVisibleItem = exploreLayoutManager.findLastVisibleItemPosition()
                if (lastVisibleItem >= totalItemCount - 3) {
                    loadExplorePosts()
                }
            }
        })

        rvResultsAccounts.layoutManager = LinearLayoutManager(context)
        rvResultsAccounts.adapter = resultsAccountsAdapter

        rvResultsPosts.layoutManager = GridLayoutManager(context, 3)
        rvResultsPosts.adapter = resultsPostsAdapter

        // Load History data
        loadSearchHistory()
        historyAdapter = com.example.adoptus.ui.search.SearchHistoryAdapter(
            searchHistory,
            onHistoryClick = { query ->
                etSearch.setText(query)
                etSearch.setSelection(query.length)
                performSearch(query)
                showSearchResultsView()
            },
            onRemoveClick = { query ->
                removeQueryFromHistory(query)
            }
        )
        rvSearchHistory.layoutManager = LinearLayoutManager(context)
        rvSearchHistory.adapter = historyAdapter

        btnClearAllHistory.setOnClickListener {
            clearAllHistory()
        }

        // Load explore posts awal (available posts)
        loadExplorePosts()

        // Focus listener for search view to trigger recent history layout
        etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                btnBackSearch.visibility = View.VISIBLE
                (activity as? MainActivity)?.hideBottomNav()

                if (etSearch.text.toString().trim().isEmpty()) {
                    showHistoryView()
                }
            }
        }

        etSearch.setOnClickListener {
            btnBackSearch.visibility = View.VISIBLE
            (activity as? MainActivity)?.hideBottomNav()

            if (etSearch.text.toString().trim().isEmpty()) {
                showHistoryView()
            }
        }

        // DETEKSI KLIK ENTER DI KEYBOARD
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                val query = etSearch.text.toString().trim()

                if (query.isNotEmpty()) {
                    // Sembunyikan keyboard
                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(etSearch.windowToken, 0)

                    // TAMPILKAN TOMBOL BACK & SEMBUNYIKAN NAVBAR UTAMA
                    btnBackSearch.visibility = View.VISIBLE
                    (activity as? MainActivity)?.hideBottomNav()

                    // Simpan pencarian ke history
                    saveQueryToHistory(query)

                    // Jalankan query pencarian dinamis
                    performSearch(query)

                    // Pindah ke mode hasil pencarian
                    showSearchResultsView()
                }
                return@setOnEditorActionListener true
            }
            false
        }

        // SAAT TOMBOL BACK DIKLIK (KEMBALI KE PAGE SEARCH AWAL)
        btnBackSearch.setOnClickListener {
            // Bersihkan teks di search bar
            etSearch.text?.clear()
            etSearch.clearFocus()

            // Sembunyikan keyboard
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etSearch.windowToken, 0)

            // SEMBUNYIKAN TOMBOL BACK & MUNCULKAN NAVBAR UTAMA KEMBALI
            btnBackSearch.visibility = View.GONE
            (activity as? MainActivity)?.showBottomNav()

            // Kembalikan visual ke grid explore awal
            tabLayoutFilter.visibility = View.GONE
            scrollResults.visibility = View.GONE
            layoutHistorySection.visibility = View.GONE
            rvExploreGrid.visibility = View.VISIBLE
        }

        // FILTER TAB LAYOUT
        tabLayoutFilter.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // Tab "All"
                        layoutAccountsSection.visibility = View.VISIBLE
                        layoutPostsSection.visibility = View.VISIBLE
                    }
                    1 -> { // Tab "Accounts" -> Munculkan akun, sembunyikan postingan
                        layoutAccountsSection.visibility = View.VISIBLE
                        layoutPostsSection.visibility = View.GONE
                    }
                    2 -> { // Tab "Posts" -> Sembunyikan akun, munculkan postingan
                        layoutAccountsSection.visibility = View.GONE
                        layoutPostsSection.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        return view
    }

    private fun showHistoryView() {
        rvExploreGrid.visibility = View.GONE
        tabLayoutFilter.visibility = View.GONE
        scrollResults.visibility = View.GONE
        layoutHistorySection.visibility = if (searchHistory.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun showSearchResultsView() {
        rvExploreGrid.visibility = View.GONE
        layoutHistorySection.visibility = View.GONE
        tabLayoutFilter.visibility = View.VISIBLE
        scrollResults.visibility = View.VISIBLE
        tabLayoutFilter.getTabAt(0)?.select()
    }

    private fun loadSearchHistory() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val historySet = prefs.getStringSet(KEY_HISTORY, emptySet()) ?: emptySet()
        searchHistory = historySet.toMutableList()
    }

    private fun saveQueryToHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        searchHistory.remove(trimmed)
        searchHistory.add(0, trimmed)
        if (searchHistory.size > 15) {
            searchHistory.removeAt(searchHistory.size - 1)
        }

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_HISTORY, searchHistory.toSet()).apply()
        historyAdapter.updateList(searchHistory)
    }

    private fun removeQueryFromHistory(query: String) {
        searchHistory.remove(query)
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_HISTORY, searchHistory.toSet()).apply()
        historyAdapter.updateList(searchHistory)

        if (searchHistory.isEmpty()) {
            layoutHistorySection.visibility = View.GONE
        }
    }

    private fun clearAllHistory() {
        searchHistory.clear()
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
        historyAdapter.updateList(searchHistory)
        layoutHistorySection.visibility = View.GONE
    }

    private fun loadExplorePosts(isRefresh: Boolean = false) {
        if (isExploreLoading || (isExploreLastPage && !isRefresh)) return
        isExploreLoading = true

        if (isRefresh) {
            explorePostsList.clear()
            lastExploreVisible = null
            isExploreLastPage = false
        }

        var query = db.collection("posts")
            .whereEqualTo("status", "available")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(15)

        lastExploreVisible?.let {
            query = query.startAfter(it)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                isExploreLoading = false
                val posts = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { Post.fromMap(doc.id, it) }
                }
                if (posts.size < 15) {
                    isExploreLastPage = true
                }
                explorePostsList.addAll(posts)
                exploreAdapter.submitList(explorePostsList.toList())
                lastExploreVisible = snapshot.documents.lastOrNull()
            }
            .addOnFailureListener { e ->
                isExploreLoading = false
                Toast.makeText(context, "Error loading explore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun performSearch(query: String) {
        val lowerQuery = query.lowercase()

        // 1. Cari Post berdasarkan petName (lowercase / containing search di client side agar fleksibel)
        db.collection("posts")
            .whereEqualTo("status", "available")
            .get()
            .addOnSuccessListener { snapshot ->
                val allPosts = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { Post.fromMap(doc.id, it) }
                }
                val filteredPosts = allPosts.filter {
                    it.petName.lowercase().contains(lowerQuery) ||
                            it.breed.lowercase().contains(lowerQuery) ||
                            it.petType.lowercase().contains(lowerQuery)
                }
                resultsPostsAdapter.submitList(filteredPosts)
            }

        // 2. Cari Accounts berdasarkan username / fullName
        db.collection("users")
            .get()
            .addOnSuccessListener { snapshot ->
                val allUsers = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { User.fromMap(doc.id, it) }
                }
                val filteredUsers = allUsers.filter {
                    it.username.lowercase().contains(lowerQuery) ||
                            it.fullName.lowercase().contains(lowerQuery)
                }
                resultsAccountsAdapter.submitList(filteredUsers)
            }
    }

    private fun navigateToDetail(postId: String) {
        val bundle = Bundle().apply {
            putString("postId", postId)
        }
        findNavController().navigate(R.id.action_search_to_detail, bundle)
    }
}
