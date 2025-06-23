package com.example.mpip

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mpip.adapters.DiaryEntriesAdapter
import com.example.mpip.domain.DiaryEntry
import com.example.mpip.domain.DiaryFilter
import com.example.mpip.domain.DiaryStats
import com.example.mpip.domain.enums.diary.DiaryMood
import com.example.mpip.domain.enums.diary.DiarySortOption
import com.example.mpip.repository.Repository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiaryFragment : Fragment() {
    private lateinit var repository: Repository
    private val auth = FirebaseAuth.getInstance()

    private lateinit var backButton: Button
    private lateinit var diaryLogo: ImageView
    private lateinit var searchInput: TextInputEditText
    private lateinit var filterButton: Button
    private lateinit var sortButton: Button
    private lateinit var entriesRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var loadingLayout: LinearLayout
    private lateinit var statsLayout: LinearLayout
    private lateinit var totalEntriesText: TextView
    private lateinit var currentStreakText: TextView
    private lateinit var totalWordsText: TextView
    private lateinit var fabNewEntry: FloatingActionButton

    private lateinit var todayEntryCard: LinearLayout
    private lateinit var todayDateText: TextView
    private lateinit var moodSelector: Spinner
    private lateinit var entryTitleInput: TextInputEditText
    private lateinit var entryContentInput: TextInputEditText
    private lateinit var wordCountText: TextView
    private lateinit var saveEntryButton: Button
    private lateinit var deleteEntryButton: Button

    private lateinit var entriesAdapter: DiaryEntriesAdapter
    private var diaryEntries = mutableListOf<DiaryEntry>()
    private var currentFilter = DiaryFilter()
    private var currentStats = DiaryStats()
    private var todaysEntry: DiaryEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = Repository()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_diary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupRecyclerView()
        setupMoodSelector()
        setupTextWatchers()
        loadTodaysEntry()
        loadDiaryEntries()
        loadDiaryStats()
    }

    private fun initializeViews(view: View) {
        backButton = view.findViewById(R.id.back_button)
        diaryLogo = view.findViewById(R.id.diary_logo)
        searchInput = view.findViewById(R.id.search_input)
        filterButton = view.findViewById(R.id.filter_button)
        sortButton = view.findViewById(R.id.sort_button)

        entriesRecyclerView = view.findViewById(R.id.entries_recycler)
        emptyStateLayout = view.findViewById(R.id.empty_state_layout)
        loadingLayout = view.findViewById(R.id.loading_layout)
        statsLayout = view.findViewById(R.id.stats_layout)
        fabNewEntry = view.findViewById(R.id.fab_new_entry)

        totalEntriesText = view.findViewById(R.id.total_entries_text)
        currentStreakText = view.findViewById(R.id.current_streak_text)
        totalWordsText = view.findViewById(R.id.total_words_text)

        todayEntryCard = view.findViewById(R.id.today_entry_card)
        todayDateText = view.findViewById(R.id.today_date_text)
        moodSelector = view.findViewById(R.id.mood_selector)
        entryTitleInput = view.findViewById(R.id.entry_title_input)
        entryContentInput = view.findViewById(R.id.entry_content_input)
        wordCountText = view.findViewById(R.id.word_count_text)
        saveEntryButton = view.findViewById(R.id.save_entry_button)
        deleteEntryButton = view.findViewById(R.id.delete_entry_button)

        backButton.setOnClickListener {
            (activity as? MainActivity)?.hideDailyTasks()
        }

        filterButton.setOnClickListener {
            showFilterDialog()
        }

        sortButton.setOnClickListener {
            showSortDialog()
        }

        fabNewEntry.setOnClickListener {
            showNewEntryForDate(DiaryEntry.getCurrentDateString())
        }

        saveEntryButton.setOnClickListener {
            saveTodaysEntry()
        }

        deleteEntryButton.setOnClickListener {
            deleteTodaysEntry()
        }

        val today = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
        todayDateText.text = today
    }

    private fun setupRecyclerView() {
        entriesAdapter = DiaryEntriesAdapter(diaryEntries) { entry ->
            showEntryDetails(entry)
        }

        entriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = entriesAdapter
        }
    }

    private fun setupMoodSelector() {
        val moods = DiaryMood.values().map { "${it.emoji} ${it.displayName}" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, moods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        moodSelector.adapter = adapter

        moodSelector.setSelection(DiaryMood.OKAY.ordinal)
    }

    private fun setupTextWatchers() {
        entryContentInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateWordCount()
            }
        })

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applySearch(s.toString())
            }
        })
    }

    private fun loadTodaysEntry() {
        val userId = auth.currentUser?.uid ?: return
        val today = DiaryEntry.getCurrentDateString()

        lifecycleScope.launch {
            try {
                todaysEntry = repository.getDiaryEntry(userId, today)

                if (todaysEntry != null) {
                    populateTodaysEntry(todaysEntry!!)
                    deleteEntryButton.visibility = View.VISIBLE
                } else {
                    clearTodaysEntry()
                    deleteEntryButton.visibility = View.GONE
                }

            } catch (e: Exception) {
                Log.e("DiaryFragment", "Error loading today's entry: ${e.message}")
                clearTodaysEntry()
            }
        }
    }

    private fun populateTodaysEntry(entry: DiaryEntry) {
        entryTitleInput.setText(entry.title)
        entryContentInput.setText(entry.content)

        if (entry.mood.isNotEmpty()) {
            val moodIndex = DiaryMood.values().indexOfFirst { it.displayName == entry.mood }
            if (moodIndex >= 0) {
                moodSelector.setSelection(moodIndex)
            }
        }

        updateWordCount()
        saveEntryButton.text = "Update Entry"
    }

    private fun clearTodaysEntry() {
        entryTitleInput.setText("")
        entryContentInput.setText("")
        moodSelector.setSelection(DiaryMood.OKAY.ordinal)
        updateWordCount()
        saveEntryButton.text = "Save Entry"
    }

    private fun updateWordCount() {
        val content = entryContentInput.text.toString()
        val wordCount = if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
        wordCountText.text = "$wordCount words"
    }

    private fun saveTodaysEntry() {
        val userId = auth.currentUser?.uid ?: return
        val today = DiaryEntry.getCurrentDateString()

        val title = entryTitleInput.text.toString().trim().ifEmpty { "My Day" }
        val content = entryContentInput.text.toString().trim()

        if (content.isEmpty()) {
            Toast.makeText(context, "Please write something first", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedMood = DiaryMood.values()[moodSelector.selectedItemPosition]
        val wordCount = content.split("\\s+".toRegex()).size

        val entry = if (todaysEntry != null) {
            todaysEntry!!.copy(
                title = title,
                content = content,
                mood = selectedMood.displayName,
                moodEmoji = selectedMood.emoji,
                updatedAt = System.currentTimeMillis(),
                wordCount = wordCount
            )
        } else {
            DiaryEntry(
                id = DiaryEntry.generateId(userId, today),
                userId = userId,
                date = today,
                title = title,
                content = content,
                mood = selectedMood.displayName,
                moodEmoji = selectedMood.emoji,
                wordCount = wordCount
            )
        }

        lifecycleScope.launch {
            try {
                val success = repository.saveDiaryEntry(entry)

                if (success) {
                    todaysEntry = entry
                    saveEntryButton.text = "Update Entry"
                    deleteEntryButton.visibility = View.VISIBLE

                    Toast.makeText(context, "Entry saved! 📝", Toast.LENGTH_SHORT).show()

                    loadDiaryEntries()
                    loadDiaryStats()
                } else {
                    Toast.makeText(context, "Failed to save entry", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("DiaryFragment", "Error saving entry: ${e.message}")
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteTodaysEntry() {
        if (todaysEntry == null) return

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Entry")
            .setMessage("Are you sure you want to delete today's entry? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val success = repository.deleteDiaryEntry(todaysEntry!!.id)

                        if (success) {
                            todaysEntry = null
                            clearTodaysEntry()
                            deleteEntryButton.visibility = View.GONE

                            Toast.makeText(context, "Entry deleted", Toast.LENGTH_SHORT).show()

                            loadDiaryEntries()
                            loadDiaryStats()
                        } else {
                            Toast.makeText(context, "Failed to delete entry", Toast.LENGTH_SHORT)
                                .show()
                        }

                    } catch (e: Exception) {
                        Log.e("DiaryFragment", "Error deleting entry: ${e.message}")
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadDiaryEntries() {
        val userId = auth.currentUser?.uid ?: return

        showLoading(true)

        lifecycleScope.launch {
            try {
                val entries = repository.getDiaryEntries(userId, currentFilter)

                diaryEntries.clear()
                diaryEntries.addAll(entries)
                entriesAdapter.notifyDataSetChanged()

                if (entries.isNotEmpty()) {
                    showEntriesList()
                } else {
                    showEmptyState()
                }

                Log.d("DiaryFragment", "Loaded ${entries.size} diary entries")

            } catch (e: Exception) {
                Log.e("DiaryFragment", "Error loading diary entries: ${e.message}")
                Toast.makeText(context, "Error loading entries", Toast.LENGTH_SHORT).show()
                showEmptyState()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun loadDiaryStats() {
        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                currentStats = repository.getDiaryStats(userId)
                updateStatsDisplay()

            } catch (e: Exception) {
                Log.e("DiaryFragment", "Error loading diary stats: ${e.message}")
            }
        }
    }

    private fun updateStatsDisplay() {
        totalEntriesText.text = currentStats.totalEntries.toString()
        currentStreakText.text = currentStats.currentStreak.toString()
        totalWordsText.text = currentStats.totalWords.toString()
    }

    private fun applySearch(query: String) {
        currentFilter = currentFilter.copy(searchText = query)
        loadDiaryEntries()
    }

    private fun showFilterDialog() {
        Toast.makeText(context, "Filter dialog - coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf(
            "Newest first",
            "Oldest first",
            "Longest first",
            "Shortest first",
            "Title A-Z"
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Sort entries by")
            .setItems(sortOptions) { _, which ->
                val sortOption = when (which) {
                    0 -> DiarySortOption.DATE_DESC
                    1 -> DiarySortOption.DATE_ASC
                    2 -> DiarySortOption.WORD_COUNT_DESC
                    3 -> DiarySortOption.WORD_COUNT_ASC
                    4 -> DiarySortOption.TITLE_ASC
                    else -> DiarySortOption.DATE_DESC
                }

                currentFilter = currentFilter.copy(sortBy = sortOption)
                loadDiaryEntries()
            }
            .show()
    }

    private fun showNewEntryForDate(date: String) {
        Toast.makeText(context, "New entry for $date - coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun showEntryDetails(entry: DiaryEntry) {
        val message = buildString {
            append("${entry.title}\n\n")
            append("${entry.content}\n\n")
            append("📅 ${entry.getDisplayDate()}\n")
            append("🕒 Written at ${entry.getCreationTime()}")

            if (entry.updatedAt != entry.createdAt) {
                append(", updated at ${entry.getLastUpdatedTime()}")
            }

            append("\n📝 ${entry.wordCount} words")
            append(" • ~${entry.getReadingTimeMinutes()} min read")

            if (entry.mood.isNotEmpty()) {
                append("\n${entry.moodEmoji} ${entry.mood}")
            }
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Diary Entry")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("Edit") { _, _ ->
                // TODO: Implement edit functionality
                Toast.makeText(context, "Edit feature - coming soon!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showLoading(show: Boolean) {
        loadingLayout.visibility = if (show) View.VISIBLE else View.GONE
        entriesRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        statsLayout.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showEntriesList() {
        emptyStateLayout.visibility = View.GONE
        entriesRecyclerView.visibility = View.VISIBLE
        statsLayout.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        emptyStateLayout.visibility = View.VISIBLE
        entriesRecyclerView.visibility = View.GONE
        statsLayout.visibility = View.GONE
        showLoading(false)
    }

    companion object {
        fun newInstance(): DiaryFragment {
            return DiaryFragment()
        }
    }
}