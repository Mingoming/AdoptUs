package com.example.adoptus.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.example.adoptus.MainActivity
import com.example.adoptus.R
import com.example.adoptus.data.model.Post
import com.example.adoptus.ui.editpost.EditPostState
import com.example.adoptus.ui.editpost.EditPostViewModel
import kotlinx.coroutines.launch

class EditPostFragment : Fragment() {

    private val args: EditPostFragmentArgs by navArgs()
    private val viewModel: EditPostViewModel by viewModels()

    private var selectedMediaUri: Uri? = null
    private var selectedMediaType: String? = null
    private var isSaving = false
    private var loadedPost: Post? = null

    private lateinit var imagePreview: ImageView
    private lateinit var videoIndicator: ImageView
    private lateinit var mediaPrompt: View
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var saveButton: TextView
    private lateinit var selectMediaButton: View

    private lateinit var etPetName: EditText
    private lateinit var actvPetType: AutoCompleteTextView
    private lateinit var etPetBreed: EditText
    private lateinit var etPetAge: EditText
    private lateinit var actvAgeUnit: AutoCompleteTextView
    private lateinit var etPetDescription: EditText
    private lateinit var etAdoptionFee: EditText
    private lateinit var cbVaccinated: CheckBox
    private lateinit var cbHealthPassport: CheckBox

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedMediaUri = uri
        if (uri == null) {
            selectedMediaType = null
            // Kembalikan ke foto/video loadedPost semula jika ada
            val post = loadedPost
            if (post != null && post.mediaUrl.isNotBlank()) {
                mediaPrompt.visibility = View.GONE
                imagePreview.visibility = View.VISIBLE
                if (post.mediaType == "video") {
                    imagePreview.setImageResource(R.drawable.placeholder)
                    videoIndicator.visibility = View.VISIBLE
                } else {
                    videoIndicator.visibility = View.GONE
                    imagePreview.load(post.mediaUrl) {
                        crossfade(true)
                        error(R.drawable.placeholder)
                    }
                }
            } else {
                imagePreview.visibility = View.GONE
                videoIndicator.visibility = View.GONE
                mediaPrompt.visibility = View.VISIBLE
            }
        } else {
            val mimeType = requireContext().contentResolver.getType(uri)
            selectedMediaType = when (mimeType) {
                "video/mp4" -> "video"
                "image/jpeg", "image/png", "image/webp" -> "image"
                else -> null
            }

            if (selectedMediaType == null) {
                selectedMediaUri = null
                Toast.makeText(
                    requireContext(),
                    "Only JPEG, PNG, WebP, and MP4 files are supported",
                    Toast.LENGTH_LONG
                ).show()
                return@registerForActivityResult
            }

            mediaPrompt.visibility = View.GONE
            imagePreview.visibility = View.VISIBLE
            if (selectedMediaType == "video") {
                imagePreview.setImageResource(R.drawable.placeholder)
                videoIndicator.visibility = View.VISIBLE
            } else {
                videoIndicator.visibility = View.GONE
                imagePreview.load(uri) {
                    crossfade(true)
                    error(R.drawable.placeholder)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_edit_post, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        saveButton = view.findViewById(R.id.btnSave)
        selectMediaButton = view.findViewById(R.id.btnSelectMedia)
        imagePreview = view.findViewById(R.id.imgSelectedPhoto)
        videoIndicator = view.findViewById(R.id.imgVideoIndicator)
        mediaPrompt = view.findViewById(R.id.mediaPrompt)
        loadingOverlay = view.findViewById(R.id.editPostLoadingOverlay)

        etPetName = view.findViewById(R.id.etPetName)
        actvPetType = view.findViewById(R.id.actvPetType)
        etPetBreed = view.findViewById(R.id.etPetBreed)
        etPetAge = view.findViewById(R.id.etPetAge)
        actvAgeUnit = view.findViewById(R.id.actvAgeUnit)
        etPetDescription = view.findViewById(R.id.etPetDescription)
        etAdoptionFee = view.findViewById(R.id.etAdoptionFee)
        cbVaccinated = view.findViewById(R.id.cbVaccinated)
        cbHealthPassport = view.findViewById(R.id.cbHealthPassport)

        val animalTypes = resources.getStringArray(R.array.animal_types)
        actvPetType.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                animalTypes
            )
        )
        val ageUnits = arrayOf("Months", "Years")
        actvAgeUnit.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                ageUnits
            )
        )
        actvPetType.setOnClickListener { actvPetType.showDropDown() }
        actvAgeUnit.setOnClickListener { actvAgeUnit.showDropDown() }

        btnBack.setOnClickListener { findNavController().navigateUp() }
        selectMediaButton.setOnClickListener {
            if (!isSaving) pickMedia.launch("*/*")
        }

        saveButton.setOnClickListener {
            if (isSaving) return@setOnClickListener
            savePostChanges()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is EditPostState.Idle -> {
                        setSaving(false)
                    }
                    is EditPostState.Loading -> {
                        setSaving(true)
                    }
                    is EditPostState.PostLoaded -> {
                        setSaving(false)
                        populateFields(state.post)
                    }
                    is EditPostState.SaveSuccess -> {
                        setSaving(false)
                        Toast.makeText(requireContext(), "Post updated successfully!", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                    is EditPostState.Error -> {
                        setSaving(false)
                        Toast.makeText(
                            requireContext(),
                            "Error: ${state.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        viewModel.loadPost(args.postId)
    }

    private fun populateFields(post: Post) {
        loadedPost = post
        etPetName.setText(post.petName)
        actvPetType.setText(post.petType, false)
        etPetBreed.setText(post.breed)
        etPetAge.setText(post.age.toString())
        actvAgeUnit.setText(post.ageUnit, false)
        etPetDescription.setText(post.description)
        etAdoptionFee.setText(post.adoptionFee.toString())
        cbVaccinated.isChecked = post.isVaccinated
        cbHealthPassport.isChecked = post.hasHealthPassport

        if (post.mediaUrl.isNotBlank()) {
            mediaPrompt.visibility = View.GONE
            imagePreview.visibility = View.VISIBLE
            if (post.mediaType == "video") {
                imagePreview.setImageResource(R.drawable.placeholder)
                videoIndicator.visibility = View.VISIBLE
            } else {
                videoIndicator.visibility = View.GONE
                imagePreview.load(post.mediaUrl) {
                    crossfade(true)
                    placeholder(R.drawable.placeholder)
                    error(R.drawable.placeholder)
                }
            }
        } else {
            mediaPrompt.visibility = View.VISIBLE
            imagePreview.visibility = View.GONE
            videoIndicator.visibility = View.GONE
        }
    }

    private fun savePostChanges() {
        val post = loadedPost ?: return

        val name = etPetName.text.toString().trim()
        val type = actvPetType.text.toString().trim()
        val breed = etPetBreed.text.toString().trim()
        val ageText = etPetAge.text.toString().trim()
        val ageUnit = actvAgeUnit.text.toString().trim()
        val description = etPetDescription.text.toString().trim()
        val feeText = etAdoptionFee.text.toString().trim()

        etPetName.error = null
        actvPetType.error = null
        etPetBreed.error = null
        etPetAge.error = null

        when {
            name.isEmpty() -> {
                etPetName.error = "Required"
                etPetName.requestFocus()
                return
            }

            type.isEmpty() -> {
                actvPetType.error = "Required"
                actvPetType.requestFocus()
                actvPetType.showDropDown()
                return
            }

            breed.isEmpty() -> {
                etPetBreed.error = "Required"
                etPetBreed.requestFocus()
                return
            }

            ageText.isEmpty() -> {
                etPetAge.error = "Required"
                etPetAge.requestFocus()
                return
            }
        }

        val age = ageText.toIntOrNull()
        if (age == null || age < 0) {
            etPetAge.error = "Please enter a valid age (whole number)"
            etPetAge.requestFocus()
            return
        }

        val updatedPost = post.copy(
            petName = name,
            petType = type,
            breed = breed,
            age = age,
            ageUnit = ageUnit.ifEmpty { "Months" },
            description = description,
            isVaccinated = cbVaccinated.isChecked,
            hasHealthPassport = cbHealthPassport.isChecked,
            adoptionFee = feeText.toIntOrNull() ?: 0
        )

        viewModel.updatePost(
            context = requireContext(),
            post = updatedPost,
            newMediaUri = selectedMediaUri,
            oldMediaUrl = post.mediaUrl
        )
    }

    private fun setSaving(saving: Boolean) {
        isSaving = saving
        loadingOverlay.visibility = if (saving) View.VISIBLE else View.GONE
        saveButton.text = if (saving) "Saving..." else "Save"
        saveButton.isEnabled = !saving
        selectMediaButton.isEnabled = !saving
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.hideBottomNav()
    }

    override fun onPause() {
        super.onPause()
        (activity as? MainActivity)?.showBottomNav()
    }
}
