package com.example.englishword.adapter

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import com.example.englishword.R
import com.example.englishword.model.RelWord
import com.example.englishword.model.Word
import com.example.englishword.model.WordState
import com.google.android.flexbox.FlexboxLayout

class WordAdapter(
    private val context: Context,
    private var words: List<Word>,
    private val onAudioClick: (Word) -> Unit,
    private val onStateToggle: (Word, WordState) -> Unit,
    private val onWordLookup: (String, String?) -> Unit
) : RecyclerView.Adapter<WordAdapter.ViewHolder>() {

    private var expandedPosition = -1

    fun updateWords(newWords: List<Word>) {
        words = newWords
        expandedPosition = -1
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val stateBar: View = itemView.findViewById(R.id.stateBar)
        val leftZone: LinearLayout = itemView.findViewById(R.id.leftZone)
        val rightZone: LinearLayout = itemView.findViewById(R.id.rightZone)
        val tvWord: TextView = itemView.findViewById(R.id.tvWord)
        val tvPhonetic: TextView = itemView.findViewById(R.id.tvPhonetic)
        val tvPos: TextView = itemView.findViewById(R.id.tvPos)
        val tvTrans: TextView = itemView.findViewById(R.id.tvTrans)
        val chevron: TextView = itemView.findViewById(R.id.chevron)
        val detailCard: LinearLayout = itemView.findViewById(R.id.detailCard)
        val btnKnown: TextView = itemView.findViewById(R.id.btnKnown)
        val btnReview: TextView = itemView.findViewById(R.id.btnReview)

        // Sections
        val sectionRelWords: LinearLayout = itemView.findViewById(R.id.sectionRelWords)
        val relWordsContainer: FlexboxLayout = itemView.findViewById(R.id.relWordsContainer)
        val sectionSynonyms: LinearLayout = itemView.findViewById(R.id.sectionSynonyms)
        val synonymsContainer: FlexboxLayout = itemView.findViewById(R.id.synonymsContainer)
        val sectionSentences: LinearLayout = itemView.findViewById(R.id.sectionSentences)
        val sentencesContainer: LinearLayout = itemView.findViewById(R.id.sentencesContainer)
        val sectionPhrases: LinearLayout = itemView.findViewById(R.id.sectionPhrases)
        val phrasesContainer: FlexboxLayout = itemView.findViewById(R.id.phrasesContainer)
        val sectionMemory: LinearLayout = itemView.findViewById(R.id.sectionMemory)
        val tvMemoryMethod: TextView = itemView.findViewById(R.id.tvMemoryMethod)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_word, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = words.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val word = words[position]
        val isExpanded = position == expandedPosition

        // ---- Main row data ----
        holder.tvWord.text = word.headWord
        holder.tvPhonetic.text = if (word.usphone.isNotBlank()) "/${word.usphone.replace("'", "")}/" else ""
        holder.tvPos.text = word.pos
        holder.tvTrans.text = word.trans

        // ---- State styling (color only, no text) ----
        val card = holder.itemView as com.google.android.material.card.MaterialCardView
        val density = context.resources.displayMetrics.density
        when (word.state) {
            WordState.KNOWN -> {
                // Known: faded, reduced, light
                holder.stateBar.visibility = View.VISIBLE
                holder.stateBar.setBackgroundColor(Color.parseColor("#6A9B7A"))
                card.setCardBackgroundColor(Color.parseColor("#F7F7F5"))
                card.cardElevation = 0f
                holder.leftZone.alpha = 0.55f
                holder.rightZone.alpha = 0.55f
                holder.tvWord.textSize = 13f
                holder.tvPhonetic.textSize = 9f
                holder.tvTrans.textSize = 11f
                holder.leftZone.setPadding((6 * density).toInt(), (4 * density).toInt(), 0, (4 * density).toInt())
                holder.rightZone.setPadding((4 * density).toInt(), (3 * density).toInt(), (6 * density).toInt(), (3 * density).toInt())
                holder.tvPos.textSize = 9f
            }
            WordState.REVIEW -> {
                // Review: prominent, warm background, eye-catching
                holder.stateBar.visibility = View.VISIBLE
                holder.stateBar.setBackgroundColor(Color.parseColor("#C2780A"))
                card.setCardBackgroundColor(Color.parseColor("#FFFBF2"))
                card.cardElevation = 2f
                holder.leftZone.alpha = 1.0f
                holder.rightZone.alpha = 1.0f
                holder.tvWord.textSize = 18f
                holder.tvPhonetic.textSize = 11f
                holder.tvTrans.textSize = 15f
                holder.tvWord.setTextColor(Color.parseColor("#5C3710"))
                holder.tvTrans.setTextColor(Color.parseColor("#6B4F10"))
                holder.rightZone.setBackgroundColor(Color.parseColor("#10C2780A"))
                holder.leftZone.setPadding((12 * density).toInt(), (12 * density).toInt(), 0, (12 * density).toInt())
                holder.rightZone.setPadding((10 * density).toInt(), 0, (12 * density).toInt(), 0)
            }
            WordState.NEW -> {
                // New: default white
                holder.stateBar.visibility = View.INVISIBLE
                card.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                card.cardElevation = 1f
                holder.leftZone.alpha = 1.0f
                holder.rightZone.alpha = 1.0f
                holder.tvWord.textSize = 18f
                holder.tvPhonetic.textSize = 11f
                holder.tvTrans.textSize = 14f
                holder.tvWord.setTextColor(Color.parseColor("#1C1C1C"))
                holder.tvTrans.setTextColor(Color.parseColor("#6B6560"))
                holder.rightZone.setBackgroundColor(Color.TRANSPARENT)
                holder.leftZone.setPadding((12 * density).toInt(), (12 * density).toInt(), 0, (12 * density).toInt())
                holder.rightZone.setPadding((10 * density).toInt(), 0, (12 * density).toInt(), 0)
            }
        }

        // ---- Chevron ----
        holder.chevron.rotation = if (isExpanded) 90f else 0f

        // ---- Detail visibility ----
        holder.detailCard.visibility = if (isExpanded) View.VISIBLE else View.GONE

        // ---- Click zones ----
        holder.leftZone.setOnClickListener {
            onAudioClick(word)
        }

        holder.rightZone.apply {
            isClickable = true
            isFocusable = true
            setOnClickListener {
                val oldPos = expandedPosition
                expandedPosition = if (expandedPosition == holder.adapterPosition) -1 else holder.adapterPosition
                notifyItemChanged(oldPos)
                notifyItemChanged(holder.adapterPosition)
            }
        }
        // Prevent child views from stealing touches
        holder.tvTrans.isClickable = false
        holder.tvTrans.isFocusable = false
        holder.chevron.isClickable = false
        holder.chevron.isFocusable = false

        // ---- State buttons in detail section ----
        val knownColor = Color.parseColor("#6A9B7A")
        val reviewColor = Color.parseColor("#C2780A")
        holder.btnKnown.apply {
            setTextColor(knownColor)
            alpha = if (word.state == WordState.KNOWN) 1.0f else 0.4f
            setOnClickListener { onStateToggle(word, WordState.KNOWN) }
        }
        holder.btnReview.apply {
            setTextColor(reviewColor)
            alpha = if (word.state == WordState.REVIEW) 1.0f else 0.4f
            setOnClickListener { onStateToggle(word, WordState.REVIEW) }
        }

        // ---- Populate detail sections if expanded ----
        if (isExpanded) {
            populateDetailSections(holder, word)
        }
    }

    private fun populateDetailSections(holder: ViewHolder, word: Word) {
        // --- Related Words ---
        if (word.relWord != null && word.relWord.isNotEmpty()) {
            holder.sectionRelWords.visibility = View.VISIBLE
            holder.relWordsContainer.removeAllViews()
            for ((pos, relWords) in word.relWord) {
                for (rw in relWords) {
                    holder.relWordsContainer.addView(createFormTag(pos, rw, word.headWord))
                }
            }
        } else {
            holder.sectionRelWords.visibility = View.GONE
        }

        // --- Synonyms ---
        if (word.syno != null && word.syno.words.isNotEmpty()) {
            holder.sectionSynonyms.visibility = View.VISIBLE
            holder.synonymsContainer.removeAllViews()
            for (syn in word.syno.words) {
                holder.synonymsContainer.addView(createSynoChip(word.syno.pos, syn, word.headWord))
            }
        } else {
            holder.sectionSynonyms.visibility = View.GONE
        }

        // --- Sentences ---
        if (word.sentences.isNotEmpty()) {
            holder.sectionSentences.visibility = View.VISIBLE
            holder.sentencesContainer.removeAllViews()
            for (sentence in word.sentences) {
                holder.sentencesContainer.addView(createSentenceView(sentence.en, sentence.cn))
            }
        } else {
            holder.sectionSentences.visibility = View.GONE
        }

        // --- Phrases ---
        if (word.phrases.isNotEmpty()) {
            holder.sectionPhrases.visibility = View.VISIBLE
            holder.phrasesContainer.removeAllViews()
            for (phrase in word.phrases) {
                holder.phrasesContainer.addView(createPhraseView(phrase.en, phrase.cn))
            }
        } else {
            holder.sectionPhrases.visibility = View.GONE
        }

        // --- Memory Method ---
        if (!word.remMethod.isNullOrBlank()) {
            holder.sectionMemory.visibility = View.VISIBLE
            holder.tvMemoryMethod.text = word.remMethod
        } else {
            holder.sectionMemory.visibility = View.GONE
        }
    }

    private fun createFormTag(pos: String, rw: RelWord, sourceWord: String): View {
        val tag = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.form_tag_background)
            setPadding(11.dpToPx())
            layoutParams = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 5.dpToPx(), 5.dpToPx())
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { onWordLookup(rw.hwd, sourceWord) }
        }

        val posText = TextView(context).apply {
            text = "$pos."
            textSize = 10f
            setTextColor(Color.parseColor("#3B5998"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        tag.addView(posText)

        val wordText = TextView(context).apply {
            text = rw.hwd
            textSize = 13f
            setTextColor(Color.parseColor("#1C1C1C"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(5.dpToPx(), 0, 0, 0)
        }
        tag.addView(wordText)

        if (rw.tran.isNotBlank()) {
            val tranText = TextView(context).apply {
                text = rw.tran
                textSize = 11f
                setTextColor(Color.parseColor("#9C9690"))
                setPadding(5.dpToPx(), 0, 0, 0)
            }
            tag.addView(tranText)
        }

        return tag
    }

    private fun createSynoChip(pos: String, word: String, sourceWord: String): View {
        val chip = TextView(context).apply {
            text = "$pos. $word"
            textSize = 13f
            setTextColor(Color.parseColor("#5B4A8A"))
            setBackgroundResource(R.drawable.syno_chip_background)
            setPadding(10.dpToPx(), 3.dpToPx(), 10.dpToPx(), 3.dpToPx())
            gravity = Gravity.CENTER
            layoutParams = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 4.dpToPx(), 4.dpToPx())
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { onWordLookup(word, sourceWord) }
        }
        return chip
    }

    private fun createSentenceView(en: String, cn: String): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.sentence_background)
            setPadding(14.dpToPx(), 10.dpToPx(), 14.dpToPx(), 10.dpToPx())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 6.dpToPx()
            }
        }

        val enText = TextView(context).apply {
            text = en
            textSize = 14f
            setTextColor(Color.parseColor("#1C1C1C"))
            setLineSpacing(0f, 1.5f)
        }
        container.addView(enText)

        val cnText = TextView(context).apply {
            text = cn
            textSize = 12f
            setTextColor(Color.parseColor("#9C9690"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 3.dpToPx()
            }
        }
        container.addView(cnText)

        return container
    }

    private fun createPhraseView(en: String, cn: String): View {
        val tag = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.phrase_background)
            setPadding(11.dpToPx(), 5.dpToPx(), 11.dpToPx(), 5.dpToPx())
            layoutParams = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 4.dpToPx(), 4.dpToPx())
            }
        }

        val enText = TextView(context).apply {
            text = en
            textSize = 13f
            setTextColor(Color.parseColor("#1C1C1C"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        tag.addView(enText)

        val cnText = TextView(context).apply {
            text = cn
            textSize = 11f
            setTextColor(Color.parseColor("#9C9690"))
            setPadding(6.dpToPx(), 0, 0, 0)
        }
        tag.addView(cnText)

        return tag
    }

    private fun Int.dpToPx(): Int = (this * context.resources.displayMetrics.density).toInt()
}
