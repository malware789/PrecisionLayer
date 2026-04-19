package com.example.precisionlayertesting.features.bug

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.precisionlayertesting.databinding.ItemAddedBugBinding

class AddedBugsAdapter : ListAdapter<ReportBugViewModel.BugDraft, AddedBugsAdapter.ViewHolder>(DIFF) {

    var onDeleteItem: ((Int) -> Unit)? = null
    var onEditItem: ((Int) -> Unit)? = null

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ReportBugViewModel.BugDraft>() {
            override fun areItemsTheSame(a: ReportBugViewModel.BugDraft, b: ReportBugViewModel.BugDraft) = 
                a.title == b.title && a.description == b.description
            override fun areContentsTheSame(a: ReportBugViewModel.BugDraft, b: ReportBugViewModel.BugDraft) = 
                a == b
        }
    }

    inner class ViewHolder(private val binding: ItemAddedBugBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            draft: ReportBugViewModel.BugDraft, 
            position: Int, 
            onDelete: (Int) -> Unit,
            onEdit: (Int) -> Unit
        ) {
            binding.tvBugTitle.text = draft.title
            binding.tvModuleName.text = draft.component
            binding.tvSeverityBadge.text = draft.severity.uppercase()

            binding.ivEdit.setOnClickListener { onEdit(position) }
            binding.ivDelete.setOnClickListener { onDelete(position) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAddedBugBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(
            getItem(position), 
            position, 
            onDelete = { idx -> onDeleteItem?.invoke(idx) },
            onEdit = { idx -> onEditItem?.invoke(idx) }
        )
    }
}
