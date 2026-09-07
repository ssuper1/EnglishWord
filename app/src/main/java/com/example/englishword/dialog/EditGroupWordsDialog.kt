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

class EditGroupWordsDialog : DialogFragment() {

    private var groupId: String? = null
    private var onWordsChanged: (() -> Unit)? = null
    private lateinit var adapter: EditWordItemAdapter

    companion object {
        private const val ARG_GROUP_ID = "group_id"

        fun newInstance(groupId: String, onChanged: () -> Unit): EditGroupWordsDialog {
            return EditGroupWordsDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_GROUP_ID, groupId)
                }
                this.onWordsChanged = onChanged
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        groupId = arguments?.getString(ARG_GROUP_ID)

        val view = layoutInflater.inflate(R.layout.dialog_edit_group_words, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.wordListRecyclerView)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)
        val btnAddWords = view.findViewById<TextView>(R.id.btnAddWords)
        val btnClose = view.findViewById<TextView>(R.id.btnClose)

        val currentGroupId = groupId ?: return super.onCreateDialog(savedInstanceState)
        val group = GroupManager.getAllGroups(requireContext()).find { it.id == currentGroupId }
            ?: return super.onCreateDialog(savedInstanceState)

        // Setup adapter
        adapter = EditWordItemAdapter(
            words = group.words.toMutableList(),
            onRemove = { word ->
                GroupManager.removeWordFromGroup(requireContext(), currentGroupId, word)
                loadWords()
                onWordsChanged?.invoke()
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        loadWords()

        btnAddWords.setOnClickListener {
            dismiss()
            val addDialog = CreateGroupWithWordsDialog.newInstance(null) { _, words ->
                for (word in words) {
                    GroupManager.addWordToGroup(requireContext(), currentGroupId, word)
                }
                onWordsChanged?.invoke()
                android.widget.Toast.makeText(
                    requireContext(),
                    "已添加 ${words.size} 个单词",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            addDialog.show(parentFragmentManager, "AddWordsDialog")
        }

        btnClose.setOnClickListener {
            dismiss()
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }

    private fun loadWords() {
        val currentGroupId = groupId ?: return
        val group = GroupManager.getAllGroups(requireContext()).find { it.id == currentGroupId } ?: return

        adapter.updateWords(group.words)
    }

    private inner class EditWordItemAdapter(
        private var words: MutableList<String>,
        private val onRemove: (String) -> Unit
    ) : RecyclerView.Adapter<EditWordItemAdapter.ViewHolder>() {

        fun updateWords(newWords: List<String>) {
            words = newWords.toMutableList()
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_edit_word, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(words[position])
        }

        override fun getItemCount() = words.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvWord: TextView = view.findViewById(R.id.tvWord)
            private val btnRemove: TextView = view.findViewById(R.id.btnRemove)

            fun bind(word: String) {
                tvWord.text = word

                btnRemove.setOnClickListener {
                    onRemove(word)
                }
            }
        }
    }
}
