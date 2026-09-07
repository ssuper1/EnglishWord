package com.example.englishword.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.englishword.R

class SettingsDialog : DialogFragment() {

    private var startPercent = 0
    private var endPercent = 10
    private var totalWords = 0
    private var onApply: ((Int, Int) -> Unit)? = null

    companion object {
        private const val REQUEST_IMPORT_DICT = 1001

        fun newInstance(
            startPercent: Int,
            endPercent: Int,
            totalWords: Int,
            onApply: (Int, Int) -> Unit
        ): SettingsDialog {
            return SettingsDialog().apply {
                this.startPercent = startPercent
                this.endPercent = endPercent
                this.totalWords = totalWords
                this.onApply = onApply
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_EnglishWord)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_settings, container, false)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val margin = (resources.displayMetrics.widthPixels * 0.03).toInt()
            setLayout(resources.displayMetrics.widthPixels - margin * 2, WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
            setDimAmount(0.25f)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val startSeekBar = view.findViewById<SeekBar>(R.id.startSeekBar)
        val endSeekBar = view.findViewById<SeekBar>(R.id.endSeekBar)
        val startPercentText = view.findViewById<TextView>(R.id.startPercentText)
        val endPercentText = view.findViewById<TextView>(R.id.endPercentText)
        val startWordInfo = view.findViewById<TextView>(R.id.startWordInfo)
        val endWordInfo = view.findViewById<TextView>(R.id.endWordInfo)
        val rangeSummary = view.findViewById<TextView>(R.id.rangeSummary)
        val subtitle = view.findViewById<TextView>(R.id.tvSettingsSubtitle)

        subtitle.text = "从 ${totalWords} 个单词中选择显示范围"

        // Init values
        startSeekBar.progress = startPercent
        endSeekBar.progress = endPercent
        updateDisplay(startPercent, endPercent, startPercentText, endPercentText, startWordInfo, endWordInfo, rangeSummary)

        startSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val end = endSeekBar.progress
                val start = if (progress >= end) (end - 1).coerceAtLeast(0) else progress
                if (progress >= end) seekBar?.progress = start
                updateDisplay(start, end, startPercentText, endPercentText, startWordInfo, endWordInfo, rangeSummary)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        endSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val start = startSeekBar.progress
                val end = if (progress <= start) (start + 1).coerceAtMost(100) else progress
                if (progress <= start) seekBar?.progress = end
                updateDisplay(start, end, startPercentText, endPercentText, startWordInfo, endWordInfo, rangeSummary)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Preset buttons
        view.findViewById<TextView>(R.id.btnPreset10).setOnClickListener { applyPreset(0, 10, startSeekBar, endSeekBar, startPercentText, endPercentText, startWordInfo, endWordInfo, rangeSummary) }
        view.findViewById<TextView>(R.id.btnPreset25).setOnClickListener { applyPreset(0, 25, startSeekBar, endSeekBar, startPercentText, endPercentText, startWordInfo, endWordInfo, rangeSummary) }
        view.findViewById<TextView>(R.id.btnPreset50).setOnClickListener { applyPreset(0, 50, startSeekBar, endSeekBar, startPercentText, endPercentText, startWordInfo, endWordInfo, rangeSummary) }
        view.findViewById<TextView>(R.id.btnPresetAll).setOnClickListener { applyPreset(0, 100, startSeekBar, endSeekBar, startPercentText, endPercentText, startWordInfo, endWordInfo, rangeSummary) }

        // Import dictionary button (if exists in layout)
        view.findViewById<Button>(R.id.btnImportDict)?.setOnClickListener {
            openFilePicker()
        }

        // Apply button
        view.findViewById<TextView>(R.id.btnApply).setOnClickListener {
            val start = startSeekBar.progress
            val end = endSeekBar.progress
            onApply?.invoke(start, end)
            dismiss()
        }
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
        if (requestCode == REQUEST_IMPORT_DICT && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val dictId = java.util.UUID.randomUUID().toString()
                    val dialog = ImportProgressDialog.newInstance { wordCount, savedDictId ->
                        android.widget.Toast.makeText(requireContext(), "导入成功: $wordCount 个单词", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    dialog.show(parentFragmentManager, "ImportProgressDialog")

                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            val inputStream = requireContext().contentResolver.openInputStream(uri)
                            if (inputStream != null) {
                                dialog.startImport(inputStream, dictId)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, 100)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun applyPreset(
        start: Int, end: Int,
        startBar: SeekBar, endBar: SeekBar,
        startText: TextView, endText: TextView,
        startInfo: TextView, endInfo: TextView,
        summary: TextView
    ) {
        startBar.progress = start
        endBar.progress = end
        updateDisplay(start, end, startText, endText, startInfo, endInfo, summary)
    }

    private fun updateDisplay(
        start: Int, end: Int,
        startText: TextView, endText: TextView,
        startInfo: TextView, endInfo: TextView,
        summary: TextView
    ) {
        startText.text = "${start}%"
        endText.text = "${end}%"

        val startIdx = (totalWords * start / 100f).toInt().coerceIn(0, totalWords)
        val endIdx = (totalWords * end / 100f).toInt().coerceIn(0, totalWords)
        startInfo.text = "第 ${startIdx + 1} 个单词"
        endInfo.text = "第 ${endIdx} 个单词"

        val count = endIdx - startIdx
        summary.text = "显示 ${count} 个单词（${start}% → ${end}%）"
    }
}
