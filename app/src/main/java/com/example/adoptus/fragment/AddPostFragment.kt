package com.example.adoptus.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.adoptus.MainActivity
import com.example.adoptus.R
import com.google.android.material.textfield.TextInputEditText

class AddPostFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_post, container, false)

        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val btnUpload = view.findViewById<TextView>(R.id.btnUpload)
        val btnSelectMedia = view.findViewById<View>(R.id.btnSelectMedia)

        val etPetName = view.findViewById<EditText>(R.id.etPetName)
        val actvPetType = view.findViewById<AutoCompleteTextView>(R.id.actvPetType)
        val etPetBreed = view.findViewById<EditText>(R.id.etPetBreed)
        val etPetAge = view.findViewById<EditText>(R.id.etPetAge)
        val actvAgeUnit = view.findViewById<AutoCompleteTextView>(R.id.actvAgeUnit)

        // Komponen Opsional & Checkbox
        val etPetDescription = view.findViewById<EditText>(R.id.etPetDescription)
        val etAdoptionFee = view.findViewById<EditText>(R.id.etAdoptionFee)
        val cbVaccinated = view.findViewById<CheckBox>(R.id.cbVaccinated)
        val cbHealthPassport = view.findViewById<CheckBox>(R.id.cbHealthPassport)

        // Setup Dropdown Pilihan (Mock Data)
        val animalTypes = arrayOf("Kucing", "Anjing", "Burung", "Kelinci", "Hamster", "Lainnya")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, animalTypes)
        actvPetType.setAdapter(typeAdapter)

        val ageUnits = arrayOf("Months", "Years")
        val unitAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, ageUnits)
        actvAgeUnit.setAdapter(unitAdapter)

        actvPetType.setOnClickListener {
            actvPetType.showDropDown()
        }

        actvAgeUnit.setOnClickListener {
            actvAgeUnit.showDropDown()
        }

        // Logika Tombol Kembali (Balik ke Feed)
        btnBack.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FeedFragment())
                .commit()
        }

        // Simulasi Klik Kotak Upload Foto/Video
        btnSelectMedia.setOnClickListener {
            Toast.makeText(context, "Membuka Galeri (Frontend Mode)", Toast.LENGTH_SHORT).show()
        }

        // LOGIKA VALIDASI UTAMA SAAT KLIK UPLOAD
        btnUpload.setOnClickListener {
            // Ambil semua data teks inputan
            val name = etPetName.text.toString().trim()
            val type = actvPetType.text.toString().trim()
            val breed = etPetBreed.text.toString().trim()
            val age = etPetAge.text.toString().trim()
            val ageUnit = actvAgeUnit.text.toString().trim()

            // Reset status error sebelum dicek ulang
            etPetName.error = null
            actvPetType.error = null
            etPetBreed.error = null
            etPetAge.error = null
            actvAgeUnit.error = null

            // Rangkaian Pengecekan Validasi Wajib Isi
            if (name.isEmpty()) {
                etPetName.error = "Please fill this field"
                etPetName.requestFocus()
            }
            else if (type.isEmpty()) {
                actvPetType.error = "Please fill this field"
                actvPetType.requestFocus()
                actvPetType.showDropDown()
            }
            else if (breed.isEmpty()) {
                etPetBreed.error = "Please fill this field"
                etPetBreed.requestFocus()
            }
            else if (age.isEmpty()) {
                etPetAge.error = "Please fill this field"
                etPetAge.requestFocus()
            }
            else if (ageUnit.isEmpty()) {
                actvAgeUnit.error = "Please fill this field"
                actvAgeUnit.requestFocus()
                actvAgeUnit.showDropDown()
            }
            else {
                // JIKA SEMUA FORM WAJIB SUDAH LOLOS SELEKSI
                Toast.makeText(context, "Post '$name' successfully uploaded!", Toast.LENGTH_LONG).show()

                // Tendang balik user ke halaman utama (Feed)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, FeedFragment())
                    .commit()
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.setBottomNavVisibility(false)
    }

    override fun onPause() {
        super.onPause()
        (activity as? MainActivity)?.setBottomNavVisibility(true)
    }
}