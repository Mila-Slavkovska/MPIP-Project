package com.example.mpip.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mpip.R
import com.example.mpip.domain.EmotionRelation

class RelationSelectionAdapter(
    private val relations: List<EmotionRelation>,
    private val onSelectionChanged: (EmotionRelation, Boolean) -> Unit
) : RecyclerView.Adapter<RelationSelectionAdapter.RelationViewHolder>() {
    private val selectedRelations = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_relation_selection, parent, false)
        return RelationViewHolder(view)
    }

    override fun onBindViewHolder(holder: RelationViewHolder, position: Int) {
        val relation = relations[position]
        holder.bind(relation, selectedRelations.contains(relation.id))
    }

    override fun getItemCount(): Int = relations.size

    inner class RelationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: View = itemView.findViewById(R.id.relation_container)
        private val name: TextView = itemView.findViewById(R.id.relation_name)
        private val description: TextView = itemView.findViewById(R.id.relation_description)
        private val selectedIndicator: View = itemView.findViewById(R.id.selected_indicator)

        fun bind(relation: EmotionRelation, isSelected: Boolean) {
            name.text = relation.name
            description.text = relation.description

            // Update selection state
            updateSelectionState(isSelected)

            container.setOnClickListener {
                val newState = !selectedRelations.contains(relation.id)

                if (newState) {
                    selectedRelations.add(relation.id)
                } else {
                    selectedRelations.remove(relation.id)
                }

                updateSelectionState(newState)
                onSelectionChanged(relation, newState)
            }
        }

        private fun updateSelectionState(isSelected: Boolean) {
            if (isSelected) {
                container.setBackgroundResource(R.drawable.bg_relation_selected)
                selectedIndicator.visibility = View.VISIBLE
                name.setTextColor(itemView.context.getColor(android.R.color.white))
                description.setTextColor(itemView.context.getColor(android.R.color.white))
            } else {
                container.setBackgroundResource(R.drawable.bg_relation_unselected)
                selectedIndicator.visibility = View.GONE
                name.setTextColor(itemView.context.getColor(R.color.text_primary))
                description.setTextColor(itemView.context.getColor(R.color.text_secondary))
            }
        }
    }
}