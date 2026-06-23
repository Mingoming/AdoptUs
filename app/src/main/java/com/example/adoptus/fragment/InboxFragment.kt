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
import com.example.adoptus.data.model.Adoption
import com.example.adoptus.data.repository.PostRepository
import com.example.adoptus.ui.inbox.InboxAdapter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InboxFragment : Fragment() {

    private val repository = PostRepository()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: InboxAdapter

    private lateinit var rvRequests: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var btnBack: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_inbox, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvRequests   = view.findViewById(R.id.rvRequests)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progressBar  = view.findViewById(R.id.progressBar)
        layoutEmpty  = view.findViewById(R.id.layoutEmpty)
        btnBack      = view.findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        setupRecyclerView()
        observeIncomingAdoptions()

        swipeRefresh.setColorSchemeColors(
            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_orange)
        )
        swipeRefresh.setOnRefreshListener {
            observeIncomingAdoptions()
        }
    }

    private fun setupRecyclerView() {
        adapter = InboxAdapter(
            onApproveClick = { adoption -> updateStatus(adoption, "approved") },
            onRejectClick  = { adoption -> updateStatus(adoption, "rejected") }
        )
        rvRequests.layoutManager = LinearLayoutManager(requireContext())
        rvRequests.adapter = adapter
    }

    private var observeJob: kotlinx.coroutines.Job? = null

    private fun observeIncomingAdoptions() {
        val currentUid = auth.currentUser?.uid ?: return
        swipeRefresh.isRefreshing = true

        observeJob?.cancel()
        observeJob = viewLifecycleOwner.lifecycleScope.launch {
            repository.getIncomingAdoptions(currentUid).collectLatest { result ->
                swipeRefresh.isRefreshing = false
                progressBar.visibility = View.GONE

                result.fold(
                    onSuccess = { adoptions ->
                        if (adoptions.isEmpty()) {
                            layoutEmpty.visibility = View.VISIBLE
                            rvRequests.visibility = View.GONE
                        } else {
                            layoutEmpty.visibility = View.GONE
                            rvRequests.visibility = View.VISIBLE
                            adapter.submitList(adoptions)
                        }
                    },
                    onFailure = { error ->
                        Toast.makeText(requireContext(), error.message ?: "Failed to load requests", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun updateStatus(adoption: Adoption, status: String) {
        progressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val result = repository.updateAdoptionStatus(adoption.adoptionId, adoption.postId, status)
            progressBar.visibility = View.GONE
            result.fold(
                onSuccess = {
                    val msg = if (status == "approved") "Application approved!" else "Application rejected!"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                },
                onFailure = { error ->
                    Toast.makeText(requireContext(), error.message ?: "Failed to update status", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
