package com.example.englishword.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.englishword.R
import com.example.englishword.data.DictManager
import java.text.SimpleDateFormat
import java.util.*

class ImportedDictAdapter(
    private val onToggle: (DictManager.ImportedDict, Boolean) -> Unit,
    private val onRename: (DictManager.ImportedDict) -> Unit,
    private val onDelete: (DictManager.ImportedDict) -> Unit
) : RecyclerView.Adapter<ImportedDictAdapter.ViewHolder>() {

    private val dictList = mutableListOf<DictManager.ImportedDict>()

    fun updateList(newList: List<DictManager.ImportedDict>) {
        dictList.clear()
        dictList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_imported_dict, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dictList[position])
    }

    override fun getItemCount() = dictList.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDictName: TextView = view.findViewById(R.id.tvDictName)
        private val tvDictInfo: TextView = view.findViewById(R.id.tvDictInfo)
        private val switchEnabled: Switch = view.findViewById(R.id.switchEnabled)
        private val btnRename: TextView = view.findViewById(R.id.btnRename)
        private val btnDelete: TextView = view.findViewById(R.id.btnDelete)

        fun bind(dict: DictManager.ImportedDict) {
            tvDictName.text = dict.name

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            tvDictInfo.text = "${dict.wordCount} 词 · ${dateFormat.format(Date(dict.importTime))}"

            switchEnabled.isChecked = dict.isEnabled
            switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                onToggle(dict, isChecked)
            }

            btnRename.setOnClickListener {
                onRename(dict)
            }

            btnDelete.setOnClickListener {
                onDelete(dict)
            }
        }
    }
}
