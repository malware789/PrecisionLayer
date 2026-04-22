package com.example.precisionlayertesting.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.precisionlayertesting.core.models.bugModel.BugDraft
import com.example.precisionlayertesting.databinding.ItemAddedBugBinding

class AddedBugsAdapter : ListAdapter<BugDraft, AddedBugsAdapter.ViewHolder>(DIFF) {

    var onDeleteItem: ((Int) -> Unit)? = null
    var onEditItem: ((Int) -> Unit)? = null

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<BugDraft>() {
            override fun areItemsTheSame(a: BugDraft, b: BugDraft) =
                a.title == b.title && a.description == b.description
            override fun areContentsTheSame(a: BugDraft, b: BugDraft) =
                a == b
        }
    }

    inner class ViewHolder(private val binding: ItemAddedBugBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            draft: BugDraft,
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