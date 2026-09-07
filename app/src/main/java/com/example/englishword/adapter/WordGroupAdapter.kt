package com.example.englishword.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.englishword.R
import com.example.englishword.data.WordGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WordGroupAdapter(
    private var groups: List<WordGroup>,
    private val onClick: (WordGroup) -> Unit,
    private val onRename: (WordGroup) -> Unit,
    private val onDelete: (WordGroup) -> Unit
) : RecyclerView.Adapter<WordGroupAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_word_group, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    override fun getItemCount() = groups.size

    fun updateGroups(newGroups: List<WordGroup>) {
        groups = newGroups
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvGroupName: TextView = view.findViewById(R.id.tvGroupName)
        private val tvGroupInfo: TextView = view.findViewById(R.id.tvGroupInfo)
        private val btnRename: TextView = view.findViewById(R.id.btnRename)
        private val btnDelete: TextView = view.findViewById(R.id.btnDelete)

        fun bind(group: WordGroup) {
            tvGroupName.text = group.name

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = dateFormat.format(Date(group.updateTime))
            tvGroupInfo.text = "${group.words.size} 词 · $dateStr"

            itemView.setOnClickListener {
                onClick(group)
            }

            btnRename.setOnClickListener {
                onRename(group)
            }

            btnDelete.setOnClickListener {
                onDelete(group)
            }
        }
    }
}
