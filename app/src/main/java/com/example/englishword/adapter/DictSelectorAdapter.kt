package com.example.englishword.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.englishword.R
import com.example.englishword.data.DictManager

class DictSelectorAdapter(
    private val dicts: List<DictManager.ImportedDict>,
    private val selectedDictId: String?,
    private val onSelect: (DictManager.ImportedDict) -> Unit
) : RecyclerView.Adapter<DictSelectorAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dict_selector, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dicts[position])
    }

    override fun getItemCount() = dicts.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDictName: TextView = view.findViewById(R.id.tvDictName)
        private val tvDictInfo: TextView = view.findViewById(R.id.tvDictInfo)
        private val tvSelected: TextView = view.findViewById(R.id.tvSelected)

        fun bind(dict: DictManager.ImportedDict) {
            tvDictName.text = dict.name

            if (dict.id == "builtin") {
                tvDictInfo.text = "内置词库"
            } else {
                tvDictInfo.text = "${dict.wordCount} 词"
            }

            tvSelected.visibility = if (dict.id == selectedDictId) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                onSelect(dict)
            }
        }
    }
}
