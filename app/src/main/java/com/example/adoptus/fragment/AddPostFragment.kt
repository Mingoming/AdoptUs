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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.adoptus.MainActivity
import com.example.adoptus.R
import com.example.adoptus.data.model.Post
import com.example.adoptus.data.repository.PostImageRepository
import com.example.adoptus.data.repository.PostRepository
import com.example.adoptus.data.repository.UploadedPostImage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AddPostFragment : Fragment() {

    private val auth = FirebaseAuth.getInstance()
    private val postRepository = PostRepository()
    private val imageRepository = PostImageRepository()

    private var selectedImageUri: Uri? = null
    private var isUploading = false

    private lateinit var imagePreview: ImageView
    private lateinit var mediaPrompt: View
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var uploadButton: TextView
    private lateinit var selectMediaButton: View

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
        if (uri == null) {
            imagePreview.visibility = View.GONE
            mediaPrompt.visibility = View.VISIBLE
        } else {
            mediaPrompt.visibility = View.GONE
            imagePreview.visibility = View.VISIBLE
            imagePreview.load(uri) {
                crossfade(true)
                error(R.drawable.placeholder)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_add_post, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)
        uploadButton = view.findViewById(R.id.btnUpload)
        selectMediaButton = view.findViewById(R.id.btnSelectMedia)
        imagePreview = view.findViewById(R.id.imgSelectedPhoto)
        mediaPrompt = view.findViewById(R.id.mediaPrompt)
        loadingOverlay = view.findViewById(R.id.addPostLoadingOverlay)

        val etPetName = view.findViewById<EditText>(R.id.etPetName)
        val actvPetType = view.findViewById<AutoCompleteTextView>(R.id.actvPetType)
        val etPetBreed = view.findViewById<EditText>(R.id.etPetBreed)
        val etPetAge = view.findViewById<EditText>(R.id.etPetAge)
        val actvAgeUnit = view.findViewById<AutoCompleteTextView>(R.id.actvAgeUnit)
        val etPetDescription = view.findViewById<EditText>(R.id.etPetDescription)
        val etAdoptionFee = view.findViewById<EditText>(R.id.etAdoptionFee)
        val cbVaccinated = view.findViewById<CheckBox>(R.id.cbVaccinated)
        val cbHealthPassport = view.findViewById<CheckBox>(R.id.cbHealthPassport)

        val animalTypes = arrayOf(
            "Kucing",
            "Anjing",
            "Burung",
            "Kelinci",
            "Hamster",
            "Lainnya"
        )
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
            if (!isUploading) pickImage.launch("image/*")
        }

        uploadButton.setOnClickListener {
            if (isUploading) return@setOnClickListener

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
                    return@setOnClickListener
                }

                type.isEmpty() -> {
                    actvPetType.error = "Required"
                    actvPetType.requestFocus()
                    actvPetType.showDropDown()
                    return@setOnClickListener
                }

                breed.isEmpty() -> {
                    etPetBreed.error = "Required"
                    etPetBreed.requestFocus()
                    return@setOnClickListener
                }

                ageText.isEmpty() -> {
                    etPetAge.error = "Required"
                    etPetAge.requestFocus()
                    return@setOnClickListener
                }
            }

            val post = Post(
                petName = name,
                petType = type,
                breed = breed,
                age = ageText.toIntOrNull() ?: 0,
                ageUnit = ageUnit.ifEmpty { "Months" },
                city = "Indonesia",
                description = description,
                mediaUrl = "",
                mediaType = "image",
                isVaccinated = cbVaccinated.isChecked,
                hasHealthPassport = cbHealthPassport.isChecked,
                adoptionFee = feeText.toIntOrNull() ?: 0,
                status = "available",
                likesCount = 0,
                createdAt = Timestamp.now()
            )

            createPost(post)
        }
    }

    private fun createPost(post: Post) {
        setUploading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            var uploadedImage: UploadedPostImage? = null

            try {
                val uid = auth.currentUser?.uid
                    ?: throw IllegalStateException("Not logged in")

                uploadedImage = selectedImageUri?.let { imageUri ->
                    imageRepository.uploadImage(
                        contentResolver = requireContext().contentResolver,
                        imageUri = imageUri,
                        uid = uid
                    ).getOrThrow()
                }

                val postWithMedia = post.copy(
                    mediaUrl = uploadedImage?.publicUrl.orEmpty(),
                    mediaType = "image"
                )

                postRepository.createPost(postWithMedia).getOrElse { postError ->
                    uploadedImage?.let { uploaded ->
                        imageRepository.deleteImage(uploaded.path)
                            .exceptionOrNull()
                            ?.let(postError::addSuppressed)
                    }
                    throw postError
                }

                Toast.makeText(context, "Post uploaded!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } catch (error: Exception) {
                setUploading(false)
                Toast.makeText(
                    context,
                    "Upload failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setUploading(uploading: Boolean) {
        isUploading = uploading
        loadingOverlay.visibility = if (uploading) View.VISIBLE else View.GONE
        uploadButton.text = if (uploading) "Uploading..." else "Upload"
        uploadButton.isEnabled = !uploading
        selectMediaButton.isEnabled = !uploading
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
