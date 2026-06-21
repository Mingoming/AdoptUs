package com.example.adoptus.ui.feed

import android.content.Intent
import android.net.Uri
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

class FeedAdapter(
    private val onDetailClick: (Post) -> Unit,
    private val onApplyClick: (Post) -> Unit
) : ListAdapter<Post, FeedAdapter.FeedViewHolder>(DiffCallback) {

    inner class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val media: ImageView = itemView.findViewById(R.id.ivMedia)
        private val videoIndicator: ImageView =
            itemView.findViewById(R.id.imgVideoIndicator)
        private val petName: TextView = itemView.findViewById(R.id.tvPetName)
        private val breedAge: TextView = itemView.findViewById(R.id.tvBreedAge)
        private val city: TextView = itemView.findViewById(R.id.tvCity)
        private val status: TextView = itemView.findViewById(R.id.tvStatus)
        private val fee: TextView = itemView.findViewById(R.id.tvFee)
        private val likesCount: TextView = itemView.findViewById(R.id.tvLikesCount)
        private val ownerAvatar: ImageView = itemView.findViewById(R.id.ivOwnerAvatar)
        private val applyButton: TextView = itemView.findViewById(R.id.btnApply)
        private val whatsappButton: TextView = itemView.findViewById(R.id.btnWhatsapp)
        private val likeButton: ImageView = itemView.findViewById(R.id.btnLike)
        private val detailButton: ImageView = itemView.findViewById(R.id.btnDetail)

        fun bind(post: Post) {
            petName.text = post.petName
            breedAge.text = "${post.breed} | ${post.ageDisplay}"
            city.text = post.city
            likesCount.text = post.likesCount.toString()
            status.text = post.status.replaceFirstChar { it.uppercase() }
            fee.text = if (post.isFree) "FREE" else "Rp ${post.adoptionFee}"

            if (post.mediaType == "video") {
                media.setImageResource(R.drawable.placeholder)
                videoIndicator.visibility = View.VISIBLE
            } else {
                videoIndicator.visibility = View.GONE
                if (post.mediaUrl.isNotBlank()) {
                    media.load(post.mediaUrl) {
                        crossfade(true)
                        placeholder(R.drawable.placeholder)
                        error(R.drawable.placeholder)
                    }
                } else {
                    media.setImageResource(R.drawable.placeholder)
                }
            }

            // Memuat info profil pembuat postingan secara dinamis dari Firestore
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("users").document(post.userId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val whatsappNum = doc.getString("whatsapp")?.trim().orEmpty()
                        val photoUrl = doc.getString("photoUrl")?.trim().orEmpty()

                        if (photoUrl.isNotBlank()) {
                            ownerAvatar.load(photoUrl) {
                                crossfade(true)
                                placeholder(R.drawable.ic_profile_placeholder)
                                error(R.drawable.ic_profile_placeholder)
                            }
                        } else {
                            ownerAvatar.setImageResource(R.drawable.ic_profile_placeholder)
                        }

                        whatsappButton.setOnClickListener {
                            val targetUrl = if (whatsappNum.isNotBlank()) {
                                val cleanedNum = whatsappNum.replace(Regex("[^0-9+]"), "")
                                val formattedNum = when {
                                    cleanedNum.startsWith("0") -> "62" + cleanedNum.substring(1)
                                    cleanedNum.startsWith("+") -> cleanedNum.substring(1)
                                    else -> cleanedNum
                                }
                                "https://wa.me/$formattedNum?text=Halo, saya tertarik mengadopsi ${post.petName}"
                            } else {
                                "https://wa.me/?text=Halo, saya tertarik mengadopsi ${post.petName}"
                            }
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                            itemView.context.startActivity(intent)
                        }
                    } else {
                        ownerAvatar.setImageResource(R.drawable.ic_profile_placeholder)
                    }
                }
                .addOnFailureListener {
                    ownerAvatar.setImageResource(R.drawable.ic_profile_placeholder)
                }

            detailButton.setOnClickListener { onDetailClick(post) }
            applyButton.setOnClickListener { onApplyClick(post) }
            likeButton.setOnClickListener { }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feed_post, parent, false)
        view.layoutParams.height = parent.height
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(old: Post, new: Post): Boolean =
                old.postId == new.postId

            override fun areContentsTheSame(old: Post, new: Post): Boolean =
                old == new
        }
    }
}
