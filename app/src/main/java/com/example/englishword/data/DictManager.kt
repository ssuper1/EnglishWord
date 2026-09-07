package com.example.englishword.data

import android.content.Context
import android.content.SharedPreferences
import com.example.englishword.model.DictWord
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object DictManager {
    private const val PREFS_NAME = "imported_dicts"
    private const val KEY_DICT_LIST = "dict_list"

    data class ImportedDict(
        val id: String,
        var name: String,
        val wordCount: Int,
        val importTime: Long,
        var isEnabled: Boolean = true,
        val filePath: String = ""
    )

    fun saveDictionary(context: Context, dict: ImportedDict) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dicts = getAllDictionaries(context).toMutableList()

        // 检查是否已存在，更新或添加
        val index = dicts.indexOfFirst { it.id == dict.id }
        if (index >= 0) {
            dicts[index] = dict
        } else {
            dicts.add(dict)
        }

        prefs.edit()
            .putString(KEY_DICT_LIST, Gson().toJson(dicts))
            .apply()
    }

    fun getAllDictionaries(context: Context): List<ImportedDict> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_DICT_LIST, null) ?: return emptyList()

        return try {
            val type = object : TypeToken<List<ImportedDict>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteDictionary(context: Context, dictId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dicts = getAllDictionaries(context).toMutableList()
        dicts.removeAll { it.id == dictId }

        prefs.edit()
            .putString(KEY_DICT_LIST, Gson().toJson(dicts))
            .apply()
    }

    fun toggleDictionary(context: Context, dictId: String, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dicts = getAllDictionaries(context).toMutableList()

        dicts.find { it.id == dictId }?.let {
            it.isEnabled = enabled
            prefs.edit()
                .putString(KEY_DICT_LIST, Gson().toJson(dicts))
                .apply()
        }
    }

    fun updateDictName(context: Context, dictId: String, newName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dicts = getAllDictionaries(context).toMutableList()

        dicts.find { it.id == dictId }?.let {
            it.name = newName
            prefs.edit()
                .putString(KEY_DICT_LIST, Gson().toJson(dicts))
                .apply()
        }
    }
}
