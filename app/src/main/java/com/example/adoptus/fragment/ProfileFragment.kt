package com.example.adoptus.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.example.adoptus.R

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 1. Inflate layout utama fragment profil
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // 2. Set Nama Akun Tiruan (Frontend Mode)
        val tvTopAccountName = view.findViewById<TextView>(R.id.tvTopAccountName)
        tvTopAccountName.text = "sarah_mitchell"

        // 3. Setup RecyclerView Grid 3 Kolom untuk Galeri Hewan
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

        val dummyContents = listOf(
            DummyItem("Tips Mandi Cat", "Edukasi • 5m", R.drawable.placeholder, true),
            DummyItem("Nutrisi Anjing", "Tips • 12m", R.drawable.placeholder, true),
            DummyItem("Mengatasi Kutu", "Kesehatan", R.drawable.placeholder, true),
            DummyItem("Training Anak Anjing", "Edukasi", R.drawable.placeholder, true)
        )

        val adapter = ProfileGridAdapter(dummyPets)
        rvPetGrid.adapter = adapter

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Cek tab posisi ke berapa yang sedang diklik user
                when (tab?.position) {
                    0 -> {
                        // Jika klik tab "Pets" (Posisi 0), tukar datanya ke daftar hewan
                        adapter.updateList(dummyPets)
                    }
                    1 -> {
                        // Jika klik tab "Content" (Posisi 1), tukar datanya ke daftar video/infografis
                        adapter.updateList(dummyContents)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        return view
    }
}

data class DummyItem(val title: String, val subtitle: String, val imageRes: Int, val isVideo: Boolean = false)

class ProfileGridAdapter(private var list: List<DummyItem>) : RecyclerView.Adapter<ProfileGridAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val ivPhoto: android.widget.ImageView = v.findViewById(R.id.ivPetPhoto)
        val tvTitle: TextView = v.findViewById(R.id.tvPetName)
        val tvSubtitle: TextView = v.findViewById(R.id.tvPetBreed)
        val ivPlay: ImageView = v.findViewById(R.id.ivPlayIcon)
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

        if (item.isVideo) {
            holder.ivPlay.visibility = View.VISIBLE
        } else {
            holder.ivPlay.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateList(newList: List<DummyItem>) {
        this.list = newList
        notifyDataSetChanged()
    }
}
