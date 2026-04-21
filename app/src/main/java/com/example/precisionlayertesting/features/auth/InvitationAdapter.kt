package com.example.precisionlayertesting.features.auth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.precisionlayertesting.core.models.auth.Invitation
import com.example.precisionlayertesting.databinding.ItemInvitationBinding
import java.util.Locale

class InvitationAdapter(
    private val onAccept: (Invitation) -> Unit,
    private val onReject: (Invitation) -> Unit
) : ListAdapter<Invitation, InvitationAdapter.InvitationViewHolder>(InvitationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvitationViewHolder {
        val binding = ItemInvitationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return InvitationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InvitationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class InvitationViewHolder(private val binding: ItemInvitationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(invitation: Invitation) {
            val workspaceName = invitation.workspace?.name ?: "Unknown Workspace"
            binding.tvWorkspaceName.text = workspaceName
            binding.tvWorkspaceInitial.text = workspaceName.take(1).uppercase()
            binding.tvRoleBadge.text = invitation.role.uppercase(Locale.getDefault())

            binding.btnAccept.setOnClickListener { onAccept(invitation) }
            binding.btnReject.setOnClickListener { onReject(invitation) }
        }
    }

    class InvitationDiffCallback : DiffUtil.ItemCallback<Invitation>() {
        override fun areItemsTheSame(oldItem: Invitation, newItem: Invitation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Invitation, newItem: Invitation): Boolean {
            return oldItem == newItem
        }
    }
}
