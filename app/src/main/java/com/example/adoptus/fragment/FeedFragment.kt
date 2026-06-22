package com.example.adoptus.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.adoptus.R
import com.example.adoptus.data.model.Post
import com.example.adoptus.data.repository.PostRepository
import com.example.adoptus.ui.feed.FeedAdapter
import com.example.adoptus.ui.feed.FeedViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class FeedFragment : Fragment() {

    private val viewModel: FeedViewModel by viewModels()
    private val postRepository = PostRepository()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: FeedAdapter
    private lateinit var rvFeed: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var layoutError: LinearLayout
    private lateinit var tvError: TextView
    private lateinit var btnRetry: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvFeed       = view.findViewById(R.id.rvFeed)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progressBar  = view.findViewById(R.id.progressBar)
        layoutEmpty  = view.findViewById(R.id.layoutEmpty)
        layoutError  = view.findViewById(R.id.layoutError)
        tvError      = view.findViewById(R.id.tvError)
        btnRetry     = view.findViewById(R.id.btnRetry)

        setupRecyclerView()
        observeFeed()

        // Configure SwipeRefreshLayout spinner color matching theme orange
        swipeRefresh.setColorSchemeColors(
            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_orange)
        )
        swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        btnRetry.setOnClickListener { viewModel.refresh() }
    }

    override fun onPause() {
        if (::adapter.isInitialized) {
            adapter.pauseAllPlayers()
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }

    private fun setupRecyclerView() {
        adapter = FeedAdapter(
            onDetailClick = { post -> navigateToDetail(post) },
            onApplyClick  = { post -> applyAdoption(post) },
            onOwnerClick  = { post -> navigateToOwnerProfile(post.userId) },
            onLikeClick   = { post -> viewModel.toggleLike(post) }
        )

        val layoutManager = LinearLayoutManager(requireContext())
        rvFeed.layoutManager = layoutManager
        rvFeed.adapter = adapter

        // PagerSnapHelper = snap satu item per scroll (TikTok behavior)
        PagerSnapHelper().attachToRecyclerView(rvFeed)
    }

    private fun applyAdoption(post: Post) {
        val currentUid = auth.currentUser?.uid ?: return
        if (post.userId == currentUid) {
            Toast.makeText(context, "You cannot adopt your own pet!", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Adopt ${post.petName}")
            .setMessage("Are you sure you want to send an adoption request for ${post.petName}?")
            .setPositiveButton("Yes") { _, _ ->
                progressBar.visibility = View.VISIBLE
                viewLifecycleOwner.lifecycleScope.launch {
                    val pendingResult = postRepository.checkPendingAdoption(post.postId, currentUid)
                    pendingResult.fold(
                        onSuccess = { alreadyPending ->
                            if (alreadyPending) {
                                progressBar.visibility = View.GONE
                                Toast.makeText(context, "You already have a pending application for this pet!", Toast.LENGTH_SHORT).show()
                            } else {
                                val applyResult = postRepository.applyForAdoption(
                                    post.postId,
                                    post.petName,
                                    post.userId,
                                    currentUid
                                )
                                progressBar.visibility = View.GONE
                                applyResult.fold(
                                    onSuccess = {
                                        Toast.makeText(context, "Application sent successfully!", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(context, error.message ?: "Failed to send application", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        onFailure = { error ->
                            progressBar.visibility = View.GONE
                            Toast.makeText(context, error.message ?: "Failed to verify application status", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeFeed() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.feedState.collect { state ->
                when (state) {
                    is FeedViewModel.FeedState.Loading -> {
                        if (!swipeRefresh.isRefreshing) {
                            progressBar.visibility  = View.VISIBLE
                        }
                        layoutEmpty.visibility  = View.GONE
                        layoutError.visibility  = View.GONE
                        if (!swipeRefresh.isRefreshing) {
                            rvFeed.visibility       = View.GONE
                        }
                    }
                    is FeedViewModel.FeedState.Success -> {
                        swipeRefresh.isRefreshing = false
                        progressBar.visibility  = View.GONE
                        layoutEmpty.visibility  = View.GONE
                        layoutError.visibility  = View.GONE
                        rvFeed.visibility       = View.VISIBLE
                        adapter.submitList(state.posts)
                    }
                    is FeedViewModel.FeedState.Empty -> {
                        swipeRefresh.isRefreshing = false
                        progressBar.visibility  = View.GONE
                        layoutEmpty.visibility  = View.VISIBLE
                        layoutError.visibility  = View.GONE
                        rvFeed.visibility       = View.GONE
                    }
                    is FeedViewModel.FeedState.Error -> {
                        swipeRefresh.isRefreshing = false
                        progressBar.visibility  = View.GONE
                        layoutEmpty.visibility  = View.GONE
                        layoutError.visibility  = View.VISIBLE
                        rvFeed.visibility       = View.GONE
                        tvError.text = state.message
                    }
                }
            }
        }
    }

    private fun navigateToDetail(post: Post) {
        val bundle = android.os.Bundle().apply {
            putString("postId", post.postId)
        }
        findNavController().navigate(R.id.action_feed_to_detail, bundle)
    }

    private fun navigateToOwnerProfile(userId: String) {
        val bundle = android.os.Bundle().apply {
            putString("userId", userId)
        }
        findNavController().navigate(R.id.action_feed_to_profile, bundle)
    }

}
