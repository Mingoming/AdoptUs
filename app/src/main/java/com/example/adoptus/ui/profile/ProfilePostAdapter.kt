package com.example.adoptus.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.adoptus.R
import com.example.adoptus.data.model.Post

class ProfilePostAdapter :
    ListAdapter<Post, ProfilePostAdapter.PostViewHolder>(DiffCallback) {

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val photo: ImageView = itemView.findViewById(R.id.ivPetPhoto)
        private val name: TextView = itemView.findViewById(R.id.tvPetName)
        private val breed: TextView = itemView.findViewById(R.id.tvPetBreed)

        fun bind(post: Post) {
            name.text = post.petName
            breed.text = post.breed.ifBlank { post.petType }

            if (post.mediaType == "image" && post.mediaUrl.isNotBlank()) {
                photo.load(post.mediaUrl) {
                    crossfade(true)
                    placeholder(R.drawable.placeholder)
                    error(R.drawable.placeholder)
                }
            } else {
                photo.setImageResource(R.drawable.placeholder)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.profile_item_pet, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean =
                oldItem.postId == newItem.postId

            override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean =
                oldItem == newItem
        }
    }
}
