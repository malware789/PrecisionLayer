package com.example.precisionlayertesting.features.dashboard

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.precisionlayertesting.R
import com.example.precisionlayertesting.core.models.bugModel.AppVersion
import com.example.precisionlayertesting.databinding.ItemAppVersionBinding

class AppVersionAdapter(
    private var items: List<AppVersion>,
    var onClick: ((AppVersion) -> Unit)? = null
) : RecyclerView.Adapter<AppVersionAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAppVersionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppVersionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            val display = "${item.versionName}\n(B${item.buildNumber})"
            tvVersionNumber.text = display
            tvBuildTitle.text = item.versionTitle
            tvDate.text = item.createdAt?.split("T")?.firstOrNull() ?: "Recent"
            tvTeam.text = "Engineering" 
            tvDescription.text = item.releaseNotes ?: "Testing build for version ${item.versionName}"

            // Bugs (Static for now, sessions will show counts)
            tvBugCount.text = "-- Bugs"
            tvBugCount.setTextColor(Color.parseColor("#757575"))

            // Badge
            tvStatusBadge.text = "ACTIVE"
            tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_pill_cyan)
            tvStatusBadge.setTextColor(Color.parseColor("#006064"))

            root.setOnClickListener { onClick?.invoke(item) }
        }
    }

    fun updateData(newItems: List<AppVersion>) {
        items = newItems
        notifyDataSetChanged()
    }
}
