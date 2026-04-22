package com.example.precisionlayertesting.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.precisionlayertesting.core.models.bugModel.BugReport
import com.example.precisionlayertesting.databinding.ItemBugReportBinding

class BugReportAdapter(
    private var bugs: List<BugReport>,
    private val onItemClick: (BugReport) -> Unit
) : RecyclerView.Adapter<BugReportAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemBugReportBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBugReportBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bug = bugs[position]
        val b = holder.binding

        b.tvBugTitle.text = bug.title
        b.tvBugId.text = "#BUG-${bug.id.take(4).uppercase()}"
        b.tvSeverityBadge.text = bug.severity.uppercase()

        // Style the badge based on severity
        val (bgColor, textColor) = when (bug.severity.lowercase()) {
            "critical" -> Pair("#fef2f2", "#dc2626")
            "high" -> Pair("#fff7ed", "#ea580c")
            "medium" -> Pair("#fefce8", "#ca8a04")
            "low" -> Pair("#f0fdf4", "#16a34a")
            else -> Pair("#f1f5f9", "#475569")
        }

        b.tvSeverityBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgColor))
        b.tvSeverityBadge.setTextColor(Color.parseColor(textColor))

        if (bug.status.lowercase() == "closed") {
            b.cardContainer.setCardBackgroundColor(Color.parseColor("#4DFFFFFF"))
            b.root.alpha = 0.7f
            b.tvBugTitle.paintFlags = b.tvBugTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            b.ivIcon.visibility = View.GONE
            b.tvTime.text = "Resolved on ${bug.createdAt?.split("T")?.firstOrNull() ?: "Unknown"}"
        } else {
            b.cardContainer.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
            b.root.alpha = 1.0f
            b.tvBugTitle.paintFlags = b.tvBugTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            b.ivIcon.visibility = View.VISIBLE
            b.tvTime.text = "Status: ${bug.status.uppercase()}"
        }

        b.root.setOnClickListener { onItemClick(bug) }
    }

    override fun getItemCount(): Int = bugs.size

    fun updateData(newBugs: List<BugReport>) {
        bugs = newBugs
        notifyDataSetChanged()
    }
}