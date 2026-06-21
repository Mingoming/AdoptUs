package com.example.adoptus.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.adoptus.R
import com.example.adoptus.data.model.Post
import com.example.adoptus.ui.feed.FeedAdapter
import com.example.adoptus.ui.feed.FeedViewModel
import kotlinx.coroutines.launch

class FeedFragment : Fragment() {

    private val viewModel: FeedViewModel by viewModels()
    private lateinit var adapter: FeedAdapter
    private lateinit var rvFeed: RecyclerView
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
        progressBar  = view.findViewById(R.id.progressBar)
        layoutEmpty  = view.findViewById(R.id.layoutEmpty)
        layoutError  = view.findViewById(R.id.layoutError)
        tvError      = view.findViewById(R.id.tvError)
        btnRetry     = view.findViewById(R.id.btnRetry)

        setupRecyclerView()
        observeFeed()

        btnRetry.setOnClickListener { viewModel.refresh() }
    }

    private fun setupRecyclerView() {
        adapter = FeedAdapter(
            onDetailClick = { post -> navigateToDetail(post) },
            onApplyClick  = { post -> navigateToDetail(post) }
        )

        val layoutManager = LinearLayoutManager(requireContext())
        rvFeed.layoutManager = layoutManager
        rvFeed.adapter = adapter

        // PagerSnapHelper = snap satu item per scroll (TikTok behavior)
        PagerSnapHelper().attachToRecyclerView(rvFeed)

    }

    private fun observeFeed() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.feedState.collect { state ->
                when (state) {
                    is FeedViewModel.FeedState.Loading -> {
                        progressBar.visibility  = View.VISIBLE
                        layoutEmpty.visibility  = View.GONE
                        layoutError.visibility  = View.GONE
                        rvFeed.visibility       = View.GONE
                    }
                    is FeedViewModel.FeedState.Success -> {
                        progressBar.visibility  = View.GONE
                        layoutEmpty.visibility  = View.GONE
                        layoutError.visibility  = View.GONE
                        rvFeed.visibility       = View.VISIBLE
                        adapter.submitList(state.posts)
                    }
                    is FeedViewModel.FeedState.Empty -> {
                        progressBar.visibility  = View.GONE
                        layoutEmpty.visibility  = View.VISIBLE
                        layoutError.visibility  = View.GONE
                        rvFeed.visibility       = View.GONE
                    }
                    is FeedViewModel.FeedState.Error -> {
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

}
