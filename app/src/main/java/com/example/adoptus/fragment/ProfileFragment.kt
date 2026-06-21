package com.example.adoptus.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.adoptus.R
import com.example.adoptus.ui.profile.ProfilePostAdapter
import com.example.adoptus.ui.profile.ProfileUiState
import com.example.adoptus.ui.profile.ProfileViewModel
import com.example.adoptus.ui.profile.profileDisplayName
import com.example.adoptus.ui.profile.profileLocation
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()
    private val postAdapter = ProfilePostAdapter()

    private lateinit var topAccountName: TextView
    private lateinit var profileName: TextView
    private lateinit var bio: TextView
    private lateinit var location: TextView
    private lateinit var whatsapp: TextView
    private lateinit var postCount: TextView
    private lateinit var message: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var avatar: ShapeableImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        topAccountName = view.findViewById(R.id.tvTopAccountName)
        profileName = view.findViewById(R.id.tvProfileName)
        bio = view.findViewById(R.id.tvBio)
        location = view.findViewById(R.id.tvLocation)
        whatsapp = view.findViewById(R.id.tvWhatsapp)
        postCount = view.findViewById(R.id.tvPostCount)
        message = view.findViewById(R.id.tvProfileMessage)
        progressBar = view.findViewById(R.id.profileProgressBar)
        avatar = view.findViewById(R.id.ivAvatar)

        view.findViewById<ImageButton>(R.id.btnSetting).setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_setting)
        }

        view.findViewById<RecyclerView>(R.id.rvPetGrid).apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = postAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect(::render)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshProfile()
    }

    private fun render(state: ProfileUiState) {
        progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

        state.user?.let { user ->
            topAccountName.text = user.username
            profileName.text = user.profileDisplayName()
            bio.text = user.bio.ifBlank { "No bio yet" }

            val city = user.profileLocation()
            location.text = city
            location.visibility = if (city.isBlank()) View.GONE else View.VISIBLE

            whatsapp.text = "WhatsApp: ${user.whatsapp}"
            whatsapp.visibility =
                if (user.whatsapp.isBlank()) View.GONE else View.VISIBLE

            if (user.photoUrl.isNotBlank()) {
                avatar.load(user.photoUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_profile_placeholder)
                    error(R.drawable.ic_profile_placeholder)
                }
            } else {
                avatar.setImageResource(R.drawable.ic_profile_placeholder)
            }
        }

        postCount.text = state.posts.size.toString()
        postAdapter.submitList(state.posts)

        val messageText = when {
            state.profileError != null -> state.profileError
            state.postsError != null -> state.postsError
            !state.isLoading && state.posts.isEmpty() -> "No posts yet"
            else -> null
        }
        message.text = messageText.orEmpty()
        message.visibility = if (messageText == null) View.GONE else View.VISIBLE
    }
}
