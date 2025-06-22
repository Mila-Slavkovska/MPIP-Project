package com.example.mpip

import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.animation.doOnEnd
import com.example.mpip.domain.UserProgress

class TaskMenuComponent(private val rootView: View) {
    private val menuHeader: LinearLayout = rootView.findViewById(R.id.task_menu_header)
    private val menuItems: LinearLayout = rootView.findViewById(R.id.task_menu_items)
    private val menuArrow: ImageView = rootView.findViewById(R.id.menu_arrow)

    private val dailyCheckinItem: LinearLayout = rootView.findViewById(R.id.menu_daily_checkin)
    private val dailyTasksItem: LinearLayout = rootView.findViewById(R.id.menu_daily_tasks)
    private val taskHistoryItem: LinearLayout = rootView.findViewById(R.id.menu_task_history)
    private val progressCalendarItem: LinearLayout =
        rootView.findViewById(R.id.menu_progress_calendar) // NEW
    private val personalDiaryItem: LinearLayout = rootView.findViewById(R.id.menu_personal_diary)


    private val checkinStatusBadge: TextView = rootView.findViewById(R.id.checkin_status_badge)
    private val tasksCountBadge: TextView = rootView.findViewById(R.id.tasks_count_badge)
    private val diaryStatusBadge: TextView = rootView.findViewById(R.id.diary_status_badge)

    private val streakText: TextView = rootView.findViewById(R.id.menu_streak_text)
    private val pointsText: TextView = rootView.findViewById(R.id.menu_points_text)
    private val completedText: TextView = rootView.findViewById(R.id.menu_completed_text)

    private var isExpanded = false

    var onDailyCheckinClick: (() -> Unit)? = null
    var onDailyTasksClick: (() -> Unit)? = null
    var onTaskHistoryClick: (() -> Unit)? = null
    var onProgressCalendarClick: (() -> Unit)? = null
    var onPersonalDiaryClick: (() -> Unit)? = null


    init {
        setupClickListeners()
    }

    private fun setupClickListeners() {
        menuHeader.setOnClickListener {
            toggleMenu()
        }

        dailyCheckinItem.setOnClickListener {
            onDailyCheckinClick?.invoke()
            collapseMenu()
        }

        dailyTasksItem.setOnClickListener {
            onDailyTasksClick?.invoke()
            collapseMenu()
        }

        taskHistoryItem.setOnClickListener {
            onTaskHistoryClick?.invoke()
            collapseMenu()
        }

        progressCalendarItem.setOnClickListener {
            onProgressCalendarClick?.invoke()
            collapseMenu()
        }

        personalDiaryItem.setOnClickListener {
            onPersonalDiaryClick?.invoke()
            collapseMenu()
        }
    }

    private fun toggleMenu() {
        if (isExpanded) {
            collapseMenu()
        } else {
            expandMenu()
        }
    }

    private fun expandMenu() {
        if (isExpanded) return

        isExpanded = true
        menuItems.visibility = View.VISIBLE

        ObjectAnimator.ofFloat(menuArrow, "rotation", 0f, 180f).apply {
            duration = 200
            start()
        }

        val initialHeight = 0
        val targetHeight = measureMenuHeight()

        val animator = ObjectAnimator.ofInt(initialHeight, targetHeight)
        animator.duration = 250
        animator.addUpdateListener { animation ->
            val height = animation.animatedValue as Int
            val layoutParams = menuItems.layoutParams
            layoutParams.height = height
            menuItems.layoutParams = layoutParams
        }
        animator.start()
    }

    private fun collapseMenu() {
        if (!isExpanded) return

        isExpanded = false

        ObjectAnimator.ofFloat(menuArrow, "rotation", 180f, 0f).apply {
            duration = 200
            start()
        }

        val initialHeight = menuItems.height
        val targetHeight = 0

        val animator = ObjectAnimator.ofInt(initialHeight, targetHeight)
        animator.duration = 200
        animator.addUpdateListener { animation ->
            val height = animation.animatedValue as Int
            val layoutParams = menuItems.layoutParams
            layoutParams.height = height
            menuItems.layoutParams = layoutParams
        }
        animator.doOnEnd {
            menuItems.visibility = View.GONE
            val layoutParams = menuItems.layoutParams
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            menuItems.layoutParams = layoutParams
        }
        animator.start()
    }

    private fun measureMenuHeight(): Int {
        menuItems.measure(
            View.MeasureSpec.makeMeasureSpec(menuItems.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        return menuItems.measuredHeight
    }


    fun updateMenuStatus(
        userProgress: UserProgress,
        hasCheckinToday: Boolean,
        pendingTasksCount: Int,
        completedTasksToday: Int,
        hasDiaryEntryToday: Boolean = false
    ) {
        if (hasCheckinToday) {
            checkinStatusBadge.text = "Done"
            checkinStatusBadge.setBackgroundColor(0xFFE8F5E8.toInt())
            checkinStatusBadge.setTextColor(0xFF00B894.toInt())
        } else {
            checkinStatusBadge.text = "New"
            checkinStatusBadge.setBackgroundColor(0xFFFFF4E6.toInt())
            checkinStatusBadge.setTextColor(0xFFE17055.toInt())
        }

        if (pendingTasksCount > 0) {
            tasksCountBadge.text = pendingTasksCount.toString()
            tasksCountBadge.visibility = View.VISIBLE
        } else {
            tasksCountBadge.text = "✓"
            tasksCountBadge.setBackgroundColor(0xFFE8F5E8.toInt())
            tasksCountBadge.setTextColor(0xFF00B894.toInt())
        }

        streakText.text = userProgress.currentStreak.toString()
        pointsText.text = userProgress.availablePoints.toString()
        completedText.text = completedTasksToday.toString()

        if (hasDiaryEntryToday) {
            diaryStatusBadge.text = "✓"
            diaryStatusBadge.setBackgroundColor(0xFFE8F5E8.toInt())
            diaryStatusBadge.setTextColor(0xFF00B894.toInt())
        } else {
            diaryStatusBadge.text = "New"
            diaryStatusBadge.setBackgroundColor(0xFFFFF4E6.toInt())
            diaryStatusBadge.setTextColor(0xFFE17055.toInt())
        }
    }

    fun forceCollapse() {
        if (isExpanded) {
            collapseMenu()
        }
    }
}