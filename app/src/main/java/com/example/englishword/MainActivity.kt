package com.example.englishword

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.englishword.adapter.WordAdapter
import com.example.englishword.data.WordJsonParser
import com.example.englishword.dialog.SettingsDialog
import com.example.englishword.dialog.WordDetailDialog
import com.example.englishword.model.Word
import com.example.englishword.model.WordState
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.DialogFragment
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var wordRecyclerView: RecyclerView
    private lateinit var headerMeta: TextView
    private lateinit var emptyState: TextView
    private lateinit var btnSettings: TextView

    private lateinit var chipAll: TextView
    private lateinit var chipReview: TextView
    private lateinit var chipNew: TextView
    private lateinit var chipKnown: TextView

    private lateinit var searchEditText: EditText
    private lateinit var clearButton: TextView
    private var searchQuery: String = ""

    private lateinit var adapter: WordAdapter
    private var currentFilter: String = "all"
    private var mediaPlayer: MediaPlayer? = null
    private var allWords: List<Word> = emptyList()
    private var rangeStart = 0
    private var rangeEnd = 10
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val PREFS_NAME = "wordmate_prefs"
        private const val KEY_RANGE_START = "range_start"
        private const val KEY_RANGE_END = "range_end"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load saved range
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        rangeStart = prefs.getInt(KEY_RANGE_START, 0)
        rangeEnd = prefs.getInt(KEY_RANGE_END, 10)

        // Initialize views
        wordRecyclerView = findViewById(R.id.wordRecyclerView)
        headerMeta = findViewById(R.id.headerMeta)
        emptyState = findViewById(R.id.emptyState)
        btnSettings = findViewById(R.id.btnSettings)

        chipAll = findViewById(R.id.chipAll)
        chipReview = findViewById(R.id.chipReview)
        chipNew = findViewById(R.id.chipNew)
        chipKnown = findViewById(R.id.chipKnown)

        searchEditText = findViewById(R.id.searchEditText)
        clearButton = findViewById(R.id.clearButton)

        // Setup search
        setupSearch()

        // Setup adapter (empty initially)
        adapter = WordAdapter(
            context = this,
            words = emptyList(),
            onAudioClick = { word -> playAudio(word.headWord) },
            onStateToggle = { word, target -> toggleWordState(word, target) },
            onWordLookup = { word, source -> showWordDetail(word, source) }
        )
        wordRecyclerView.adapter = adapter

        // Setup filter chips
        setupFilterChips()
        selectChip(chipAll)

        // Settings button
        btnSettings.setOnClickListener { showSettings() }

        // Load words from JSON in background
        thread {
            allWords = WordJsonParser.parseWords(this)
            runOnUiThread {
                updateHeader()
                applyFilter()
            }
        }
    }

    private fun updateHeader() {
        val wordsInRange = getWordsInRange()
        headerMeta.text = "IELTS 核心词汇 · ${wordsInRange.size} / ${allWords.size} 词"
    }

    private fun getWordsInRange(): List<Word> {
        if (allWords.isEmpty()) return emptyList()
        val startIdx = (allWords.size * rangeStart / 100.0).toInt().coerceIn(0, allWords.size)
        val endIdx = (allWords.size * rangeEnd / 100.0).toInt().coerceIn(0, allWords.size)
        return allWords.subList(startIdx, endIdx)
    }

    private fun setupFilterChips() {
        val chips = listOf(
            chipAll to "all",
            chipReview to "review",
            chipNew to "new",
            chipKnown to "known"
        )

        for ((chip, filter) in chips) {
            chip.setOnClickListener {
                selectChip(chip)
                currentFilter = filter
                applyFilter()
            }
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim() ?: ""
                clearButton.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                applyFilter()
            }
        })
        clearButton.setOnClickListener {
            searchEditText.setText("")
        }
    }

    private fun selectChip(selectedChip: TextView) {
        for (chip in listOf(chipAll, chipReview, chipNew, chipKnown)) {
            chip.isSelected = chip == selectedChip
        }
    }

    private fun applyFilter() {
        val source = if (searchQuery.isNotEmpty()) {
            allWords.filter {
                it.headWord.contains(searchQuery, ignoreCase = true) ||
                it.trans.contains(searchQuery, ignoreCase = true)
            }
        } else {
            getWordsInRange()
        }

        val filtered = when (currentFilter) {
            "known" -> source.filter { it.state == WordState.KNOWN }
            "review" -> source.filter { it.state == WordState.REVIEW }
            "new" -> source.filter { it.state == WordState.NEW }
            else -> source
        }

        if (filtered.isEmpty()) {
            wordRecyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            wordRecyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }

        adapter.updateWords(filtered)
    }

    private fun toggleWordState(word: Word, target: WordState) {
        word.state = if (word.state == target) WordState.NEW else target
        applyFilter()
    }

    private fun playAudio(word: String) {
        try {
            mediaPlayer?.apply {
                try { stop() } catch (_: Exception) {}
                try { reset() } catch (_: Exception) {}
                try { release() } catch (_: Exception) {}
            }
            mediaPlayer = null

            val encodedWord = java.net.URLEncoder.encode(word, "UTF-8")
            val url = "https://dict.youdao.com/dictvoice?audio=$encodedWord&type=1"
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener {
                    try { start() } catch (_: Exception) {}
                }
                setOnErrorListener { _, _, _ -> true }
                setOnCompletionListener {
                    try { release() } catch (_: Exception) {}
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showSettings() {
        val frag = supportFragmentManager.findFragmentByTag("SettingsDialog")
        if (frag != null) return
        try {
            val dialog = SettingsDialog.newInstance(
                startPercent = rangeStart,
                endPercent = rangeEnd,
                totalWords = allWords.size,
                onApply = { start, end ->
                    rangeStart = start
                    rangeEnd = end
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putInt(KEY_RANGE_START, start)
                        .putInt(KEY_RANGE_END, end)
                        .apply()
                    updateHeader()
                    applyFilter()
                }
            )
            dialog.show(supportFragmentManager, "SettingsDialog")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var detailTagCounter = 0

    private fun showWordDetail(word: String, sourceWord: String?) {
        try {
            // Dismiss existing dialog first
            val oldFrag = supportFragmentManager.findFragmentByTag("WordDetailDialog")
            if (oldFrag != null) {
                (oldFrag as? DialogFragment)?.dismissAllowingStateLoss()
            }
            // Show new dialog after brief delay to let dismiss complete
            handler.postDelayed({
                try {
                    val dialog = WordDetailDialog.newInstance(
                        word = word,
                        sourceWord = sourceWord,
                        onAudioPlay = { w -> playAudio(w) },
                        onWordLookup = { w, s -> showWordDetail(w, s) },
                        localWordFinder = { w -> allWords.find { it.headWord.equals(w, ignoreCase = true) } }
                    )
                    dialog.show(supportFragmentManager, "WordDetailDialog")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, 200)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.apply {
            stop()
            reset()
            release()
        }
        mediaPlayer = null
    }
}
