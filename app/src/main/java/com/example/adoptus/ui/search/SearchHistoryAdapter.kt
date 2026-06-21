package com.example.adoptus.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.adoptus.R

class SearchHistoryAdapter(
    private var historyList: List<String>,
    private val onHistoryClick: (String) -> Unit,
    private val onRemoveClick: (String) -> Unit
) : RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvQuery: TextView = v.findViewById(R.id.tvHistoryQuery)
        val btnRemove: ImageView = v.findViewById(R.id.btnRemoveHistory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val query = historyList[position]
        holder.tvQuery.text = query
        holder.itemView.setOnClickListener { onHistoryClick(query) }
        holder.btnRemove.setOnClickListener { onRemoveClick(query) }
    }

    override fun getItemCount(): Int = historyList.size

    fun updateList(newList: List<String>) {
        historyList = newList
        notifyDataSetChanged()
    }
}
