package com.example.englishword.fragment

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.englishword.R
import com.example.englishword.adapter.WordAdapter
import com.example.englishword.data.DictManager
import com.example.englishword.data.GroupManager
import com.example.englishword.data.UniversalDictParser
import com.example.englishword.data.WordJsonParser
import com.example.englishword.dialog.AddToGroupDialog
import com.example.englishword.dialog.DictSelectorDialog
import com.example.englishword.dialog.SettingsDialog
import com.example.englishword.dialog.WordDetailDialog
import com.example.englishword.model.Word
import com.example.englishword.model.WordState
import java.io.File
import kotlin.concurrent.thread

class WordListFragment : Fragment() {

    private lateinit var wordRecyclerView: RecyclerView
    private lateinit var headerMeta: TextView
    private lateinit var emptyState: TextView
    private lateinit var btnSettings: TextView
    private lateinit var btnDictSelector: TextView

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
    private var currentDictId: String? = null
    private var currentDict: DictManager.ImportedDict? = null

    companion object {
        private const val PREFS_NAME = "wordmate_prefs"
        private const val KEY_RANGE_START = "range_start"
        private const val KEY_RANGE_END = "range_end"
        private const val KEY_SELECTED_DICT = "selected_dict_id"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_word_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load saved range and dict
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        rangeStart = prefs.getInt(KEY_RANGE_START, 0)
        rangeEnd = prefs.getInt(KEY_RANGE_END, 10)
        currentDictId = prefs.getString(KEY_SELECTED_DICT, "builtin")

        // Initialize views
        wordRecyclerView = view.findViewById(R.id.wordRecyclerView)
        headerMeta = view.findViewById(R.id.headerMeta)
        emptyState = view.findViewById(R.id.emptyState)
        btnSettings = view.findViewById(R.id.btnSettings)
        btnDictSelector = view.findViewById(R.id.btnDictSelector)

        chipAll = view.findViewById(R.id.chipAll)
        chipReview = view.findViewById(R.id.chipReview)
        chipNew = view.findViewById(R.id.chipNew)
        chipKnown = view.findViewById(R.id.chipKnown)

        searchEditText = view.findViewById(R.id.searchEditText)
        clearButton = view.findViewById(R.id.clearButton)

        // Setup search
        setupSearch()

        // Setup adapter (empty initially)
        adapter = WordAdapter(
            context = requireContext(),
            words = emptyList(),
            onAudioClick = { word -> playAudio(word.headWord) },
            onStateToggle = { word, target -> toggleWordState(word, target) },
            onWordLookup = { word, source -> showWordDetail(word, source) },
            onAddToGroup = { word -> showAddToGroupDialog(word) }
        )
        wordRecyclerView.adapter = adapter

        // Setup filter chips
        setupFilterChips()
        selectChip(chipAll)

        // Settings button
        btnSettings.setOnClickListener { showSettings() }

        // Dict selector button
        btnDictSelector.setOnClickListener { showDictSelector() }

        // Load words in background
        loadWords()
    }

    private fun loadWords() {
        android.util.Log.d("WordListFragment", "loadWords called, currentDictId=$currentDictId")
        thread {
            if (currentDictId == "builtin" || currentDictId == null) {
                // Load built-in IELTS vocabulary
                android.util.Log.d("WordListFragment", "Loading builtin IELTS vocabulary")
                allWords = WordJsonParser.parseWords(requireContext())
                currentDict = null
                android.util.Log.d("WordListFragment", "Builtin loaded: ${allWords.size} words")
            } else {
                // Load imported dictionary
                android.util.Log.d("WordListFragment", "Looking for imported dict: $currentDictId")
                val dict = DictManager.getAllDictionaries(requireContext())
                    .find { it.id == currentDictId && it.isEnabled }

                android.util.Log.d("WordListFragment", "Found dict: $dict")

                if (dict != null) {
                    currentDict = dict
                    android.util.Log.d("WordListFragment", "Loading words from dict: ${dict.name}")
                    allWords = loadWordsFromDict(dict)
                    android.util.Log.d("WordListFragment", "Loaded ${allWords.size} words from dict")
                } else {
                    // Fallback to builtin if dict not found or disabled
                    android.util.Log.w("WordListFragment", "Dict not found or disabled, falling back to builtin")
                    currentDictId = "builtin"
                    currentDict = null
                    allWords = WordJsonParser.parseWords(requireContext())
                    saveSelectedDict("builtin")
                }
            }

            handler.post {
                android.util.Log.d("WordListFragment", "Updating UI with ${allWords.size} words")
                updateHeader()
                applyFilter()
            }
        }
    }

