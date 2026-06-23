package com.example.adoptus.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.adoptus.R
import com.example.adoptus.data.repository.PostRepository
import com.example.adoptus.ui.adoptionhistory.AdoptionHistoryAdapter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AdoptionHistoryFragment : Fragment() {

    private val repository = PostRepository()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: AdoptionHistoryAdapter

    private lateinit var rvHistory: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var btnBack: ImageButton
    private var historyJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_adoption_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvHistory = view.findViewById(R.id.rvHistory)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progressBar = view.findViewById(R.id.progressBar)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        btnBack = view.findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        setupRecyclerView()
        observeMyAdoptions()

        swipeRefresh.setColorSchemeColors(
            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_orange)
        )
        swipeRefresh.setOnRefreshListener {
            observeMyAdoptions()
        }
    }

    override fun onDestroyView() {
        historyJob?.cancel()
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        adapter = AdoptionHistoryAdapter { adoption ->
            val bundle = Bundle().apply {
                putString("postId", adoption.postId)
            }
            findNavController().navigate(R.id.action_adoption_history_to_detail, bundle)
        }
        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = adapter
    }

    private fun observeMyAdoptions() {
        val currentUid = auth.currentUser?.uid ?: return
        swipeRefresh.isRefreshing = true
        historyJob?.cancel()

        historyJob = viewLifecycleOwner.lifecycleScope.launch {
            repository.getMyAdoptions(currentUid).collectLatest { result ->
                swipeRefresh.isRefreshing = false
                progressBar.visibility = View.GONE

                result.fold(
                    onSuccess = { adoptions ->
                        adapter.submitList(adoptions)
                        layoutEmpty.visibility = if (adoptions.isEmpty()) View.VISIBLE else View.GONE
                        rvHistory.visibility = if (adoptions.isEmpty()) View.GONE else View.VISIBLE
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            requireContext(),
                            error.message ?: "Failed to load adoption history",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}
