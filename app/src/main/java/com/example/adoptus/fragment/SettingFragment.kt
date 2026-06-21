package com.example.adoptus.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.adoptus.R
import com.example.adoptus.data.model.User
import com.example.adoptus.ui.auth.LoginActivity
import android.content.Intent
import android.widget.ImageView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.adoptus.data.repository.PostMediaRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingFragment : Fragment() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()
    private val mediaRepository = PostMediaRepository()
    private var selectedAvatarUri: Uri? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedAvatarUri = uri
            view?.findViewById<ImageView>(R.id.ivAvatar)?.load(uri) {
                crossfade(true)
                error(R.drawable.ic_profile_placeholder)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivAvatar          = view.findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.ivAvatar)
        val btnBack           = view.findViewById<ImageView>(R.id.btnBack)
        val btnSave           = view.findViewById<TextView>(R.id.btnSave)
        val btnLogout         = view.findViewById<TextView>(R.id.btnLogout)
        val btnChangePhoto    = view.findViewById<TextView>(R.id.btnChangePhoto)
        val loadingOverlay    = view.findViewById<FrameLayout>(R.id.loadingOverlay)

        val etFullName        = view.findViewById<TextInputEditText>(R.id.etFullName)
        val etUsername        = view.findViewById<TextInputEditText>(R.id.etUsername)
        val etBio             = view.findViewById<TextInputEditText>(R.id.etBio)
        val etCity            = view.findViewById<TextInputEditText>(R.id.etCity)
        val etWhatsapp        = view.findViewById<TextInputEditText>(R.id.etWhatsapp)
        val etNewPassword     = view.findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirmPassword = view.findViewById<TextInputEditText>(R.id.etConfirmPassword)

        val tilUsername       = view.findViewById<TextInputLayout>(R.id.tilUsername)
        val tilNewPassword    = view.findViewById<TextInputLayout>(R.id.tilNewPassword)
        val tilConfirmPassword= view.findViewById<TextInputLayout>(R.id.tilConfirmPassword)

        // Load data user dari Firestore
        loadUserData(ivAvatar, etFullName, etUsername, etBio, etCity, etWhatsapp)

        btnBack.setOnClickListener { findNavController().navigateUp() }

        btnChangePhoto.setOnClickListener {
            pickMedia.launch("image/*")
        }

        // Simpan perubahan profil
        btnSave.setOnClickListener {
            val fullName  = etFullName.text.toString().trim()
            val username  = etUsername.text.toString()
            val bio       = etBio.text.toString().trim()
            val city      = etCity.text.toString().trim()
            val whatsapp  = etWhatsapp.text.toString().trim()
            val newPass   = etNewPassword.text.toString()
            val confirmPass = etConfirmPassword.text.toString()

            // Validasi username
            tilUsername.error = null
            if (username.isEmpty()) {
                tilUsername.error = "Username is required"
                return@setOnClickListener
            }
            if (!User.isValidUsername(username)) {
                tilUsername.error = "Username must be 3-30 characters without whitespace"
                return@setOnClickListener
            }
            if (fullName.isEmpty()) {
                Toast.makeText(context, "Full name is required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (fullName.length > 80) {
                Toast.makeText(context, "Full name is too long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (bio.length > 300 || city.length > 80 || whatsapp.length > 30) {
                Toast.makeText(context, "Profile field is too long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validasi password kalau diisi
            tilNewPassword.error = null
            tilConfirmPassword.error = null
            if (newPass.isNotEmpty()) {
                if (newPass.length < 6) {
                    tilNewPassword.error = "Password must be at least 6 characters"
                    return@setOnClickListener
                }
                if (newPass != confirmPass) {
                    tilConfirmPassword.error = "Passwords do not match"
                    return@setOnClickListener
                }
            }

            loadingOverlay.visibility = View.VISIBLE
            btnSave.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val uid = auth.currentUser?.uid
                        ?: throw Exception("Not logged in")

                    var uploadedPhotoUrl: String? = null
                    selectedAvatarUri?.let { avatarUri ->
                        val result = mediaRepository.uploadMedia(
                            contentResolver = requireContext().contentResolver,
                            mediaUri = avatarUri,
                            uid = uid
                        ).getOrThrow()
                        uploadedPhotoUrl = result.publicUrl
                    }

                    // Update profil di Firestore
                    val updates = mutableMapOf<String, Any>(
                        "fullName" to fullName,
                        "username" to username,
                        "bio"      to bio,
                        "city"     to city,
                        "whatsapp" to whatsapp
                    )

                    if (uploadedPhotoUrl != null) {
                        updates["photoUrl"] = uploadedPhotoUrl!!
                    }

                    db.collection("users").document(uid)
                        .update(updates)
                        .await()

                    // Update password kalau diisi
                    if (newPass.isNotEmpty()) {
                        auth.currentUser?.updatePassword(newPass)?.await()
                    }

                    loadingOverlay.visibility = View.GONE
                    btnSave.isEnabled = true
                    Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()

                } catch (e: Exception) {
                    loadingOverlay.visibility = View.GONE
                    btnSave.isEnabled = true

                    // Firebase requires re-auth untuk update password
                    val msg = when {
                        e.message?.contains("REQUIRES_RECENT_LOGIN", ignoreCase = true) == true ->
                            "Please log out and log in again before changing password."
                        else -> "Failed to save: ${e.message}"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Logout
        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }

    private fun loadUserData(
        ivAvatar: com.google.android.material.imageview.ShapeableImageView,
        etFullName: TextInputEditText,
        etUsername: TextInputEditText,
        etBio: TextInputEditText,
        etCity: TextInputEditText,
        etWhatsapp: TextInputEditText
    ) {
        val uid = auth.currentUser?.uid ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val doc = db.collection("users").document(uid).get().await()
                val user = User.fromMap(doc.id, doc.data.orEmpty())
                etFullName.setText(user.fullName)
                etUsername.setText(user.username)
                etBio.setText(user.bio)
                etCity.setText(user.city)
                etWhatsapp.setText(user.whatsapp)
                if (user.photoUrl.isNotEmpty()) {
                    ivAvatar.load(user.photoUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_profile_placeholder)
                        error(R.drawable.ic_profile_placeholder)
                    }
                } else {
                    ivAvatar.setImageResource(R.drawable.ic_profile_placeholder)
                }
            } catch (e: Exception) {
                // Kalau gagal load, biarkan field kosong
            }
        }
    }
}
