package com.example.englishword.data

import android.content.Context
import com.example.englishword.model.*
import org.json.JSONArray
import org.json.JSONObject

object WordJsonParser {

    fun parseWords(context: Context): List<Word> {
        val words = mutableListOf<Word>()
        try {
            val inputStream = context.assets.open("words.json")
            val bytes = inputStream.readBytes()
            inputStream.close()

            val text = String(bytes, Charsets.UTF_8)
            // Wrap in array brackets, replace JSONL separators with commas
            val jsonText = "[${text.trim()}]"
                .replace("}\n{", "},{")
                .replace("}\r\n{", "},{")

            val jsonArray = JSONArray(jsonText)
            for (i in 0 until jsonArray.length()) {
                try {
                    val json = jsonArray.getJSONObject(i)
                    val word = parseWordEntry(json)
                    if (word != null) {
                        words.add(word)
                    }
                } catch (e: Exception) {
                    // Skip malformed entries
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return words
    }

    private fun parseWordEntry(json: JSONObject): Word? {
        val headWord = json.optString("headWord", "")
        val bookId = json.optString("bookId", "")
        val content = json.optJSONObject("content")
            ?.optJSONObject("word")
            ?.optJSONObject("content") ?: return null

        val usphone = content.optString("usphone", "")
        val ukphone = content.optString("ukphone", "")

        // Parse trans - array of {tranCn, pos, ...}
        val transArr = content.optJSONArray("trans")
        val transBuilder = StringBuilder()
        val posBuilder = StringBuilder()
        if (transArr != null && transArr.length() > 0) {
            for (i in 0 until transArr.length()) {
                val t = transArr.getJSONObject(i)
                val cn = t.optString("tranCn", "")
                val pos = t.optString("pos", "")
                if (cn.isNotBlank()) {
                    if (transBuilder.isNotEmpty()) transBuilder.append("；")
                    transBuilder.append(cn)
                }
                if (pos.isNotBlank()) {
                    if (posBuilder.isNotEmpty()) posBuilder.append(" · ")
                    posBuilder.append(pos)
                }
            }
        }

        // Parse sentences
        val sentences = mutableListOf<Sentence>()
        val sentObj = content.optJSONObject("sentence")
        if (sentObj != null) {
            val sentArr = sentObj.optJSONArray("sentences")
            if (sentArr != null) {
                for (i in 0 until sentArr.length()) {
                    val s = sentArr.getJSONObject(i)
                    val en = s.optString("sContent", "")
                    val cn = s.optString("sCn", "")
                    if (en.isNotBlank()) {
                        sentences.add(Sentence(en, cn))
                    }
                }
            }
        }

        // Parse synonyms
        var syno: SynonymGroup? = null
        val synoObj = content.optJSONObject("syno")
        if (synoObj != null) {
            val synoArr = synoObj.optJSONArray("synos")
            if (synoArr != null && synoArr.length() > 0) {
                val firstSyno = synoArr.getJSONObject(0)
                val pos = firstSyno.optString("pos", "")
                val hwds = firstSyno.optJSONArray("hwds")
                val words = mutableListOf<String>()
                if (hwds != null) {
                    for (i in 0 until hwds.length()) {
                        val h = hwds.getJSONObject(i)
                        val w = h.optString("w", "")
                        if (w.isNotBlank()) words.add(w)
                    }
                }
                if (words.isNotEmpty()) {
                    syno = SynonymGroup(pos, words)
                }
            }
        }

        // Parse related words
        var relWordMap: Map<String, List<RelWord>>? = null
        val relObj = content.optJSONObject("relWord")
        if (relObj != null) {
            val relArr = relObj.optJSONArray("rels")
            if (relArr != null && relArr.length() > 0) {
                val map = LinkedHashMap<String, List<RelWord>>()
                for (i in 0 until relArr.length()) {
                    val group = relArr.getJSONObject(i)
                    val pos = group.optString("pos", "")
                    val wordsArr = group.optJSONArray("words")
                    if (wordsArr != null && wordsArr.length() > 0) {
                        val relWords = mutableListOf<RelWord>()
                        for (j in 0 until wordsArr.length()) {
                            val w = wordsArr.getJSONObject(j)
                            val hwd = w.optString("hwd", "")
                            val tran = w.optString("tran", "").trim()
                            if (hwd.isNotBlank()) {
                                relWords.add(RelWord(hwd, tran))
                            }
                        }
                        if (relWords.isNotEmpty()) {
                            map[pos] = relWords
                        }
                    }
                }
                if (map.isNotEmpty()) {
                    relWordMap = map
                }
            }
        }

        // Parse phrases
        val phrases = mutableListOf<Phrase>()
        val phraseObj = content.optJSONObject("phrase")
        if (phraseObj != null) {
            val phraseArr = phraseObj.optJSONArray("phrases")
            if (phraseArr != null) {
                for (i in 0 until phraseArr.length()) {
                    val p = phraseArr.getJSONObject(i)
                    val en = p.optString("pContent", "")
                    val cn = p.optString("pCn", "")
                    if (en.isNotBlank()) {
                        phrases.add(Phrase(en, cn))
                    }
                }
            }
        }

        // Parse memory method
        var remMethod: String? = null
        val memObj = content.optJSONObject("remMethod")
        if (memObj != null) {
            remMethod = memObj.optString("val", "")
        }

        return Word(
            headWord = headWord,
            usphone = usphone,
            ukphone = ukphone,
            pos = posBuilder.toString(),
            trans = transBuilder.toString(),
            sentences = sentences,
            syno = syno,
            relWord = relWordMap,
            phrases = phrases,
            remMethod = remMethod,
            bookId = bookId,
            state = WordState.NEW
        )
    }
}
