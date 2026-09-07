package com.example.englishword.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.englishword.R
import com.example.englishword.adapter.DictSelectorAdapter
import com.example.englishword.data.DictManager

class DictSelectorDialog : DialogFragment() {

    private var currentDictId: String? = null
    private var onDictSelected: ((DictManager.ImportedDict?) -> Unit)? = null

    companion object {
        private const val ARG_CURRENT_DICT_ID = "current_dict_id"

        fun newInstance(currentDictId: String?, onSelected: (DictManager.ImportedDict?) -> Unit): DictSelectorDialog {
            return DictSelectorDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_CURRENT_DICT_ID, currentDictId)
                }
                this.onDictSelected = onSelected
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        currentDictId = arguments?.getString(ARG_CURRENT_DICT_ID)

        val view = layoutInflater.inflate(R.layout.dialog_select_dict, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.dictListRecyclerView)
        val tvNoDicts = view.findViewById<TextView>(R.id.tvNoDicts)

        // 获取所有已启用的词典，加上内置词库选项
        val allDicts = mutableListOf<DictManager.ImportedDict>()

        // 添加内置词库选项
        allDicts.add(DictManager.ImportedDict(
            id = "builtin",
            name = "IELTS 核心词汇（内置）",
            wordCount = 0, // 会在实际加载时获取
            importTime = 0,
            isEnabled = true,
            filePath = "builtin"
        ))

        // 添加导入的词典（只显示已启用的）
        allDicts.addAll(DictManager.getAllDictionaries(requireContext()).filter { it.isEnabled })

        if (allDicts.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvNoDicts.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvNoDicts.visibility = View.GONE

            val adapter = DictSelectorAdapter(
                dicts = allDicts,
                selectedDictId = currentDictId,
                onSelect = { dict ->
                    onDictSelected?.invoke(dict)
                    dismiss()
                }
            )
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }
}
