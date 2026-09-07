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
import com.example.englishword.data.GroupManager
import com.example.englishword.data.WordGroup

class AddToGroupDialog : DialogFragment() {

    private var word: String? = null
    private var onGroupSelected: ((WordGroup) -> Unit)? = null

    companion object {
        private const val ARG_WORD = "word"

        fun newInstance(word: String, onSelected: (WordGroup) -> Unit): AddToGroupDialog {
            return AddToGroupDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_WORD, word)
                }
                this.onGroupSelected = onSelected
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        word = arguments?.getString(ARG_WORD)

        val view = layoutInflater.inflate(R.layout.dialog_add_to_group, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.groupListRecyclerView)
        val tvNoGroups = view.findViewById<TextView>(R.id.tvNoGroups)
        val btnCreateNew = view.findViewById<TextView>(R.id.btnCreateNew)

        val groups = GroupManager.getAllGroups(requireContext())
        val currentWord = word ?: ""

        if (groups.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvNoGroups.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            tvNoGroups.visibility = View.GONE

            val adapter = GroupSelectorAdapter(
                groups = groups,
                selectedWord = currentWord,
                onSelect = { group ->
                    onGroupSelected?.invoke(group)
                    dismiss()
                }
            )
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
        }

        btnCreateNew.setOnClickListener {
            dismiss()
            // 创建新分组并添加当前单词
            val createDialog = CreateGroupDialog.newInstance { name ->
                val newGroup = WordGroup(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    words = mutableListOf(currentWord)
                )
                GroupManager.saveGroup(requireContext(), newGroup)
                android.widget.Toast.makeText(
                    requireContext(),
                    "已创建分组「$name」并添加单词",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            createDialog.show(parentFragmentManager, "CreateGroupDialog")
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }

    private inner class GroupSelectorAdapter(
        private val groups: List<WordGroup>,
        private val selectedWord: String,
        private val onSelect: (WordGroup) -> Unit
    ) : RecyclerView.Adapter<GroupSelectorAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_group_selector, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(groups[position])
        }

        override fun getItemCount() = groups.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvGroupName: TextView = view.findViewById(R.id.tvGroupName)
            private val tvGroupInfo: TextView = view.findViewById(R.id.tvGroupInfo)
            private val tvAdded: TextView = view.findViewById(R.id.tvAdded)

            fun bind(group: WordGroup) {
                tvGroupName.text = group.name
                tvGroupInfo.text = "${group.words.size} 词"

                val isAdded = group.words.contains(selectedWord)
                tvAdded.visibility = if (isAdded) View.VISIBLE else View.GONE

                itemView.setOnClickListener {
                    onSelect(group)
                }
            }
        }
    }
}
