package com.example.adoptus.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.adoptus.R
import com.example.adoptus.data.model.Post
import com.example.adoptus.ui.feed.FeedViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AddPostFragment : Fragment() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // State upload
    private var isUploading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack        = view.findViewById<ImageView>(R.id.btnBack) // TODO: ganti ke ImageButton
        val btnUpload      = view.findViewById<TextView>(R.id.btnUpload)
        val btnSelectMedia = view.findViewById<View>(R.id.btnSelectMedia)

        val etPetName        = view.findViewById<EditText>(R.id.etPetName)
        val actvPetType      = view.findViewById<AutoCompleteTextView>(R.id.actvPetType)
        val etPetBreed       = view.findViewById<EditText>(R.id.etPetBreed)
        val etPetAge         = view.findViewById<EditText>(R.id.etPetAge)
        val actvAgeUnit      = view.findViewById<AutoCompleteTextView>(R.id.actvAgeUnit)
        val etPetDescription = view.findViewById<EditText>(R.id.etPetDescription)
        val etAdoptionFee    = view.findViewById<EditText>(R.id.etAdoptionFee)
        val cbVaccinated     = view.findViewById<CheckBox>(R.id.cbVaccinated)
        val cbHealthPassport = view.findViewById<CheckBox>(R.id.cbHealthPassport)

        // Setup dropdown Animal type
        val animalTypes = arrayOf("Kucing", "Anjing", "Burung", "Kelinci", "Hamster", "Lainnya")
        actvPetType.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, animalTypes)
        )
        val ageUnits = arrayOf("Months", "Years")
        actvAgeUnit.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, ageUnits)
        )
        actvPetType.setOnClickListener { actvPetType.showDropDown() }
        actvAgeUnit.setOnClickListener { actvAgeUnit.showDropDown() }

        // Tombol back
        btnBack.setOnClickListener { findNavController().navigateUp() }

        // Media — placeholder dulu sampai Storage aktif
        btnSelectMedia.setOnClickListener {
            Toast.makeText(context, "Photo upload coming soon (Storage pending)", Toast.LENGTH_SHORT).show()
        }

        // Upload ke Firestore
        btnUpload.setOnClickListener {
            if (isUploading) return@setOnClickListener

            val name     = etPetName.text.toString().trim()
            val type     = actvPetType.text.toString().trim()
            val breed    = etPetBreed.text.toString().trim()
            val ageStr   = etPetAge.text.toString().trim()
            val ageUnit  = actvAgeUnit.text.toString().trim()
            val desc     = etPetDescription.text.toString().trim()
            val feeStr   = etAdoptionFee.text.toString().trim()

            // Validasi
            etPetName.error   = null
            actvPetType.error = null
            etPetBreed.error  = null
            etPetAge.error    = null

            when {
                name.isEmpty()   -> { etPetName.error = "Required"; etPetName.requestFocus(); return@setOnClickListener }
                type.isEmpty()   -> { actvPetType.error = "Required"; actvPetType.requestFocus(); actvPetType.showDropDown(); return@setOnClickListener }
                breed.isEmpty()  -> { etPetBreed.error = "Required"; etPetBreed.requestFocus(); return@setOnClickListener }
                ageStr.isEmpty() -> { etPetAge.error = "Required"; etPetAge.requestFocus(); return@setOnClickListener }
            }

            val age = ageStr.toIntOrNull() ?: 0
            val fee = feeStr.toIntOrNull() ?: 0
            val unit = if (ageUnit.isEmpty()) "Months" else ageUnit

            // Ambil kota dari profil user — untuk sekarang pakai placeholder
            val city = "Indonesia"

            val post = Post(
                petName           = name,
                petType           = type,
                breed             = breed,
                age               = age,
                ageUnit           = unit,
                city              = city,
                description       = desc,
                mediaUrl          = "",        // kosong dulu sampai Storage aktif
                mediaType         = "image",
                isVaccinated      = cbVaccinated.isChecked,
                hasHealthPassport = cbHealthPassport.isChecked,
                adoptionFee       = fee,
                status            = "available",
                likesCount        = 0,
                createdAt         = Timestamp.now()
            )

            uploadPost(post, btnUpload)
        }
    }

    private fun uploadPost(post: Post, btnUpload: TextView) {
        isUploading = true
        btnUpload.text = "Uploading..."
        btnUpload.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val uid = auth.currentUser?.uid
                    ?: throw Exception("Not logged in")

                val docRef = db.collection("posts").document()
                val postWithId = post.copy(
                    postId = docRef.id,
                    userId = uid
                )
                docRef.set(postWithId.toMap()).await()

                Toast.makeText(context, "Post uploaded! 🐾", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()

            } catch (e: Exception) {
                isUploading = false
                btnUpload.text = "Upload"
                btnUpload.isEnabled = true
                Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as? com.example.adoptus.MainActivity)?.hideBottomNav()
    }

    override fun onPause() {
        super.onPause()
        (activity as? com.example.adoptus.MainActivity)?.showBottomNav()
    }
}