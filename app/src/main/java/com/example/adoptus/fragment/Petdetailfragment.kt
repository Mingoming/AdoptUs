package com.example.adoptus.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs

class PetDetailFragment : Fragment() {

    // Argumen postId dikirim dari Feed/Search/Profile
    private val args: PetDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Placeholder — akan diimplementasi saat fase detail hewan
        val tv = TextView(requireContext()).apply {
            text = "Pet Detail — postId: ${args.postId}\n(Coming soon)"
            textSize = 16f
            setPadding(48, 48, 48, 48)
        }

        // Tombol back HP sudah otomatis ditangani NavController
        return tv
    }
}