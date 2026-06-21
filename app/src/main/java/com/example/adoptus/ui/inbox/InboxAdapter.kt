package com.example.adoptus.ui.inbox

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.adoptus.R
import com.example.adoptus.data.model.Adoption

class InboxAdapter(
    private val onApproveClick: (Adoption) -> Unit,
    private val onRejectClick: (Adoption) -> Unit
) : ListAdapter<Adoption, InboxAdapter.InboxViewHolder>(DiffCallback) {

    inner class InboxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val petName: TextView = itemView.findViewById(R.id.tvPetName)
        private val adopterName: TextView = itemView.findViewById(R.id.tvAdopterName)
        private val status: TextView = itemView.findViewById(R.id.tvStatus)
        private val layoutActions: LinearLayout = itemView.findViewById(R.id.layoutActions)
        private val btnApprove: TextView = itemView.findViewById(R.id.btnApprove)
        private val btnReject: TextView = itemView.findViewById(R.id.btnReject)

        fun bind(adoption: Adoption) {
            petName.text = adoption.petName
            adopterName.text = "Pelamar: ${adoption.adopterName}"
            status.text = adoption.status.uppercase()

            // Atur warna status badge
            val context = itemView.context
            when (adoption.status) {
                "pending" -> {
                    status.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.primary_orange))
                    layoutActions.visibility = View.VISIBLE
                }
                "approved" -> {
                    status.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Green
                    layoutActions.visibility = View.GONE
                }
                "rejected" -> {
                    status.setTextColor(android.graphics.Color.parseColor("#F44336")) // Red
                    layoutActions.visibility = View.GONE
                }
                else -> {
                    status.setTextColor(android.graphics.Color.GRAY)
                    layoutActions.visibility = View.GONE
                }
            }

            btnApprove.setOnClickListener { onApproveClick(adoption) }
            btnReject.setOnClickListener { onRejectClick(adoption) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InboxViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inbox_request, parent, false)
        return InboxViewHolder(view)
    }

    override fun onBindViewHolder(holder: InboxViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<Adoption>() {
            override fun areItemsTheSame(old: Adoption, new: Adoption): Boolean =
                old.adoptionId == new.adoptionId

            override fun areContentsTheSame(old: Adoption, new: Adoption): Boolean =
                old == new
        }
    }
}