    private fun loadWordsFromDict(dict: DictManager.ImportedDict): List<Word> {
        val words = mutableListOf<Word>()
        val file = File(requireContext().filesDir, "imported_dicts/${dict.id}.txt")

        android.util.Log.d("WordListFragment", "Loading from file: ${file.absolutePath}")
        android.util.Log.d("WordListFragment", "File exists: ${file.exists()}")

        if (file.exists()) {
            android.util.Log.d("WordListFragment", "File size: ${file.length()} bytes")
            var lineCount = 0
            file.forEachLine { line ->
                lineCount++
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    val result = UniversalDictParser.detectAndParse(trimmed)
                    if (result != null) {
                        words.add(Word(
                            headWord = result.word,
                            trans = result.translation,
                            usphone = result.phonetic ?: "",
                            state = WordState.NEW
                        ))
                        if (words.size <= 3) {
                            android.util.Log.d("WordListFragment", "Parsed word #${words.size}: ${result.word}")
                        }
                    } else {
                        if (lineCount <= 3) {
                            android.util.Log.w("WordListFragment", "Failed to parse line $lineCount: ${trimmed.take(80)}")
                        }
                    }
                }
            }
            android.util.Log.d("WordListFragment", "Total lines processed: $lineCount, words parsed: ${words.size}")
        } else {
            android.util.Log.e("WordListFragment", "Dict file does not exist: ${file.absolutePath}")
        }

        return words
    }

    private fun updateHeader() {
        val dictName = if (currentDict != null) {
            currentDict!!.name
        } else {
            "IELTS 核心词汇"
        }

        val wordsInRange = getWordsInRange()
        headerMeta.text = "$dictName · ${wordsInRange.size} / ${allWords.size} 词"
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
        val frag = parentFragmentManager.findFragmentByTag("SettingsDialog")
        if (frag != null) return
        try {
            val dialog = SettingsDialog.newInstance(
                startPercent = rangeStart,
                endPercent = rangeEnd,
                totalWords = allWords.size,
                onApply = { start, end ->
                    rangeStart = start
                    rangeEnd = end
                    requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putInt(KEY_RANGE_START, start)
                        .putInt(KEY_RANGE_END, end)
                        .apply()
                    updateHeader()
                    applyFilter()
                }
            )
            dialog.show(parentFragmentManager, "SettingsDialog")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showDictSelector() {
        try {
            val dialog = DictSelectorDialog.newInstance(currentDictId) { selectedDict ->
                if (selectedDict != null) {
                    currentDictId = selectedDict.id
                    saveSelectedDict(selectedDict.id)
                    loadWords()
                }
            }
            dialog.show(parentFragmentManager, "DictSelectorDialog")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveSelectedDict(dictId: String) {
        requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_DICT, dictId)
            .apply()
    }

    private fun showAddToGroupDialog(word: Word) {
        try {
            val dialog = AddToGroupDialog.newInstance(word.headWord) { group ->
                GroupManager.addWordToGroup(requireContext(), group.id, word.headWord)
                android.widget.Toast.makeText(
                    requireContext(),
                    "已添加「${word.headWord}」到分组「${group.name}」",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            dialog.show(parentFragmentManager, "AddToGroupDialog")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showWordDetail(word: String, sourceWord: String?) {
        try {
            val oldFrag = parentFragmentManager.findFragmentByTag("WordDetailDialog")
            if (oldFrag != null) {
                (oldFrag as? androidx.fragment.app.DialogFragment)?.dismissAllowingStateLoss()
            }
            handler.postDelayed({
                try {
                    val dialog = WordDetailDialog.newInstance(
                        word = word,
                        sourceWord = sourceWord,
                        onAudioPlay = { w -> playAudio(w) },
                        onWordLookup = { w, s -> showWordDetail(w, s) },
                        localWordFinder = { w -> allWords.find { it.headWord.equals(w, ignoreCase = true) } }
                    )
                    dialog.show(parentFragmentManager, "WordDetailDialog")
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
