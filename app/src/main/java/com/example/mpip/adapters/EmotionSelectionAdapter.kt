package com.example.mpip.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mpip.R
import com.example.mpip.domain.Emotion

class EmotionSelectionAdapter(
    private val emotions: List<Emotion>,
    private val onSelectionChanged: (Emotion, Boolean) -> Unit
) : RecyclerView.Adapter<EmotionSelectionAdapter.EmotionViewHolder>() {
    private val selectedEmotions = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmotionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_emotion_selection, parent, false)
        return EmotionViewHolder(view)
    }

    override fun onBindViewHolder(holder: EmotionViewHolder, position: Int) {
        val emotion = emotions[position]
        holder.bind(emotion, selectedEmotions.contains(emotion.id))
    }

    override fun getItemCount(): Int = emotions.size

    inner class EmotionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: View = itemView.findViewById(R.id.emotion_container)
        private val emoji: TextView = itemView.findViewById(R.id.emotion_emoji)
        private val name: TextView = itemView.findViewById(R.id.emotion_name)
        private val selectedIndicator: View = itemView.findViewById(R.id.selected_indicator)

        fun bind(emotion: Emotion, isSelected: Boolean) {
            emoji.text = emotion.emoji
            name.text = emotion.name

            // Update selection state
            updateSelectionState(isSelected)

            container.setOnClickListener {
                val newState = !selectedEmotions.contains(emotion.id)

                if (newState) {
                    selectedEmotions.add(emotion.id)
                } else {
                    selectedEmotions.remove(emotion.id)
                }

                updateSelectionState(newState)
                onSelectionChanged(emotion, newState)
            }
        }

        private fun updateSelectionState(isSelected: Boolean) {
            if (isSelected) {
                container.setBackgroundResource(R.drawable.bg_emotion_selected)
                selectedIndicator.visibility = View.VISIBLE
                name.setTextColor(itemView.context.getColor(android.R.color.white))
            } else {
                container.setBackgroundResource(R.drawable.bg_emotion_unselected)
                selectedIndicator.visibility = View.GONE
                name.setTextColor(itemView.context.getColor(R.color.text_primary))
            }
        }
    }
}