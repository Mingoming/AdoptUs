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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class FeedAdapter(
    private val onDetailClick: (Post) -> Unit,
    private val onApplyClick: (Post) -> Unit,
    private val onOwnerClick: (Post) -> Unit,
    private val onLikeClick: (Post) -> Unit
) : ListAdapter<Post, FeedAdapter.FeedViewHolder>(DiffCallback) {

    private val activeViewHolders = mutableSetOf<FeedViewHolder>()
    private var lastPlayedPosition = RecyclerView.NO_POSITION

    fun pauseAllPlayers() {
        activeViewHolders.forEach { it.pausePlayer() }
    }

    fun playItemAtPosition(recyclerView: RecyclerView?, position: Int) {
        if (recyclerView == null || position == RecyclerView.NO_POSITION) return

        // Release ONLY the previously-played item (kalo masih di layout)
        if (lastPlayedPosition != RecyclerView.NO_POSITION && lastPlayedPosition != position) {
            for (i in 0 until recyclerView.childCount) {
                val child = recyclerView.getChildAt(i)
                if (recyclerView.getChildAdapterPosition(child) == lastPlayedPosition) {
                    (recyclerView.getChildViewHolder(child) as? FeedViewHolder)?.releasePlayer()
                    break
                }
            }
        }

        // Play snapped item from start, mute+pause the rest
        val postAtPosition = getItem(if (position < itemCount) position else 0)
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val childPos = recyclerView.getChildAdapterPosition(child)
            val holder = recyclerView.getChildViewHolder(child) as? FeedViewHolder ?: continue
            if (childPos == position) {
                val player = holder.exoPlayer
                if (player == null && postAtPosition.mediaType == "video") {
                    // Player direlease sebelumnya — recreate
                    holder.initPlayer(postAtPosition.mediaUrl)
                }
                holder.exoPlayer?.apply {
                    seekTo(0)
                    volume = 1f
                    playWhenReady = true
                    play()
                }
            } else {
                holder.exoPlayer?.apply {
                    volume = 0f
                    pause()
                }
            }
        }

        lastPlayedPosition = position
    }

    inner class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val media: ImageView = itemView.findViewById(R.id.ivMedia)
        private val videoIndicator: ImageView = itemView.findViewById(R.id.imgVideoIndicator)
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
        private val pvVideo: PlayerView = itemView.findViewById(R.id.pvVideo)
        @JvmField
        internal var exoPlayer: ExoPlayer? = null
        private var lastPost: Post? = null

        fun bind(post: Post) {
            petName.text = post.petName
            breedAge.text = "${post.breed} | ${post.ageDisplay}"
            city.text = post.city
            likesCount.text = post.likesCount.toString()
            status.text = post.status.replaceFirstChar { it.uppercase() }
            fee.text = if (post.isFree) "FREE" else "Rp ${post.adoptionFee}"

            // Bersihkan player lama
            exoPlayer?.release()
            exoPlayer = null
            pvVideo.player = null
            pvVideo.visibility = View.GONE
            media.visibility = View.VISIBLE

            if (post.mediaType == "video") {
                videoIndicator.visibility = View.GONE
                pvVideo.visibility = View.VISIBLE
                initPlayer(post.mediaUrl)
            } else {
                videoIndicator.visibility = View.GONE
                media.setOnClickListener(null)
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
            lastPost = post

            // Memuat info profil pembuat postingan: gunakan embedded owner info jika tersedia untuk mencegah N+1 query
            val hasOwnerInfo = post.ownerUsername.isNotBlank()
            if (hasOwnerInfo) {
                if (post.ownerPhotoUrl.isNotBlank()) {
                    ownerAvatar.load(post.ownerPhotoUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_profile_placeholder)
                        error(R.drawable.ic_profile_placeholder)
                    }
                } else {
                    ownerAvatar.setImageResource(R.drawable.ic_profile_placeholder)
                }

                whatsappButton.setOnClickListener {
                    val targetUrl = if (post.ownerWhatsapp.isNotBlank()) {
                        val cleanedNum = post.ownerWhatsapp.replace(Regex("[^0-9+]"), "")
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
                // Fallback untuk post lama (legacy posts)
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
            }

            // Tint heart icon based on like status
            if (post.isLikedByCurrentUser) {
                likeButton.setColorFilter(androidx.core.content.ContextCompat.getColor(itemView.context, R.color.primary_orange))
            } else {
                likeButton.clearColorFilter()
            }

            detailButton.setOnClickListener { onDetailClick(post) }
            applyButton.setOnClickListener { onApplyClick(post) }
            ownerAvatar.setOnClickListener { onOwnerClick(post) }
            likeButton.setOnClickListener { onLikeClick(post) }
        }

        fun playPlayer() {
            exoPlayer?.play()
        }

        fun pausePlayer() {
            exoPlayer?.pause()
        }

        fun initPlayer(mediaUrl: String) {
            exoPlayer?.release()
            val player = ExoPlayer.Builder(itemView.context).build().also {
                it.repeatMode = Player.REPEAT_MODE_ALL
                val mediaItem = MediaItem.fromUri(mediaUrl)
                it.setMediaItem(mediaItem)
                it.prepare()
                it.playWhenReady = false
                it.volume = 0f
            }
            exoPlayer = player
            pvVideo.player = player

            val togglePlay = View.OnClickListener {
                val p = exoPlayer ?: return@OnClickListener
                if (p.isPlaying) {
                    p.pause()
                    videoIndicator.visibility = View.VISIBLE
                } else {
                    p.play()
                    videoIndicator.visibility = View.GONE
                }
            }
            pvVideo.setOnClickListener(togglePlay)
            media.setOnClickListener(togglePlay)
        }

        fun releasePlayer() {
            exoPlayer?.release()
            exoPlayer = null
            pvVideo.player = null
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

    override fun onViewAttachedToWindow(holder: FeedViewHolder) {
        super.onViewAttachedToWindow(holder)
        activeViewHolders.add(holder)
    }

    override fun onViewRecycled(holder: FeedViewHolder) {
        super.onViewRecycled(holder)
        activeViewHolders.remove(holder)
        holder.releasePlayer()
    }

    override fun onViewDetachedFromWindow(holder: FeedViewHolder) {
        super.onViewDetachedFromWindow(holder)
        activeViewHolders.remove(holder)
        holder.pausePlayer()
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
