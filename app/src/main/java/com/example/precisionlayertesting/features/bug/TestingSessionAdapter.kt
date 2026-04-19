package com.example.precisionlayertesting.features.bug

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.precisionlayertesting.R
import com.example.precisionlayertesting.data.models.bug.TestingSession
import com.example.precisionlayertesting.databinding.ItemTestingSessionBinding

class TestingSessionAdapter(
    private var sessions: List<TestingSession>,
    private val onItemClick: (TestingSession) -> Unit
) : RecyclerView.Adapter<TestingSessionAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemTestingSessionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTestingSessionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val session = sessions[position]
        val b = holder.binding

        val bugCount = session.bugCount
        val reporterName = session.userProfile?.fullName ?: "Unknown Tester"
        
        b.tvUserName.text = reporterName
        b.tvSessionTime.text = formatTimeAgo(session.createdAt)
        
        // Avatar logic using initials
        val initial = reporterName.take(1).uppercase()
        b.tvInitials.text = initial
        b.tvInitials.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E8F0FE"))
        b.tvInitials.setTextColor(Color.parseColor("#1A73E8"))
        
        b.tvTotalBugs.text = if (bugCount == 1) "1 Bug Total" else "$bugCount Bugs Total"
        b.tvOpenBugs.text = "$bugCount Active"
        b.tvResolvedBugs.text = "0 Resolved" // Default for now

        b.root.setOnClickListener {
            onItemClick(session)
        }
    }

    private fun formatTimeAgo(dateStr: String?): String {
        if (dateStr == null) return "Unknown"
        return try {
            // Supabase returns timestamps like 2026-04-19T14:58:07.330846+00:00
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = sdf.parse(dateStr)
            if (date != null) {
                android.text.format.DateUtils.getRelativeTimeSpanString(date.time, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS).toString()
            }
            else "Recent"
        }
        catch (e: Exception) {
            "Recent"
        }
    }

    override fun getItemCount(): Int = sessions.size
    
    fun updateData(newSessions: List<TestingSession>) {
        sessions = newSessions
        notifyDataSetChanged()
    }
}
