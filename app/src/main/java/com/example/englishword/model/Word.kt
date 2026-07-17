package com.example.englishword.model

import java.io.Serializable

enum class WordState(val value: Int, val label: String) {
    NEW(0, "未标记"),
    KNOWN(1, "已掌握"),
    REVIEW(2, "需复习");

    companion object {
        fun fromValue(value: Int): WordState = entries.firstOrNull { it.value == value } ?: NEW
        fun next(state: WordState): WordState = when (state) {
            NEW -> KNOWN
            KNOWN -> REVIEW
            REVIEW -> NEW
        }
    }
}

data class RelWord(
    val hwd: String,
    val tran: String
) : Serializable

data class SynonymGroup(
    val pos: String,
    val words: List<String>
) : Serializable

data class Sentence(
    val en: String,
    val cn: String
) : Serializable

data class Phrase(
    val en: String,
    val cn: String
) : Serializable

data class Word(
    val headWord: String,
    val usphone: String = "",
    val ukphone: String = "",
    val pos: String = "",
    val trans: String = "",
    val sentences: List<Sentence> = emptyList(),
    val syno: SynonymGroup? = null,
    val relWord: Map<String, List<RelWord>>? = null,
    val phrases: List<Phrase> = emptyList(),
    val remMethod: String? = null,
    val bookId: String = "",
    var state: WordState = WordState.NEW
) : Serializable
