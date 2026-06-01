package com.example.adoptus.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adoptus.R

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val tvTopAccountName = view.findViewById<TextView>(R.id.tvTopAccountName)
        tvTopAccountName.text = "sarah_mitchell"

        // Tombol setting → navigasi ke SettingFragment
        val btnSetting = view.findViewById<ImageView>(R.id.btnSetting)
        btnSetting.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_setting)
        }

        val rvPetGrid = view.findViewById<RecyclerView>(R.id.rvPetGrid)
        rvPetGrid.layoutManager = GridLayoutManager(context, 3)

        val dummyPets = listOf(
            DummyItem("David", "Blasteran", R.drawable.placeholder),
            DummyItem("Abu Gosong", "KAMPUNG", R.drawable.placeholder),
            DummyItem("Teddy", "Poodle", R.drawable.placeholder),
            DummyItem("Luna", "Labrador", R.drawable.placeholder),
            DummyItem("Simba", "Shorthair", R.drawable.placeholder),
            DummyItem("Barnie", "Border Collie", R.drawable.placeholder),
            DummyItem("Amel", "Siamese", R.drawable.placeholder),
            DummyItem("Peanut", "Chihuahua", R.drawable.placeholder),
            DummyItem("David", "Blasteran", R.drawable.placeholder),
            DummyItem("Amel", "Siamese", R.drawable.placeholder),
            DummyItem("Peanut", "Chihuahua", R.drawable.placeholder),
            DummyItem("David", "Blasteran", R.drawable.placeholder)
        )

        val adapter = ProfileGridAdapter(dummyPets)
        rvPetGrid.adapter = adapter

        return view
    }
}

data class DummyItem(val title: String, val subtitle: String, val imageRes: Int)

class ProfileGridAdapter(private val list: List<DummyItem>) : RecyclerView.Adapter<ProfileGridAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val ivPhoto: android.widget.ImageView = v.findViewById(R.id.ivPetPhoto)
        val tvTitle: TextView = v.findViewById(R.id.tvPetName)
        val tvSubtitle: TextView = v.findViewById(R.id.tvPetBreed)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.profile_item_pet, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvTitle.text = item.title
        holder.tvSubtitle.text = item.subtitle
        holder.ivPhoto.setImageResource(item.imageRes)
    }

    override fun getItemCount(): Int = list.size
}