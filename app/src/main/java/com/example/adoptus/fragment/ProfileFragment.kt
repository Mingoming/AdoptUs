package com.example.adoptus.fragment

import android.content.Intent
import android.net.Uri
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
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
    private val postAdapter = ProfilePostAdapter { post ->
        val bundle = Bundle().apply {
            putString("postId", post.postId)
        }
        findNavController().navigate(R.id.action_profile_to_detail, bundle)
    }

    private lateinit var topAccountName: TextView
    private lateinit var profileName: TextView
    private lateinit var bio: TextView
    private lateinit var location: TextView
    private lateinit var whatsapp: TextView
    private lateinit var postCount: TextView
    private lateinit var message: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var avatar: ShapeableImageView
    private lateinit var swipeRefresh: SwipeRefreshLayout

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
        swipeRefresh = view.findViewById(R.id.swipeRefresh)

        swipeRefresh.setColorSchemeColors(
            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_orange)
        )
        swipeRefresh.setOnRefreshListener {
            viewModel.refreshProfile()
        }

        view.findViewById<ImageButton>(R.id.btnSetting).setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_setting)
        }

        view.findViewById<ImageButton>(R.id.btnInbox).setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_inbox)
        }

        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
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
        val userId = arguments?.getString("userId")
        viewModel.loadProfile(userId)

        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val isOwnProfile = userId == null || userId == currentUid
        view?.findViewById<ImageButton>(R.id.btnSetting)?.visibility = if (isOwnProfile) View.VISIBLE else View.GONE
        view?.findViewById<ImageButton>(R.id.btnInbox)?.visibility = if (isOwnProfile) View.VISIBLE else View.GONE
        view?.findViewById<ImageButton>(R.id.btnBack)?.visibility = if (isOwnProfile) View.GONE else View.VISIBLE
    }

    private fun render(state: ProfileUiState) {
        swipeRefresh.isRefreshing = state.isLoading && swipeRefresh.isRefreshing
        progressBar.visibility = if (state.isLoading && !swipeRefresh.isRefreshing) View.VISIBLE else View.GONE

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

            if (user.whatsapp.isNotBlank()) {
                whatsapp.setOnClickListener {
                    val cleanedNum = user.whatsapp.replace(Regex("[^0-9+]"), "")
                    val formattedNum = when {
                        cleanedNum.startsWith("0") -> "62" + cleanedNum.substring(1)
                        cleanedNum.startsWith("+") -> cleanedNum.substring(1)
                        else -> cleanedNum
                    }
                    val targetUrl = "https://wa.me/$formattedNum"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                    startActivity(intent)
                }
                whatsapp.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_orange))
            } else {
                whatsapp.setOnClickListener(null)
                whatsapp.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.light_gray))
            }

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
