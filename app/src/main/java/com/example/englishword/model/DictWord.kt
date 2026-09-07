package com.example.englishword.model

data class DictWord(
    val word: String,
    val phonetic: String,
    val translation: String,
    val source: DictSource
)

enum class DictSource {
    LOCAL_JSON,      // 本地JSON词库
    JSON_DICT,       // 导入的JSON词典
    PHONETIC_DICT,   // 带音标的词典
    SIMPLE_DICT,     // 简单格式词典
    ONLINE_API       // 在线API
}
