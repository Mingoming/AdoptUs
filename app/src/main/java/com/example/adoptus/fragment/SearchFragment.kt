package com.example.adoptus.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adoptus.MainActivity
import com.example.adoptus.R
import com.google.android.material.tabs.TabLayout

class SearchFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        val btnBackSearch = view.findViewById<ImageView>(R.id.btnBackSearch)
        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val tabLayoutFilter = view.findViewById<TabLayout>(R.id.tabLayoutFilter)
        val rvExploreGrid = view.findViewById<RecyclerView>(R.id.rvExploreGrid)
        val scrollResults = view.findViewById<View>(R.id.scrollResults)

        val layoutAccountsSection = view.findViewById<View>(R.id.layoutAccountsSection)
        val layoutPostsSection = view.findViewById<View>(R.id.layoutPostsSection)
        val rvResultsAccounts = view.findViewById<RecyclerView>(R.id.rvResultsAccounts)
        val rvResultsPosts = view.findViewById<RecyclerView>(R.id.rvResultsPosts)

        // --- DATA MOCK / DUMMY ---
        val dummyExplorePosts = listOf(
            SearchPostItem(R.drawable.placeholder), SearchPostItem(R.drawable.placeholder),
            SearchPostItem(R.drawable.placeholder), SearchPostItem(R.drawable.placeholder),
            SearchPostItem(R.drawable.placeholder), SearchPostItem(R.drawable.placeholder),
            SearchPostItem(R.drawable.placeholder), SearchPostItem(R.drawable.placeholder),
            SearchPostItem(R.drawable.placeholder)
        )

        val dummyAccounts = listOf(
            SearchAccountItem("stray_rescue_mataram", "Followed by kkn_darek + 5 more"),
            SearchAccountItem("cat_lover_lombok", "Popular shelter around you"),
            SearchAccountItem("dog_adoption_id", "Verified Organization")
        )

        // SETUP ADAPTER AWAL
        rvExploreGrid.layoutManager = GridLayoutManager(context, 3)
        rvExploreGrid.adapter = SimpleGridAdapter(dummyExplorePosts)

        rvResultsAccounts.layoutManager = LinearLayoutManager(context)
        rvResultsAccounts.adapter = SearchAccountAdapter(dummyAccounts)

        rvResultsPosts.layoutManager = GridLayoutManager(context, 3)
        rvResultsPosts.adapter = SimpleGridAdapter(dummyExplorePosts)

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
                    (activity as? MainActivity)?.setBottomNavVisibility(false)

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

            // 💥 SEMBUNYIKAN TOMBOL BACK & MUNCULKAN NAVBAR UTAMA KEMBALI
            btnBackSearch.visibility = View.GONE
            (activity as? MainActivity)?.setBottomNavVisibility(true)

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
}

// DATA CLASS & ADAPTERS KUSTOM
data class SearchPostItem(val imageRes: Int)
data class SearchAccountItem(val username: String, val detail: String)

class SimpleGridAdapter(private val list: List<SearchPostItem>) : RecyclerView.Adapter<SimpleGridAdapter.ViewHolder>() {
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val ivPhoto: ImageView = v.findViewById(R.id.ivPetPhoto)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.profile_item_pet, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.ivPhoto.setImageResource(list[position].imageRes)
    }
    override fun getItemCount(): Int = list.size
}

class SearchAccountAdapter(private val list: List<SearchAccountItem>) : RecyclerView.Adapter<SearchAccountAdapter.ViewHolder>() {
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvUsername: TextView = v.findViewById(R.id.tvUsername)
        //val tvSubDetail: TextView = v.findViewById(R.id.tvSubDetail)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.search_result, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvUsername.text = list[position].username
        //holder.tvSubDetail.text = list[position].detail
    }
    override fun getItemCount(): Int = list.size
}