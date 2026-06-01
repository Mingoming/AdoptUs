package com.example.adoptus.ui.feed

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.adoptus.R
import com.example.adoptus.data.model.Post

class FeedAdapter(
    private val onDetailClick: (Post) -> Unit,
    private val onApplyClick: (Post) -> Unit
) : ListAdapter<Post, FeedAdapter.FeedViewHolder>(DiffCallback) {

    // Simpan player aktif agar bisa pause saat scroll
    private var activePlayer: ExoPlayer? = null
    private var activePlayerView: PlayerView? = null

    inner class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivMedia: ImageView      = itemView.findViewById(R.id.ivMedia)
        val pvVideo: PlayerView     = itemView.findViewById(R.id.pvVideo)
        val tvPetName: TextView     = itemView.findViewById(R.id.tvPetName)
        val tvBreedAge: TextView    = itemView.findViewById(R.id.tvBreedAge)
        val tvCity: TextView        = itemView.findViewById(R.id.tvCity)
        val tvStatus: TextView      = itemView.findViewById(R.id.tvStatus)
        val tvFee: TextView         = itemView.findViewById(R.id.tvFee)
        val tvLikesCount: TextView  = itemView.findViewById(R.id.tvLikesCount)
        val ivOwnerAvatar: ImageView= itemView.findViewById(R.id.ivOwnerAvatar)
        val btnApply: TextView      = itemView.findViewById(R.id.btnApply)
        val btnWhatsapp: TextView   = itemView.findViewById(R.id.btnWhatsapp)
        val btnLike: ImageView      = itemView.findViewById(R.id.btnLike)
        val btnDetail: ImageView    = itemView.findViewById(R.id.btnDetail)

        var exoPlayer: ExoPlayer? = null

        fun bind(post: Post) {
            // Info teks
            tvPetName.text    = post.petName
            tvBreedAge.text   = "${post.breed} · ${post.ageDisplay}"
            tvCity.text       = post.city
            tvLikesCount.text = post.likesCount.toString()
            tvStatus.text     = post.status.replaceFirstChar { it.uppercase() }
            tvFee.text        = if (post.isFree) "FREE" else "Rp ${post.adoptionFee}"

            // Media: foto vs video
            if (post.mediaType == "video") {
                ivMedia.visibility = View.GONE
                pvVideo.visibility = View.VISIBLE
                setupVideo(post.mediaUrl)
            } else {
                ivMedia.visibility = View.VISIBLE
                pvVideo.visibility = View.GONE
                releasePlayer()
                if (post.mediaUrl.isNotEmpty()) {
                    ivMedia.load(post.mediaUrl) {
                        crossfade(true)
                        placeholder(R.drawable.placeholder)
                        error(R.drawable.placeholder)
                    }
                } else {
                    ivMedia.setImageResource(R.drawable.placeholder)
                }
            }

            // Avatar owner (placeholder dulu, nanti load dari users collection)
            ivOwnerAvatar.setImageResource(R.drawable.ic_profile_placeholder)

            // Aksi tombol
            btnDetail.setOnClickListener  { onDetailClick(post) }
            btnApply.setOnClickListener   { onApplyClick(post) }
            btnLike.setOnClickListener    { /* TODO: toggle like */ }

            // WhatsApp deep link — buka wa.me/{nomor}
            btnWhatsapp.setOnClickListener {
                // Nomor WA dari owner — untuk sekarang buka WhatsApp saja
                // Nanti diganti dengan data dari users collection
                val intent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/?text=Halo, saya tertarik mengadopsi ${post.petName}"))
                itemView.context.startActivity(intent)
            }
        }

        private fun setupVideo(url: String) {
            releasePlayer()
            val player = ExoPlayer.Builder(itemView.context).build()
            exoPlayer = player
            pvVideo.player = player
            player.setMediaItem(MediaItem.fromUri(url))
            player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
            player.volume = 0f  // mute by default seperti TikTok
            player.prepare()
        }

        fun playVideo() {
            exoPlayer?.let { player ->
                // Pause player sebelumnya
                activePlayer?.pause()
                activePlayerView?.player = null

                pvVideo.player = player
                player.play()
                activePlayer = player
                activePlayerView = pvVideo
            }
        }

        fun pauseVideo() {
            exoPlayer?.pause()
        }

        fun releasePlayer() {
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feed_post, parent, false)
        // Item harus full screen
        view.layoutParams.height = parent.height
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: FeedViewHolder) {
        super.onViewRecycled(holder)
        holder.releasePlayer()
    }

    // Dipanggil dari FeedFragment saat item snap ke tengah
    fun onItemVisible(position: Int) {
        val holder = recyclerView?.findViewHolderForAdapterPosition(position) as? FeedViewHolder
        holder?.playVideo()
    }

    fun onItemInvisible(position: Int) {
        val holder = recyclerView?.findViewHolderForAdapterPosition(position) as? FeedViewHolder
        holder?.pauseVideo()
    }

    // Simpan reference RecyclerView untuk findViewHolderForAdapterPosition
    private var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
        activePlayer?.release()
        activePlayer = null
    }

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(old: Post, new: Post) = old.postId == new.postId
            override fun areContentsTheSame(old: Post, new: Post) = old == new
        }
    }
}