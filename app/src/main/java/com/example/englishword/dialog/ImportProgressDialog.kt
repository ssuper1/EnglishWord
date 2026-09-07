package com.example.englishword.dialog

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.englishword.R
import com.example.englishword.data.DictManager
import com.example.englishword.data.UniversalDictParser
import java.io.InputStream
import java.util.UUID
import kotlin.concurrent.thread

class ImportProgressDialog : DialogFragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var tvProgress: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var onImportComplete: ((Int, String) -> Unit)? = null

    companion object {
        fun newInstance(onComplete: (Int, String) -> Unit): ImportProgressDialog {
            return ImportProgressDialog().apply {
                this.onImportComplete = onComplete
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_import_progress, null)

        progressBar = view.findViewById(R.id.progressBar)
        tvStatus = view.findViewById(R.id.tvStatus)
        tvProgress = view.findViewById(R.id.tvProgress)

        isCancelable = false

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }

    fun startImport(inputStream: InputStream, dictId: String) {
        android.util.Log.d("ImportProgress", "startImport called with dictId: $dictId")

        // 获取 context 引用，避免在后台线程中调用 requireContext()
        val context = requireContext()

        handler.post {
            tvStatus.text = "开始读取文件..."
            tvProgress.text = "准备中..."
        }

        thread {
            android.util.Log.d("ImportProgress", "Background thread started")
            var successCount = 0
            var failCount = 0
            var lineCount = 0
            val validLines = mutableListOf<String>()
            var lastWord = ""

            try {
                // 先读取所有内容
                val content = inputStream.use { it.bufferedReader().readText() }
                android.util.Log.d("ImportProgress", "File read, length: ${content.length}")

                handler.post {
                    tvStatus.text = "文件读取成功，开始解析..."
                }

                val lines = content.split("\n")
                android.util.Log.d("ImportProgress", "Total lines: ${lines.size}")

                lines.forEachIndexed { index, line ->
                    lineCount++
                    val trimmed = line.trim()

                    // 跳过空行
                    if (trimmed.isEmpty()) {
                        return@forEachIndexed
                    }

                    val result = UniversalDictParser.detectAndParse(trimmed)
                    if (result != null) {
                        successCount++
                        lastWord = result.word
                        validLines.add(trimmed)
                        if (successCount <= 5) {
                            android.util.Log.d("ImportProgress", "Success #$successCount: $lastWord")
                        }
                    } else {
                        failCount++
                        if (failCount <= 5) {
                            android.util.Log.d("ImportProgress", "Failed line $lineCount: ${trimmed.take(80)}")
                        }
                    }

                    // 每100行更新一次UI
                    if (lineCount % 100 == 0) {
                        val currentWord = lastWord
                        val currentSuccess = successCount
                        val currentTotal = lineCount
                        handler.post {
                            tvProgress.text = "$currentSuccess / $currentTotal"
                            tvStatus.text = "解析中: $currentWord"
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("ImportProgress", "Error during import: ${e.message}", e)
                handler.post {
                    tvStatus.text = "导入失败: ${e.message}"
                    tvProgress.text = "错误"
                }
                handler.postDelayed({
                    try {
                        if (isAdded && !isDetached) {
                            dismissAllowingStateLoss()
                        }
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }, 3000)
                return@thread
            }

            // 导入完成
            val finalSuccess = successCount
            val finalFail = failCount
            val finalTotal = lineCount
            android.util.Log.d("ImportProgress", "Import completed: Success=$finalSuccess, Fail=$finalFail, Total=$finalTotal")
            android.util.Log.d("ImportProgress", "Valid lines to save: ${validLines.size}")

            // 保存有效的词汇行到文件
            try {
                val dictDir = java.io.File(context.filesDir, "imported_dicts")
                android.util.Log.d("ImportProgress", "Dict directory: ${dictDir.absolutePath}")

                if (!dictDir.exists()) {
                    val created = dictDir.mkdirs()
                    android.util.Log.d("ImportProgress", "Created dict directory: $created")
                }

                val dictFile = java.io.File(dictDir, "$dictId.txt")
                android.util.Log.d("ImportProgress", "Saving to file: ${dictFile.absolutePath}")

                dictFile.writeText(validLines.joinToString("\n"))

                android.util.Log.d("ImportProgress", "File saved successfully. File exists: ${dictFile.exists()}, size: ${dictFile.length()} bytes")
            } catch (e: Exception) {
                android.util.Log.e("ImportProgress", "Error saving dict file: ${e.message}", e)
                e.printStackTrace()
            }

            handler.post {
                progressBar.isIndeterminate = false
                progressBar.max = 100
                progressBar.progress = 100
                tvStatus.text = "导入完成！"
                tvProgress.text = "成功: $finalSuccess / $finalTotal (失败: $finalFail)"

                // 延迟关闭并显示成功对话框
                handler.postDelayed({
                    try {
                        if (isAdded && !isDetached) {
                            dismissAllowingStateLoss()
                            onImportComplete?.invoke(finalSuccess, dictId)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, 1500)
            }
        }
    }
}
