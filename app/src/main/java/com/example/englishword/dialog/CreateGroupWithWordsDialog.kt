package com.example.englishword.dialog

import android.app.Dialog
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.englishword.R

class CreateGroupWithWordsDialog : DialogFragment() {

    private var onGroupCreated: ((String, List<String>) -> Unit)? = null
    private var initialName: String? = null

    companion object {
        private const val ARG_INITIAL_NAME = "initial_name"

        fun newInstance(initialName: String? = null, onCreated: (String, List<String>) -> Unit): CreateGroupWithWordsDialog {
            return CreateGroupWithWordsDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_INITIAL_NAME, initialName)
                }
                this.onGroupCreated = onCreated
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        initialName = arguments?.getString(ARG_INITIAL_NAME)

        val view = layoutInflater.inflate(R.layout.dialog_create_group_with_words, null)
        val etGroupName = view.findViewById<EditText>(R.id.etGroupName)
        val etWords = view.findViewById<EditText>(R.id.etWords)
        val tvPasteFromClipboard = view.findViewById<TextView>(R.id.tvPasteFromClipboard)
        val tvWordCount = view.findViewById<TextView>(R.id.tvWordCount)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancel)
        val btnCreate = view.findViewById<TextView>(R.id.btnCreate)

        initialName?.let {
            etGroupName.setText(it)
            etGroupName.setSelection(it.length)
        }

        // 监听单词输入变化，更新计数
        etWords.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val words = parseWords(s?.toString() ?: "")
                tvWordCount.text = "识别到 ${words.size} 个单词"
            }
        })

        // 从剪贴板粘贴
        tvPasteFromClipboard.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).text?.toString() ?: ""
                etWords.setText(text)
                android.widget.Toast.makeText(requireContext(), "已粘贴剪贴板内容", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(requireContext(), "剪贴板为空", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnCreate.setOnClickListener {
            val name = etGroupName.text.toString().trim()
            val wordsText = etWords.text.toString()
            val words = parseWords(wordsText)

            if (name.isEmpty()) {
                etGroupName.error = "请输入分组名称"
                return@setOnClickListener
            }

            onGroupCreated?.invoke(name, words)
            dismiss()
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }

    private fun parseWords(text: String): List<String> {
        if (text.isBlank()) return emptyList()

        val words = mutableSetOf<String>()

        // 按行分割
        val lines = text.split("\n")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            // 尝试识别不同格式
            // 1. JSON 格式: {"headWord":"word","trans":"翻译"}
            if (trimmed.startsWith("{") && trimmed.contains("headWord")) {
                try {
                    val jsonMatch = Regex(""""headWord"\s*:\s*"([^"]+)"""").find(trimmed)
                    jsonMatch?.groupValues?.get(1)?.let { words.add(it.lowercase()) }
                    continue
                } catch (e: Exception) {
                    // 继续尝试其他格式
                }
            }

            // 2. 带音标格式: word/phrase [phonetic] translation
            val phoneticMatch = Regex("""^([a-zA-Z\s-]+)\s*\[.*?\]\s+(.+)$""").find(trimmed)
            if (phoneticMatch != null) {
                val word = phoneticMatch.groupValues[1].trim()
                if (word.isNotEmpty()) {
                    words.add(word.lowercase())
                }
                continue
            }

            // 3. 制表符分隔: word\ttranslation
            if (trimmed.contains("\t")) {
                val parts = trimmed.split("\t")
                val word = parts[0].trim()
                if (word.matches(Regex("^[a-zA-Z\\s-]+$"))) {
                    words.add(word.lowercase())
                    continue
                }
            }

            // 4. 空格分隔带翻译: word/phrase translation
            // 识别模式：英文(可能包含空格和连字符) + 空格 + 非英文内容
            val spaceMatch = Regex("""^([a-zA-Z\s-]+?)\s+([一-龥].*|[（(].*|[\d一二三四五六七八九十]+.*|.*[一-龥].*)$""").find(trimmed)
            if (spaceMatch != null) {
                val word = spaceMatch.groupValues[1].trim()
                // 移除单词末尾可能多余的空格
                if (word.isNotEmpty() && word.matches(Regex("^[a-zA-Z\\s-]+$"))) {
                    words.add(word.lowercase())
                    continue
                }
            }

            // 5. 纯单词或短语（整行只有英文单词、空格和连字符）
            if (trimmed.matches(Regex("^[a-zA-Z\\s-]+$"))) {
                words.add(trimmed.lowercase())
            }
        }

        return words.toList()
    }
}
