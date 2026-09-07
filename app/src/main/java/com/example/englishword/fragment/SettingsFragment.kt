package com.example.englishword.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.englishword.R
import com.example.englishword.adapter.ImportedDictAdapter
import com.example.englishword.data.DictManager
import com.example.englishword.dialog.ImportProgressDialog
import com.example.englishword.dialog.ImportSuccessDialog
import java.util.UUID

class SettingsFragment : Fragment() {

    companion object {
        private const val REQUEST_IMPORT_DICT = 1002
    }

    private lateinit var dictRecyclerView: RecyclerView
    private lateinit var emptyDictView: LinearLayout
    private lateinit var dictAdapter: ImportedDictAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dictRecyclerView = view.findViewById(R.id.dictRecyclerView)
        emptyDictView = view.findViewById(R.id.emptyDictView)

        // 设置词典列表
        setupDictList()

        // 导入词典按钮
        view.findViewById<LinearLayout>(R.id.settingImportDict).setOnClickListener {
            openFilePicker()
        }

        // 关于应用
        view.findViewById<LinearLayout>(R.id.settingAbout).setOnClickListener {
            // TODO: 显示关于对话框
        }

        // 加载词典列表
        loadDictList()
    }

    private fun setupDictList() {
        dictAdapter = ImportedDictAdapter(
            onToggle = { dict, enabled ->
                DictManager.toggleDictionary(requireContext(), dict.id, enabled)
            },
            onRename = { dict ->
                showRenameDictDialog(dict)
            },
            onDelete = { dict ->
                showDeleteConfirmDialog(dict)
            }
        )
        dictRecyclerView.adapter = dictAdapter
        dictRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadDictList() {
        val dicts = DictManager.getAllDictionaries(requireContext())
        dictAdapter.updateList(dicts)

        if (dicts.isEmpty()) {
            dictRecyclerView.visibility = View.GONE
            emptyDictView.visibility = View.VISIBLE
        } else {
            dictRecyclerView.visibility = View.VISIBLE
            emptyDictView.visibility = View.GONE
        }
    }

    private fun showRenameDictDialog(dict: DictManager.ImportedDict) {
        val editText = android.widget.EditText(requireContext()).apply {
            setText(dict.name)
            setSelectAllOnFocus(true)
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("重命名词典")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    DictManager.updateDictName(requireContext(), dict.id, newName)
                    loadDictList()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDeleteConfirmDialog(dict: DictManager.ImportedDict) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("删除词典")
            .setMessage("确定要删除「${dict.name}」吗？\n这将删除 ${dict.wordCount} 个单词。")
            .setPositiveButton("删除") { _, _ ->
                DictManager.deleteDictionary(requireContext(), dict.id)
                loadDictList()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "text/plain",
                "text/*",
                "application/json",
                "application/octet-stream",
                "*/*"
            ))
        }
        startActivityForResult(intent, REQUEST_IMPORT_DICT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMPORT_DICT && resultCode == android.app.Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    android.util.Log.d("SettingsFragment", "File URI: $uri")

                    // 先生成 dictId
                    val dictId = UUID.randomUUID().toString()

                    val dialog = ImportProgressDialog.newInstance { wordCount, savedDictId ->
                        // 导入成功后显示保存对话框
                        showImportSuccessDialog(wordCount, savedDictId)
                    }
                    dialog.show(parentFragmentManager, "ImportProgressDialog")

                    // 延迟执行，确保对话框已显示
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            val inputStream = requireContext().contentResolver.openInputStream(uri)
                            if (inputStream != null) {
                                dialog.startImport(inputStream, dictId)
                            } else {
                                android.util.Log.e("SettingsFragment", "InputStream is null")
                                android.widget.Toast.makeText(requireContext(), "无法读取文件", android.widget.Toast.LENGTH_SHORT).show()
                                dialog.dismissAllowingStateLoss()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SettingsFragment", "Error opening file", e)
                            android.widget.Toast.makeText(requireContext(), "文件读取失败: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            dialog.dismissAllowingStateLoss()
                        }
                    }, 100)
                } catch (e: Exception) {
                    android.util.Log.e("SettingsFragment", "Error in onActivityResult", e)
                    android.widget.Toast.makeText(requireContext(), "出错了: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showImportSuccessDialog(wordCount: Int, dictId: String) {
        // 先自动保存词典元数据
        val defaultName = "导入词典 ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}"

        val dict = DictManager.ImportedDict(
            id = dictId,
            name = defaultName,
            wordCount = wordCount,
            importTime = System.currentTimeMillis(),
            isEnabled = true
        )
        DictManager.saveDictionary(requireContext(), dict)

        // 立即刷新列表
        loadDictList()

        // 显示对话框让用户可以修改名称
        val dialog = ImportSuccessDialog.newInstance(wordCount) { newName ->
            // 用户修改了名称，更新
            if (newName != defaultName) {
                DictManager.updateDictName(requireContext(), dictId, newName)
                loadDictList()
            }
        }
        dialog.show(parentFragmentManager, "ImportSuccessDialog")
    }
}
