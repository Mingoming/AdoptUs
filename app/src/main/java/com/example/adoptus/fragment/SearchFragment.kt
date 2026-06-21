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

    private lateinit var exploreAdapter: SearchPostAdapter
    private lateinit var resultsPostsAdapter: SearchPostAdapter
    private lateinit var resultsAccountsAdapter: SearchUserAdapter

    private lateinit var rvExploreGrid: RecyclerView
    private lateinit var rvResultsAccounts: RecyclerView
    private lateinit var rvResultsPosts: RecyclerView
    private lateinit var tabLayoutFilter: TabLayout
    private lateinit var scrollResults: View
    private lateinit var layoutAccountsSection: View
    private lateinit var layoutPostsSection: View

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

        layoutAccountsSection = view.findViewById(R.id.layoutAccountsSection)
        layoutPostsSection = view.findViewById(R.id.layoutPostsSection)
        rvResultsAccounts = view.findViewById(R.id.rvResultsAccounts)
        rvResultsPosts = view.findViewById(R.id.rvResultsPosts)

        // Setup adapters
        exploreAdapter = SearchPostAdapter { post -> navigateToDetail(post.postId) }
        resultsPostsAdapter = SearchPostAdapter { post -> navigateToDetail(post.postId) }
        resultsAccountsAdapter = SearchUserAdapter { user ->
            // Menampilkan info profile user lain (bisa disesuaikan atau show toast)
            Toast.makeText(context, "Username: @${user.username}", Toast.LENGTH_SHORT).show()
        }

        // Setup RecyclerViews
        rvExploreGrid.layoutManager = GridLayoutManager(context, 3)
        rvExploreGrid.adapter = exploreAdapter

        rvResultsAccounts.layoutManager = LinearLayoutManager(context)
        rvResultsAccounts.adapter = resultsAccountsAdapter

        rvResultsPosts.layoutManager = GridLayoutManager(context, 3)
        rvResultsPosts.adapter = resultsPostsAdapter

        // Load explore posts awal (available posts)
        loadExplorePosts()

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

                    // Jalankan query pencarian dinamis
                    performSearch(query)

                    // Pindah ke mode hasil pencarian
                    rvExploreGrid.visibility = View.GONE
                    tabLayoutFilter.visibility = View.VISIBLE
                    scrollResults.visibility = View.VISIBLE
                    tabLayoutFilter.getTabAt(0)?.select()
                }
                return@setOnEditorActionListener true
            }
            false
        }

        // SAAT TOMBOL BACK DIKLIK (KEMBALI KE PAGE SEARCH AWAL)
        btnBackSearch.setOnClickListener {
            // Bersihkan teks di search bar
            etSearch.text?.clear()

            // SEMBUNYIKAN TOMBOL BACK & MUNCULKAN NAVBAR UTAMA KEMBALI
            btnBackSearch.visibility = View.GONE
            (activity as? MainActivity)?.showBottomNav()

            // Kembalikan visual ke grid explore awal
            tabLayoutFilter.visibility = View.GONE
            scrollResults.visibility = View.GONE
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

    private fun loadExplorePosts() {
        db.collection("posts")
            .whereEqualTo("status", "available")
            .get()
            .addOnSuccessListener { snapshot ->
                val posts = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { Post.fromMap(doc.id, it) }
                }
                exploreAdapter.submitList(posts)
            }
            .addOnFailureListener { e ->
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
