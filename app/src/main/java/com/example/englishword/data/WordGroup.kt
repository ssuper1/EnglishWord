package com.example.englishword.data

import java.io.Serializable

data class WordGroup(
    val id: String,
    var name: String,
    val words: MutableList<String> = mutableListOf(), // 存储单词的 headWord
    val createTime: Long = System.currentTimeMillis(),
    var updateTime: Long = System.currentTimeMillis()
) : Serializable
