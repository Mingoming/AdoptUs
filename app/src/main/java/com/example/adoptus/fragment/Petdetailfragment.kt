package com.example.adoptus.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.example.adoptus.R
import com.example.adoptus.data.model.Post
import com.example.adoptus.ui.detail.PetDetailState
import com.example.adoptus.ui.detail.PetDetailViewModel
import com.example.adoptus.ui.detail.detailBreedAge
import com.example.adoptus.ui.detail.detailFee
import com.example.adoptus.ui.detail.hasDetailImage
import kotlinx.coroutines.launch

class PetDetailFragment : Fragment() {

    private val args: PetDetailFragmentArgs by navArgs()
    private val viewModel: PetDetailViewModel by viewModels()

    private lateinit var progressBar: ProgressBar
    private lateinit var content: NestedScrollView
    private lateinit var errorLayout: LinearLayout
    private lateinit var errorText: TextView
    private lateinit var media: ImageView
    private lateinit var petName: TextView
    private lateinit var breedAge: TextView
    private lateinit var city: TextView
    private lateinit var status: TextView
    private lateinit var fee: TextView
    private lateinit var description: TextView
    private lateinit var vaccinated: TextView
    private lateinit var healthPassport: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_petdetail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.detailProgressBar)
        content = view.findViewById(R.id.detailContent)
        errorLayout = view.findViewById(R.id.detailErrorLayout)
        errorText = view.findViewById(R.id.tvDetailError)
        media = view.findViewById(R.id.ivPetMedia)
        petName = view.findViewById(R.id.tvPetName)
        breedAge = view.findViewById(R.id.tvBreedAge)
        city = view.findViewById(R.id.tvCity)
        status = view.findViewById(R.id.tvStatus)
        fee = view.findViewById(R.id.tvFee)
        description = view.findViewById(R.id.tvDescription)
        vaccinated = view.findViewById(R.id.tvVaccinated)
        healthPassport = view.findViewById(R.id.tvHealthPassport)

        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }
        view.findViewById<Button>(R.id.btnRetry).setOnClickListener {
            viewModel.loadPost(args.postId)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect(::render)
        }

        viewModel.loadPost(args.postId)
    }

    private fun render(state: PetDetailState) {
        when (state) {
            PetDetailState.Loading -> {
                progressBar.visibility = View.VISIBLE
                content.visibility = View.GONE
                errorLayout.visibility = View.GONE
            }

            is PetDetailState.Success -> {
                progressBar.visibility = View.GONE
                content.visibility = View.VISIBLE
                errorLayout.visibility = View.GONE
                bindPost(state.post)
            }

            is PetDetailState.Error -> {
                progressBar.visibility = View.GONE
                content.visibility = View.GONE
                errorLayout.visibility = View.VISIBLE
                errorText.text = state.message
            }
        }
    }

    private fun bindPost(post: Post) {
        petName.text = post.petName
        breedAge.text = post.detailBreedAge()
        city.text = post.city.ifBlank { "Location not provided" }
        status.text = post.status.replaceFirstChar { it.uppercase() }
        fee.text = post.detailFee()
        description.text = post.description.ifBlank { "No description provided" }
        vaccinated.text = "Vaccinated: ${if (post.isVaccinated) "Yes" else "No"}"
        healthPassport.text =
            "Health passport: ${if (post.hasHealthPassport) "Yes" else "No"}"

        if (post.hasDetailImage()) {
            media.load(post.mediaUrl) {
                crossfade(true)
                placeholder(R.drawable.placeholder)
                error(R.drawable.placeholder)
            }
        } else {
            media.setImageResource(R.drawable.placeholder)
        }
    }
}
