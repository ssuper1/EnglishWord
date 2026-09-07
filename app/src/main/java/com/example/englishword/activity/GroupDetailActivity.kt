package com.example.englishword.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.englishword.R
import com.example.englishword.adapter.WordAdapter
import com.example.englishword.data.GroupManager
import com.example.englishword.data.WordJsonParser
import com.example.englishword.dialog.EditGroupWordsDialog
import com.example.englishword.dialog.WordDetailDialog
import com.example.englishword.model.Word
import com.example.englishword.model.WordState
import kotlin.concurrent.thread

class GroupDetailActivity : AppCompatActivity() {

    private lateinit var tvGroupName: TextView
    private lateinit var tvGroupMeta: TextView
    private lateinit var wordRecyclerView: RecyclerView
    private lateinit var emptyState: TextView
    private lateinit var btnBack: TextView
    private lateinit var btnEdit: TextView

    private lateinit var adapter: WordAdapter
    private var groupId: String? = null
    private var groupWords: List<Word> = emptyList()
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val EXTRA_GROUP_ID = "group_id"

        fun start(context: Context, groupId: String) {
            val intent = Intent(context, GroupDetailActivity::class.java).apply {
                putExtra(EXTRA_GROUP_ID, groupId)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_detail)

        groupId = intent.getStringExtra(EXTRA_GROUP_ID)

        tvGroupName = findViewById(R.id.tvGroupName)
        tvGroupMeta = findViewById(R.id.tvGroupMeta)
        wordRecyclerView = findViewById(R.id.wordRecyclerView)
        emptyState = findViewById(R.id.emptyState)
        btnBack = findViewById(R.id.btnBack)
        btnEdit = findViewById(R.id.btnEdit)

        btnBack.setOnClickListener {
            finish()
        }

        btnEdit.setOnClickListener {
            showEditDialog()
        }

        // Setup adapter
        adapter = WordAdapter(
            context = this,
            words = emptyList(),
            onAudioClick = { word -> playAudio(word.headWord) },
            onStateToggle = { word, target -> toggleWordState(word, target) },
            onWordLookup = { word, source -> showWordDetail(word, source) },
            onAddToGroup = null
        )
        wordRecyclerView.adapter = adapter
        wordRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)

        loadGroupWords()
    }

    private fun loadGroupWords() {
        val currentGroupId = groupId ?: return

        thread {
            val group = GroupManager.getAllGroups(this).find { it.id == currentGroupId }

            if (group == null) {
                handler.post {
                    finish()
                }
                return@thread
            }

            // 加载所有单词（不受范围限制）
            val allWords = WordJsonParser.parseWords(this)

            // 筛选出分组内的单词
            groupWords = group.words.mapNotNull { wordInGroup ->
                allWords.find { it.headWord.equals(wordInGroup, ignoreCase = true) }
            }

            android.util.Log.d("GroupDetail", "Group: ${group.name}, words in group: ${group.words.size}, found: ${groupWords.size}")

            handler.post {
                tvGroupName.text = group.name
                tvGroupMeta.text = "${groupWords.size} 词"

                if (groupWords.isEmpty()) {
                    wordRecyclerView.visibility = android.view.View.GONE
                    emptyState.visibility = android.view.View.VISIBLE
                } else {
                    wordRecyclerView.visibility = android.view.View.VISIBLE
                    emptyState.visibility = android.view.View.GONE
                    adapter.updateWords(groupWords)
                }
            }
        }
    }

    private fun toggleWordState(word: Word, target: WordState) {
        word.state = if (word.state == target) WordState.NEW else target
        adapter.updateWords(groupWords)
    }

    private fun playAudio(word: String) {
        try {
            val encodedWord = java.net.URLEncoder.encode(word, "UTF-8")
            val url = "https://dict.youdao.com/dictvoice?audio=$encodedWord&type=1"
            val mediaPlayer = android.media.MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { start() }
                setOnErrorListener { _, _, _ -> true }
                setOnCompletionListener { release() }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showEditDialog() {
        val currentGroupId = groupId ?: return
        val dialog = EditGroupWordsDialog.newInstance(currentGroupId) {
            // 重新加载单词列表
            loadGroupWords()
        }
        dialog.show(supportFragmentManager, "EditGroupWordsDialog")
    }

    private fun showWordDetail(word: String, sourceWord: String?) {
        try {
            val dialog = WordDetailDialog.newInstance(
                word = word,
                sourceWord = sourceWord,
                onAudioPlay = { w -> playAudio(w) },
                onWordLookup = { w, s -> showWordDetail(w, s) },
                localWordFinder = { w -> groupWords.find { it.headWord == w } }
            )
            dialog.show(supportFragmentManager, "WordDetailDialog")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
