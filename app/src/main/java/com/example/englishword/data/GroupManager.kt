package com.example.englishword.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object GroupManager {
    private const val PREFS_NAME = "wordmate_groups"
    private const val KEY_GROUPS = "groups"

    fun getAllGroups(context: Context): List<WordGroup> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_GROUPS, null) ?: return emptyList()

        return try {
            val type = object : TypeToken<List<WordGroup>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveGroup(context: Context, group: WordGroup) {
        val groups = getAllGroups(context).toMutableList()
        val existingIndex = groups.indexOfFirst { it.id == group.id }

        if (existingIndex >= 0) {
            groups[existingIndex] = group
        } else {
            groups.add(group)
        }

        saveAllGroups(context, groups)
    }

    fun deleteGroup(context: Context, groupId: String) {
        val groups = getAllGroups(context).filter { it.id != groupId }
        saveAllGroups(context, groups)
    }

    fun addWordToGroup(context: Context, groupId: String, word: String) {
        val groups = getAllGroups(context).toMutableList()
        val group = groups.find { it.id == groupId } ?: return

        if (!group.words.contains(word)) {
            group.words.add(word)
            group.updateTime = System.currentTimeMillis()
            saveAllGroups(context, groups)
        }
    }

    fun removeWordFromGroup(context: Context, groupId: String, word: String) {
        val groups = getAllGroups(context).toMutableList()
        val group = groups.find { it.id == groupId } ?: return

        group.words.remove(word)
        group.updateTime = System.currentTimeMillis()
        saveAllGroups(context, groups)
    }

    fun getGroupsContainingWord(context: Context, word: String): List<WordGroup> {
        return getAllGroups(context).filter { it.words.contains(word) }
    }

    private fun saveAllGroups(context: Context, groups: List<WordGroup>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(groups)
        prefs.edit().putString(KEY_GROUPS, json).apply()
    }
}
