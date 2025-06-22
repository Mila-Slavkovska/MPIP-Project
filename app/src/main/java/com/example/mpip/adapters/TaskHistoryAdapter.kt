package com.example.mpip.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mpip.R
import com.example.mpip.domain.TaskHistoryItem
import com.example.mpip.domain.enums.tasks.TaskType

class TaskHistoryAdapter(
    private val historyItems: List<TaskHistoryItem>,
    private val onItemClick: (TaskHistoryItem) -> Unit
) : RecyclerView.Adapter<TaskHistoryAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(historyItems[position])
    }

    override fun getItemCount() = historyItems.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskIcon: TextView = itemView.findViewById(R.id.task_icon)
        private val titleText: TextView = itemView.findViewById(R.id.task_title)
        private val dateText: TextView = itemView.findViewById(R.id.completion_date)
        private val pointsText: TextView = itemView.findViewById(R.id.points_earned)
        private val emotionsText: TextView = itemView.findViewById(R.id.emotions_text)
        private val relationsText: TextView = itemView.findViewById(R.id.relations_text)

        fun bind(historyItem: TaskHistoryItem) {
            val task = historyItem.task

            taskIcon.text = when (task.type) {
                TaskType.TEXT -> "📝"
                TaskType.PHOTO -> "📸"
                TaskType.SIMPLE_ACTION -> "✅"
            }

            titleText.text = task.title
            dateText.text = "${historyItem.completionDate} • ${historyItem.completionTime}"
            pointsText.text = "+${historyItem.pointsEarned} pts"

            if (task.triggeringEmotionNames.isNotEmpty()) {
                emotionsText.text = "🎭 ${task.triggeringEmotionNames.joinToString(", ")}"
                emotionsText.visibility = View.VISIBLE
            } else {
                emotionsText.visibility = View.GONE
            }

            if (task.questionnaireRelations.isNotEmpty()) {
                relationsText.text = "🔗 ${task.questionnaireRelations.joinToString(", ")}"
                relationsText.visibility = View.VISIBLE
            } else {
                relationsText.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onItemClick(historyItem)
            }
        }
    }
}