package com.example.englishword.dialog

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.englishword.R
import com.example.englishword.model.RelWord
import com.example.englishword.model.Word
import com.google.android.flexbox.FlexboxLayout
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class WordDetailDialog : DialogFragment() {

    private var word: String = ""
    private var sourceWord: String? = null
    private var onAudioPlay: ((String) -> Unit)? = null
    private var onWordLookup: ((String, String?) -> Unit)? = null
    private var localWordFinder: ((String) -> Word?)? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val API_KEY = "ab432345-6d31-470d-a3e4-898920ce5069"
        private const val API_URL = "https://api.ruseo.cn/api/englishwords"

        fun newInstance(
            word: String,
            sourceWord: String?,
            onAudioPlay: (String) -> Unit,
            onWordLookup: (String, String?) -> Unit,
            localWordFinder: (String) -> Word?
        ): WordDetailDialog {
            return WordDetailDialog().apply {
                this.word = word
                this.sourceWord = sourceWord
                this.onAudioPlay = onAudioPlay
                this.onWordLookup = onWordLookup
                this.localWordFinder = localWordFinder
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_EnglishWord)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
        }
        dialog.setCanceledOnTouchOutside(true)
        dialog.setCancelable(true)
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val displayMetrics = resources.displayMetrics
            val margin = (displayMetrics.widthPixels * 0.03).toInt()
            val height = (displayMetrics.heightPixels * 0.70).toInt()
            setLayout(displayMetrics.widthPixels - margin * 2, height)
            setGravity(Gravity.CENTER)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.25f)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_word_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val modalWord = view.findViewById<TextView>(R.id.modalWord)
        val modalPhonetic = view.findViewById<TextView>(R.id.modalPhonetic)
        val modalPos = view.findViewById<TextView>(R.id.modalPos)
        val modalTrans = view.findViewById<TextView>(R.id.modalTrans)
        val modalSource = view.findViewById<TextView>(R.id.modalSource)
        val modalNotFound = view.findViewById<TextView>(R.id.modalNotFound)
        val modalAudioBtn = view.findViewById<TextView>(R.id.modalAudioBtn)
        val modalRelWords = view.findViewById<LinearLayout>(R.id.modalRelWords)
        val modalRelWordsGrid = view.findViewById<FlexboxLayout>(R.id.modalRelWordsGrid)
        val modalSynonyms = view.findViewById<LinearLayout>(R.id.modalSynonyms)
        val modalSynonymsList = view.findViewById<FlexboxLayout>(R.id.modalSynonymsList)
        val modalSentences = view.findViewById<LinearLayout>(R.id.modalSentences)
        val modalSentencesList = view.findViewById<LinearLayout>(R.id.modalSentencesList)
        val modalPhrases = view.findViewById<LinearLayout>(R.id.modalPhrases)
        val modalPhrasesList = view.findViewById<FlexboxLayout>(R.id.modalPhrasesList)
        val modalCloseBtn = view.findViewById<TextView>(R.id.modalCloseBtn)

        // Setup word
        modalWord.text = word

        // Clear all sections
        clearModalDetails(view)

        // Try local data first
        val found = localWordFinder?.invoke(word)
        if (found != null) {
            showLocalWord(view, found)
        } else {
            // Fallback to API
            modalTrans.text = getString(R.string.querying)
            val sourceText = sourceWord?.let { src ->
                getString(R.string.source_linking).replace("%s", src)
            } ?: "正在联网查询…"
            modalSource.text = sourceText
            fetchWordFromAPI(view)
        }

        // Audio button
        modalAudioBtn.setOnClickListener {
            onAudioPlay?.invoke(word)
        }

        // Close button
        modalCloseBtn.setOnClickListener {
            dismiss()
        }
    }

    private fun showLocalWord(view: View, word: Word) {
        val modalPhonetic = view.findViewById<TextView>(R.id.modalPhonetic)
        val modalPos = view.findViewById<TextView>(R.id.modalPos)
        val modalTrans = view.findViewById<TextView>(R.id.modalTrans)
        val modalSource = view.findViewById<TextView>(R.id.modalSource)
        val modalAudioBtn = view.findViewById<TextView>(R.id.modalAudioBtn)

        modalPhonetic.text = "/${word.usphone.replace("'", "")}/"
        modalPos.text = word.pos
        modalTrans.text = word.trans
        modalSource.text = getString(R.string.source_from).replace("%s", word.bookId)
        modalAudioBtn.visibility = View.VISIBLE

        populateDetailSections(view, word)
    }

    private fun clearModalDetails(view: View) {
        view.findViewById<LinearLayout>(R.id.modalRelWords).visibility = View.GONE
        view.findViewById<FlexboxLayout>(R.id.modalRelWordsGrid).removeAllViews()
        view.findViewById<LinearLayout>(R.id.modalSynonyms).visibility = View.GONE
        view.findViewById<FlexboxLayout>(R.id.modalSynonymsList).removeAllViews()
        view.findViewById<LinearLayout>(R.id.modalSentences).visibility = View.GONE
        view.findViewById<LinearLayout>(R.id.modalSentencesList).removeAllViews()
        view.findViewById<LinearLayout>(R.id.modalPhrases).visibility = View.GONE
        view.findViewById<FlexboxLayout>(R.id.modalPhrasesList).removeAllViews()
        view.findViewById<TextView>(R.id.modalNotFound).visibility = View.GONE
        view.findViewById<TextView>(R.id.modalAudioBtn).visibility = View.GONE
    }

    private fun populateDetailSections(view: View, word: Word) {
        // Related Words
        if (word.relWord != null && word.relWord.isNotEmpty()) {
            view.findViewById<LinearLayout>(R.id.modalRelWords).visibility = View.VISIBLE
            val grid = view.findViewById<FlexboxLayout>(R.id.modalRelWordsGrid)
            grid.removeAllViews()
            for ((pos, relWords) in word.relWord) {
                for (rw in relWords) {
                    grid.addView(createFormTag(pos, rw))
                }
            }
        }

        // Synonyms
        if (word.syno != null && word.syno.words.isNotEmpty()) {
            view.findViewById<LinearLayout>(R.id.modalSynonyms).visibility = View.VISIBLE
            val list = view.findViewById<FlexboxLayout>(R.id.modalSynonymsList)
            list.removeAllViews()
            for (syn in word.syno.words) {
                list.addView(createSynoChip(word.syno.pos, syn))
            }
        }

        // Sentences
        if (word.sentences.isNotEmpty()) {
            view.findViewById<LinearLayout>(R.id.modalSentences).visibility = View.VISIBLE
            val list = view.findViewById<LinearLayout>(R.id.modalSentencesList)
            list.removeAllViews()
            for (s in word.sentences) {
                list.addView(createSentenceView(s.en, s.cn))
            }
        }

        // Phrases
        if (word.phrases.isNotEmpty()) {
            view.findViewById<LinearLayout>(R.id.modalPhrases).visibility = View.VISIBLE
            val list = view.findViewById<FlexboxLayout>(R.id.modalPhrasesList)
            list.removeAllViews()
            for (p in word.phrases) {
                list.addView(createPhraseView(p.en, p.cn))
            }
        }
    }

    private fun fetchWordFromAPI(view: View) {
        executor.execute {
            try {
                val url = URL("$API_URL?word=${java.net.URLEncoder.encode(word, "UTF-8")}&key=$API_KEY")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                conn.disconnect()

                val json = JSONObject(sb.toString())
                val data = if (json.has("data")) json.getJSONObject("data") else json

                handler.post {
                    if (data.has("success") && !data.getBoolean("success")) {
                        showNotFound(view)
                        return@post
                    }

                    val phonetic = when {
                        data.has("us_phone") && !data.isNull("us_phone") -> "/${data.getString("us_phone")}/"
                        data.has("uk_phone") && !data.isNull("uk_phone") -> "/${data.getString("uk_phone")}/"
                        else -> ""
                    }

                    val transBuilder = StringBuilder()
                    if (data.has("translations")) {
                        val translations = data.getJSONArray("translations")
                        for (i in 0 until translations.length()) {
                            val t = translations.getJSONObject(i)
                            if (t.has("tran_cn") && !t.isNull("tran_cn")) {
                                if (transBuilder.isNotEmpty()) transBuilder.append("；")
                                transBuilder.append(t.getString("tran_cn"))
                            }
                        }
                    }

                    val posBuilder = StringBuilder()
                    if (data.has("translations")) {
                        val translations = data.getJSONArray("translations")
                        for (i in 0 until translations.length()) {
                            val t = translations.getJSONObject(i)
                            if (t.has("pos") && !t.isNull("pos")) {
                                if (posBuilder.isNotEmpty()) posBuilder.append(" · ")
                                posBuilder.append(t.getString("pos"))
                            }
                        }
                    }

                    view.findViewById<TextView>(R.id.modalPhonetic).text = phonetic
                    view.findViewById<TextView>(R.id.modalPos).text = posBuilder.toString()
                    view.findViewById<TextView>(R.id.modalTrans).text = transBuilder.toString().ifBlank { "（未找到释义）" }

                    val sourceText = sourceWord?.let { src ->
                        getString(R.string.source_linked).replace("%s", src)
                    } ?: "澄曜词典"
                    view.findViewById<TextView>(R.id.modalSource).text = sourceText
                    view.findViewById<TextView>(R.id.modalAudioBtn).visibility = View.VISIBLE
                    view.findViewById<TextView>(R.id.modalNotFound).visibility = View.GONE

                    // Fill detail sections from API data
                    populateApiDetails(view, data)

                }
            } catch (e: Exception) {
                handler.post {
                    showNotFound(view)
                }
            }
        }
    }

    private fun showNotFound(view: View) {
        view.findViewById<TextView>(R.id.modalTrans).text = ""
        view.findViewById<TextView>(R.id.modalSource).text = if (sourceWord != null) {
            "由 $sourceWord 关联"
        } else ""
        view.findViewById<TextView>(R.id.modalNotFound).visibility = View.VISIBLE
        view.findViewById<TextView>(R.id.modalAudioBtn).visibility = View.GONE
    }

    private fun populateApiDetails(view: View, data: JSONObject) {
        // Rel words
        if (data.has("rel_words") && !data.isNull("rel_words")) {
            val relWordsArr = data.getJSONArray("rel_words")
            if (relWordsArr.length() > 0) {
                view.findViewById<LinearLayout>(R.id.modalRelWords).visibility = View.VISIBLE
                val grid = view.findViewById<FlexboxLayout>(R.id.modalRelWordsGrid)
                grid.removeAllViews()

                for (i in 0 until relWordsArr.length()) {
                    val group = relWordsArr.getJSONObject(i)
                    val pos = group.optString("Pos", group.optString("pos", ""))
                    val hwds = group.optJSONArray("Hwds")
                    if (hwds != null) {
                        for (j in 0 until hwds.length()) {
                            val h = hwds.getJSONObject(j)
                            val hwd = h.optString("hwd", "")
                            val tran = h.optString("tran", "")
                            grid.addView(createFormTag(pos.lowercase(), RelWord(hwd, tran)))
                        }
                    }
                }
            }
        }

        // Synonyms
        if (data.has("synonyms") && !data.isNull("synonyms")) {
            val synoArr = data.getJSONArray("synonyms")
            if (synoArr.length() > 0) {
                view.findViewById<LinearLayout>(R.id.modalSynonyms).visibility = View.VISIBLE
                val list = view.findViewById<FlexboxLayout>(R.id.modalSynonymsList)
                list.removeAllViews()

                for (i in 0 until synoArr.length()) {
                    val g = synoArr.getJSONObject(i)
                    val pos = g.optString("pos", "")
                    val hwds = g.optJSONArray("Hwds")
                    if (hwds != null) {
                        for (j in 0 until hwds.length()) {
                            val h = hwds.getJSONObject(j)
                            val sw = h.optString("word", h.optString("w", ""))
                            list.addView(createSynoChip(pos, sw))
                        }
                    }
                }
            }
        }

        // Sentences
        if (data.has("sentences") && !data.isNull("sentences")) {
            val sentArr = data.getJSONArray("sentences")
            if (sentArr.length() > 0) {
                view.findViewById<LinearLayout>(R.id.modalSentences).visibility = View.VISIBLE
                val list = view.findViewById<LinearLayout>(R.id.modalSentencesList)
                list.removeAllViews()

                for (i in 0 until sentArr.length()) {
                    val s = sentArr.getJSONObject(i)
                    val en = s.optString("s_content", s.optString("en", ""))
                    val cn = s.optString("s_cn", s.optString("cn", ""))
                    list.addView(createSentenceView(en, cn))
                }
            }
        }

        // Phrases
        if (data.has("phrases") && !data.isNull("phrases")) {
            val phrArr = data.getJSONArray("phrases")
            if (phrArr.length() > 0) {
                view.findViewById<LinearLayout>(R.id.modalPhrases).visibility = View.VISIBLE
                val list = view.findViewById<FlexboxLayout>(R.id.modalPhrasesList)
                list.removeAllViews()

                for (i in 0 until phrArr.length()) {
                    val p = phrArr.getJSONObject(i)
                    val en = p.optString("p_content", p.optString("en", ""))
                    val cn = p.optString("p_cn", p.optString("cn", ""))
                    list.addView(createPhraseView(en, cn))
                }
            }
        }
    }

    // --- View creation helpers (same pattern as adapter) ---

    private fun createFormTag(pos: String, rw: RelWord): View {
        val density = resources.displayMetrics.density
        val tag = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.form_tag_background)
            setPadding((11 * density).toInt(), (5 * density).toInt(), (11 * density).toInt(), (5 * density).toInt())
            layoutParams = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, (5 * density).toInt(), (5 * density).toInt())
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                onWordLookup?.invoke(rw.hwd, this@WordDetailDialog.word)
            }
        }

        val posText = TextView(requireContext()).apply {
            text = "$pos."
            textSize = 10f
            setTextColor(Color.parseColor("#3B5998"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        tag.addView(posText)

        val wordText = TextView(requireContext()).apply {
            text = rw.hwd
            textSize = 13f
            setTextColor(Color.parseColor("#1C1C1C"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding((5 * density).toInt(), 0, 0, 0)
        }
        tag.addView(wordText)

        if (rw.tran.isNotBlank()) {
            val tranText = TextView(requireContext()).apply {
                text = rw.tran
                textSize = 11f
                setTextColor(Color.parseColor("#9C9690"))
                setPadding((5 * density).toInt(), 0, 0, 0)
            }
            tag.addView(tranText)
        }

        return tag
    }

    private fun createSynoChip(pos: String, word: String): View {
        val density = resources.displayMetrics.density
        val chip = TextView(requireContext()).apply {
            text = "$pos. $word"
            textSize = 13f
            setTextColor(Color.parseColor("#5B4A8A"))
            setBackgroundResource(R.drawable.syno_chip_background)
            setPadding((10 * density).toInt(), (3 * density).toInt(), (10 * density).toInt(), (3 * density).toInt())
            gravity = Gravity.CENTER
            layoutParams = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, (4 * density).toInt(), (4 * density).toInt())
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                onWordLookup?.invoke(word, this@WordDetailDialog.word)
            }
        }
        return chip
    }

    private fun createSentenceView(en: String, cn: String): View {
        val density = resources.displayMetrics.density
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.sentence_background)
            setPadding((14 * density).toInt(), (10 * density).toInt(), (14 * density).toInt(), (10 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (6 * density).toInt()
            }
        }

        val enText = TextView(requireContext()).apply {
            text = en
            textSize = 14f
            setTextColor(Color.parseColor("#1C1C1C"))
            setLineSpacing(0f, 1.5f)
        }
        container.addView(enText)

        val cnText = TextView(requireContext()).apply {
            text = cn
            textSize = 12f
            setTextColor(Color.parseColor("#9C9690"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (3 * density).toInt()
            }
        }
        container.addView(cnText)

        return container
    }

    private fun createPhraseView(en: String, cn: String): View {
        val density = resources.displayMetrics.density
        val tag = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.phrase_background)
            setPadding((11 * density).toInt(), (5 * density).toInt(), (11 * density).toInt(), (5 * density).toInt())
            layoutParams = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, (4 * density).toInt(), (4 * density).toInt())
            }
        }

        val enText = TextView(requireContext()).apply {
            text = en
            textSize = 13f
            setTextColor(Color.parseColor("#1C1C1C"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        tag.addView(enText)

        val cnText = TextView(requireContext()).apply {
            text = cn
            textSize = 11f
            setTextColor(Color.parseColor("#9C9690"))
            setPadding((6 * density).toInt(), 0, 0, 0)
        }
        tag.addView(cnText)

        return tag
    }
}
