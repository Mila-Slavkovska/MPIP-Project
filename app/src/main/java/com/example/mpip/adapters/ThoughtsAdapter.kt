package com.example.mpip.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mpip.R

class ThoughtsAdapter(
    private val thoughts: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<ThoughtsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thoughtText: TextView = itemView.findViewById(R.id.thoughtText)
        fun bind(thought: String) {
            thoughtText.text = thought
            itemView.setOnClickListener {
                onClick(thought)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_thought, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(thoughts[position])
    }

    override fun getItemCount(): Int = thoughts.size
}
