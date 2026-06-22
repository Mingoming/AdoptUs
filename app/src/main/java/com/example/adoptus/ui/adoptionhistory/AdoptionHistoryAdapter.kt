package com.example.adoptus.ui.adoptionhistory

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.adoptus.R
import com.example.adoptus.data.model.Adoption
import java.text.SimpleDateFormat
import java.util.Locale

class AdoptionHistoryAdapter(
    private val onItemClick: (Adoption) -> Unit
) : ListAdapter<Adoption, AdoptionHistoryAdapter.HistoryViewHolder>(DiffCallback) {

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val petName: TextView = itemView.findViewById(R.id.tvPetName)
        private val status: TextView = itemView.findViewById(R.id.tvStatus)
        private val statusMessage: TextView = itemView.findViewById(R.id.tvStatusMessage)
        private val submittedAt: TextView = itemView.findViewById(R.id.tvSubmittedAt)

        fun bind(adoption: Adoption) {
            petName.text = adoption.petName
            status.text = AdoptionHistoryUi.statusLabel(adoption.status)
            statusMessage.text = AdoptionHistoryUi.statusMessage(adoption.status)
            submittedAt.text = adoption.createdAt?.toDate()?.let { dateFormatter.format(it) }
                ?: "Tanggal pengajuan belum tersedia"

            val context = itemView.context
            status.setTextColor(
                when (adoption.status.lowercase()) {
                    "pending" -> ContextCompat.getColor(context, R.color.primary_orange)
                    "approved" -> Color.parseColor("#4CAF50")
                    "rejected" -> Color.parseColor("#F44336")
                    else -> Color.GRAY
                }
            )

            itemView.setOnClickListener { onItemClick(adoption) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_adoption_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))

        val DiffCallback = object : DiffUtil.ItemCallback<Adoption>() {
            override fun areItemsTheSame(old: Adoption, new: Adoption): Boolean =
                old.adoptionId == new.adoptionId

            override fun areContentsTheSame(old: Adoption, new: Adoption): Boolean =
                old == new
        }
    }
}
