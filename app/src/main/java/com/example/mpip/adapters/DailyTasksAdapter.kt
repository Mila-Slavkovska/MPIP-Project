package com.example.mpip.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mpip.R
import com.example.mpip.domain.DailyTask
import com.example.mpip.domain.enums.tasks.TaskAction
import com.example.mpip.domain.enums.tasks.TaskCategory
import com.example.mpip.domain.enums.tasks.TaskDifficulty
import com.example.mpip.domain.enums.tasks.TaskType

class DailyTasksAdapter(
    private val tasks: List<DailyTask>,
    private val onTaskAction: (DailyTask, TaskAction) -> Unit
) : RecyclerView.Adapter<DailyTasksAdapter.TaskViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_daily_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskCard: LinearLayout = itemView.findViewById(R.id.task_card)
        private val taskIcon: TextView = itemView.findViewById(R.id.task_icon)
        private val taskTitle: TextView = itemView.findViewById(R.id.task_title)
        private val taskDescription: TextView = itemView.findViewById(R.id.task_description)
        private val taskCategory: TextView = itemView.findViewById(R.id.task_category)
        private val pointsBadge: TextView = itemView.findViewById(R.id.points_badge)
        private val actionButton: Button = itemView.findViewById(R.id.action_button)
        private val completedIndicator: LinearLayout =
            itemView.findViewById(R.id.completed_indicator)
        private val completedText: TextView = itemView.findViewById(R.id.completed_text)
        private val detailsButton: ImageButton = itemView.findViewById(R.id.details_button)

        fun bind(task: DailyTask) {
            taskTitle.text = task.title
            taskDescription.text = task.description
            pointsBadge.text = "${task.points} pts"

            when (task.category) {
                TaskCategory.QUESTIONNAIRE_BASED -> {
                    taskCategory.text = "Personal Task"
                    taskCategory.setBackgroundResource(R.drawable.bg_category_personal)
                    taskCard.setBackgroundResource(R.drawable.bg_task_card_personal)
                }

                TaskCategory.DAILY_ROUTINE -> {
                    taskCategory.text = "Daily Routine"
                    taskCategory.setBackgroundResource(R.drawable.bg_category_routine)
                    taskCard.setBackgroundResource(R.drawable.bg_task_card_routine)
                }
            }

            when (task.type) {
                TaskType.TEXT -> {
                    taskIcon.text = "✍️"
                    if (!task.completed) {
                        actionButton.text = "Write Response"
                        actionButton.setBackgroundResource(R.drawable.bg_button_text_task)
                    }
                }

                TaskType.PHOTO -> {
                    taskIcon.text = "📸"
                    if (!task.completed) {
                        actionButton.text = "Take Photo"
                        actionButton.setBackgroundResource(R.drawable.bg_button_photo_task)
                    }
                }

                TaskType.SIMPLE_ACTION -> {
                    taskIcon.text = "✅"
                    if (!task.completed) {
                        actionButton.text = "Mark Complete"
                        actionButton.setBackgroundResource(R.drawable.bg_button_simple_task)
                    }
                }
            }

            if (task.completed) {
                showCompletedState(task)
            } else {
                showActiveState(task)
            }

            setupClickListeners(task)
        }

        private fun showCompletedState(task: DailyTask) {
            actionButton.visibility = View.GONE
            completedIndicator.visibility = View.VISIBLE

            val completionTime = if (task.completedAt != null) {
                val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(task.completedAt))
                "Completed at $time"
            } else {
                "Completed"
            }

            completedText.text = completionTime

            taskCard.alpha = 0.7f

            if (task.type == TaskType.TEXT && task.userResponse.isNotEmpty()) {
                val preview = if (task.userResponse.length > 50) {
                    "${task.userResponse.take(50)}..."
                } else {
                    task.userResponse
                }
                taskDescription.text = "${task.description}\n\n💭 \"$preview\""
            }
        }

        private fun showActiveState(task: DailyTask) {
            actionButton.visibility = View.VISIBLE
            completedIndicator.visibility = View.GONE
            taskCard.alpha = 1.0f
        }

        private fun setupClickListeners(task: DailyTask) {
            if (!task.completed) {
                actionButton.setOnClickListener {
                    val action = when (task.type) {
                        TaskType.TEXT -> TaskAction.COMPLETE_TEXT
                        TaskType.PHOTO -> TaskAction.COMPLETE_PHOTO
                        TaskType.SIMPLE_ACTION -> TaskAction.COMPLETE_SIMPLE
                    }
                    onTaskAction(task, action)
                }

                if (task.type == TaskType.SIMPLE_ACTION) {
                    taskCard.setOnClickListener {
                        onTaskAction(task, TaskAction.COMPLETE_SIMPLE)
                    }
                    taskCard.isClickable = true
                    taskCard.isFocusable = true
                }
            }

            detailsButton.setOnClickListener {
                onTaskAction(task, TaskAction.VIEW_DETAILS)
            }
        }
    }
}

private fun TaskDifficulty.getColor(): Int {
    return when (this) {
        TaskDifficulty.EASY -> android.R.color.holo_green_light
        TaskDifficulty.MEDIUM -> android.R.color.holo_orange_light
        TaskDifficulty.HARD -> android.R.color.holo_red_light
    }
}

private fun TaskCategory.getEmoji(): String {
    return when (this) {
        TaskCategory.QUESTIONNAIRE_BASED -> "🎯"
        TaskCategory.DAILY_ROUTINE -> "⚡"
    }
}