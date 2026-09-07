package com.example.englishword.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.englishword.R
import java.text.SimpleDateFormat
import java.util.*

class ImportSuccessDialog : DialogFragment() {

    private var wordCount = 0
    private var onSave: ((String) -> Unit)? = null

    companion object {
        private const val ARG_WORD_COUNT = "word_count"

        fun newInstance(wordCount: Int, onSave: (String) -> Unit): ImportSuccessDialog {
            return ImportSuccessDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_WORD_COUNT, wordCount)
                }
                this.onSave = onSave
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        wordCount = arguments?.getInt(ARG_WORD_COUNT) ?: 0

        val view = layoutInflater.inflate(R.layout.dialog_import_success, null)

        val tvImportStats = view.findViewById<TextView>(R.id.tvImportStats)
        val tvImportTime = view.findViewById<TextView>(R.id.tvImportTime)
        val etDictName = view.findViewById<EditText>(R.id.etDictName)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancel)
        val btnSave = view.findViewById<TextView>(R.id.btnSave)

        tvImportStats.text = "成功导入 $wordCount 个单词"

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val currentTime = dateFormat.format(Date())
        tvImportTime.text = "导入时间: $currentTime"

        // 生成默认名称
        val defaultName = "导入词典 $currentTime"
        etDictName.hint = defaultName

        // 修改按钮文字和提示
        btnCancel.text = "稍后修改"
        btnSave.text = "重命名"

        btnCancel.setOnClickListener {
            // 稍后修改，直接关闭对话框（词典已自动保存）
            dismiss()
        }

        btnSave.setOnClickListener {
            val name = etDictName.text.toString().trim().ifEmpty { defaultName }
            onSave?.invoke(name)
            dismiss()
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }
}
