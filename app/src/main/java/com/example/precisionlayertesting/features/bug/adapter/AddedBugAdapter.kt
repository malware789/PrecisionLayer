package com.example.precisionlayertesting.features.bug.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.precisionlayertesting.core.models.bugModel.BugReport
import com.example.precisionlayertesting.databinding.ItemAddedBugBinding

class AddedBugAdapter(private val bugs: List<BugReport>) :
    RecyclerView.Adapter<AddedBugAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAddedBugBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAddedBugBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bug = bugs[position]
        holder.binding.tvBugTitle.text = bug.title
        holder.binding.tvSeverityBadge.text = bug.severity.uppercase()
        holder.binding.tvModuleName.text = "Authentication Engine" // Dummy for now

        // Handle edit/delete actions if needed


    }

    override fun getItemCount(): Int = bugs.size
}