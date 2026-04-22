package com.example.precisionlayertesting.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.precisionlayertesting.core.models.auth.WorkspaceMemberDetailed
import com.example.precisionlayertesting.databinding.ItemWorkspaceSwitcherBinding

class WorkspaceSwitcherAdapter(
    private var workspaces: List<WorkspaceMemberDetailed>,
    private var currentWorkspaceId: String?,
    private val onWorkspaceSelected: (WorkspaceMemberDetailed) -> Unit
) : RecyclerView.Adapter<WorkspaceSwitcherAdapter.ViewHolder>() {

    fun updateList(newList: List<WorkspaceMemberDetailed>, currentId: String?) {
        workspaces = newList
        currentWorkspaceId = currentId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWorkspaceSwitcherBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = workspaces[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = workspaces.size

    inner class ViewHolder(private val binding: ItemWorkspaceSwitcherBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WorkspaceMemberDetailed) {
            binding.tvWorkspaceName.text = item.workspace.name
            binding.tvRole.text = "Role: ${item.role.capitalize()}"

            val isSelected = item.workspaceId == currentWorkspaceId
            binding.ivCheck.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE

            binding.root.setOnClickListener {
                onWorkspaceSelected(item)
            }
        }
    }
}