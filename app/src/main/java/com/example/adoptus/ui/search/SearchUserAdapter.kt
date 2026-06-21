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
import com.example.adoptus.data.model.User

class SearchUserAdapter(
    private val onUserClick: (User) -> Unit
) : ListAdapter<User, SearchUserAdapter.UserViewHolder>(DiffCallback) {

    class UserViewHolder(
        itemView: View,
        private val onUserClick: (User) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)

        fun bind(user: User) {
            tvUsername.text = user.username
            if (user.photoUrl.isNotBlank()) {
                ivAvatar.load(user.photoUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_profile_placeholder)
                    error(R.drawable.ic_profile_placeholder)
                }
            } else {
                ivAvatar.setImageResource(R.drawable.ic_profile_placeholder)
            }
            itemView.setOnClickListener { onUserClick(user) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_result, parent, false)
        return UserViewHolder(view, onUserClick)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(oldItem: User, newItem: User): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: User, newItem: User): Boolean =
                oldItem == newItem
        }
    }
}
