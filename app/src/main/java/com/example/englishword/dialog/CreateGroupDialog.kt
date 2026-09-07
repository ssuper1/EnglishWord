package com.example.englishword.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.englishword.R

class CreateGroupDialog : DialogFragment() {

    private var onGroupCreated: ((String) -> Unit)? = null
    private var initialName: String? = null

    companion object {
        private const val ARG_INITIAL_NAME = "initial_name"

        fun newInstance(initialName: String? = null, onCreated: (String) -> Unit): CreateGroupDialog {
            return CreateGroupDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_INITIAL_NAME, initialName)
                }
                this.onGroupCreated = onCreated
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        initialName = arguments?.getString(ARG_INITIAL_NAME)

        val view = layoutInflater.inflate(R.layout.dialog_create_group, null)
        val etGroupName = view.findViewById<EditText>(R.id.etGroupName)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancel)
        val btnCreate = view.findViewById<TextView>(R.id.btnCreate)

        initialName?.let {
            etGroupName.setText(it)
            etGroupName.setSelection(it.length)
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnCreate.setOnClickListener {
            val name = etGroupName.text.toString().trim()
            if (name.isNotEmpty()) {
                onGroupCreated?.invoke(name)
                dismiss()
            } else {
                etGroupName.error = "请输入分组名称"
            }
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }
}
