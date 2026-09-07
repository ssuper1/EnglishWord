package com.example.englishword.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.englishword.R
import com.example.englishword.adapter.WordGroupAdapter
import com.example.englishword.data.GroupManager
import com.example.englishword.data.WordGroup
import com.example.englishword.dialog.CreateGroupDialog
import com.example.englishword.dialog.CreateGroupWithWordsDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.UUID

class CustomGroupsFragment : Fragment() {

    private lateinit var groupsRecyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var groupMeta: TextView
    private lateinit var fabAddGroup: FloatingActionButton
    private lateinit var adapter: WordGroupAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_custom_groups, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupsRecyclerView = view.findViewById(R.id.groupsRecyclerView)
        emptyState = view.findViewById(R.id.emptyState)
        groupMeta = view.findViewById(R.id.groupMeta)
        fabAddGroup = view.findViewById(R.id.fabAddGroup)

        // Setup RecyclerView
        adapter = WordGroupAdapter(
            groups = emptyList(),
            onClick = { group -> openGroup(group) },
            onRename = { group -> showRenameDialog(group) },
            onDelete = { group -> showDeleteConfirmDialog(group) }
        )
        groupsRecyclerView.adapter = adapter
        groupsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        fabAddGroup.setOnClickListener {
            showCreateGroupDialog()
        }

        loadGroups()
    }

    override fun onResume() {
        super.onResume()
        loadGroups()
    }

    private fun loadGroups() {
        val groups = GroupManager.getAllGroups(requireContext())

        if (groups.isEmpty()) {
            emptyState.visibility = View.VISIBLE
            groupsRecyclerView.visibility = View.GONE
            groupMeta.text = "还没有分组"
        } else {
            emptyState.visibility = View.GONE
            groupsRecyclerView.visibility = View.VISIBLE

            val totalWords = groups.sumOf { it.words.size }
            groupMeta.text = "${groups.size} 个分组 · 共 $totalWords 词"

            adapter.updateGroups(groups)
        }
    }

    private fun showCreateGroupDialog() {
        val dialog = CreateGroupWithWordsDialog.newInstance { name, words ->
            val group = WordGroup(
                id = UUID.randomUUID().toString(),
                name = name,
                words = words.toMutableList()
            )
            GroupManager.saveGroup(requireContext(), group)
            loadGroups()

            if (words.isNotEmpty()) {
                android.widget.Toast.makeText(
                    requireContext(),
                    "已创建分组「$name」并添加 ${words.size} 个单词",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                android.widget.Toast.makeText(
                    requireContext(),
                    "已创建分组「$name」",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
        dialog.show(parentFragmentManager, "CreateGroupWithWordsDialog")
    }

    private fun showRenameDialog(group: WordGroup) {
        val dialog = CreateGroupDialog.newInstance(group.name) { newName ->
            group.name = newName
            group.updateTime = System.currentTimeMillis()
            GroupManager.saveGroup(requireContext(), group)
            loadGroups()
        }
        dialog.show(parentFragmentManager, "RenameGroupDialog")
    }

    private fun showDeleteConfirmDialog(group: WordGroup) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除分组")
            .setMessage("确定要删除「${group.name}」吗？\n这将删除 ${group.words.size} 个单词。")
            .setPositiveButton("删除") { _, _ ->
                GroupManager.deleteGroup(requireContext(), group.id)
                loadGroups()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun openGroup(group: WordGroup) {
        com.example.englishword.activity.GroupDetailActivity.start(requireContext(), group.id)
    }
}
