package com.example.adoptus.ui.search

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

class SearchPostAdapter(
    private val onPostClick: (Post) -> Unit
) : ListAdapter<Post, SearchPostAdapter.PostViewHolder>(DiffCallback) {

    class PostViewHolder(
        itemView: View,
        private val onPostClick: (Post) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivPetPhoto)
        private val imgVideoIndicator: ImageView = itemView.findViewById(R.id.imgVideoIndicator)
        private val tvPetName: TextView = itemView.findViewById(R.id.tvPetName)
        private val tvPetBreed: TextView = itemView.findViewById(R.id.tvPetBreed)

        fun bind(post: Post) {
            tvPetName.text = post.petName
            tvPetBreed.text = post.breed.ifBlank { post.petType }

            if (post.mediaType == "video") {
                ivPhoto.setImageResource(R.drawable.placeholder)
                imgVideoIndicator.visibility = View.VISIBLE
            } else {
                imgVideoIndicator.visibility = View.GONE
                if (post.mediaUrl.isNotBlank()) {
                    ivPhoto.load(post.mediaUrl) {
                        crossfade(true)
                        placeholder(R.drawable.placeholder)
                        error(R.drawable.placeholder)
                    }
                } else {
                    ivPhoto.setImageResource(R.drawable.placeholder)
                }
            }

            itemView.setOnClickListener { onPostClick(post) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.profile_item_pet, parent, false)
        return PostViewHolder(view, onPostClick)
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
