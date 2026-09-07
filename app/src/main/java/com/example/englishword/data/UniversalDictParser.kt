package com.example.englishword.data

import org.json.JSONObject

object UniversalDictParser {

    fun detectAndParse(line: String): ParseResult? {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        return when {
            // JSON格式：{"wordRank":1,"headWord":"hale"...
            trimmed.startsWith("{") -> parseJson(trimmed)

            // 带音标格式：adhesive [əd'hi:siv] a.带粘性的
            trimmed.contains(Regex("\\[.*?\\]")) -> parseWithPhonetic(trimmed)

            // 简单Tab格式：zygosporic	adj.[植]接合孢子的
            trimmed.contains("\t") -> parseSimpleTab(trimmed)

            // 空格分隔：word translation
            trimmed.contains(Regex("^[a-zA-Z-]+\\s+[^a-zA-Z]")) -> parseSpaceSeparated(trimmed)

            else -> null
        }
    }

    private fun parseJson(line: String): ParseResult? {
        try {
            val json = JSONObject(line)
            val headWord = json.optString("headWord", "")
            if (headWord.isEmpty()) {
                android.util.Log.d("DictParser", "JSON missing headWord")
                return null
            }

            val content = json.optJSONObject("content")
                ?.optJSONObject("word")
                ?.optJSONObject("content")

            if (content == null) {
                android.util.Log.d("DictParser", "JSON missing content structure")
                return null
            }

            val trans = content.optJSONArray("trans")?.let { arr ->
                (0 until arr.length()).mapNotNull { i ->
                    arr.optJSONObject(i)?.optString("tranCn", "")
                }.filter { it.isNotEmpty() }.joinToString("；")
            } ?: ""

            val ukphone = content.optString("ukphone", "") ?: ""
            val usphone = content.optString("usphone", "") ?: ""

            android.util.Log.d("DictParser", "Parsed JSON: $headWord - $trans")

            return ParseResult(
                word = headWord,
                phonetic = ukphone.ifEmpty { usphone },
                translation = trans
            )
        } catch (e: Exception) {
            android.util.Log.e("DictParser", "JSON parse error: ${e.message}")
            return null
        }
    }

    private fun parseWithPhonetic(line: String): ParseResult? {
        // adhesive [əd'hi:siv] a.带粘性的，胶粘的；n.胶合剂
        val pattern = Regex("^([a-zA-Z-]+)\\s*\\[([^\\]]+)\\]\\s*(.+)$")
        val match = pattern.find(line) ?: return null

        return ParseResult(
            word = match.groupValues[1].trim(),
            phonetic = match.groupValues[2].trim(),
            translation = match.groupValues[3].trim()
        )
    }

    private fun parseSimpleTab(line: String): ParseResult? {
        // zygosporic	adj.[植]接合孢子的
        val parts = line.split("\t", limit = 2)
        if (parts.size != 2) return null

        return ParseResult(
            word = parts[0].trim(),
            phonetic = "",
            translation = parts[1].trim()
        )
    }

    private fun parseSpaceSeparated(line: String): ParseResult? {
        val parts = line.split(Regex("\\s+"), limit = 2)
        if (parts.size != 2) return null

        return ParseResult(
            word = parts[0].trim(),
            phonetic = "",
            translation = parts[1].trim()
        )
    }

    data class ParseResult(
        val word: String,
        val phonetic: String,
        val translation: String
    )
}
