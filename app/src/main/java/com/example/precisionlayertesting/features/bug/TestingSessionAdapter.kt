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
        
        b.tvUserName.text = session.title
        b.tvSessionTime.text = "Workspace: ${session.workspaceId.take(8)}"
        
        // Placeholder Avatar logic
        val initial = session.title.take(1).uppercase()
        b.tvInitials.text = initial
        b.tvInitials.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0E7FF"))
        b.tvInitials.setTextColor(Color.parseColor("#4338CA"))
        
        b.tvTotalBugs.text = "$bugCount Bugs Total"
        b.tvOpenBugs.text = "Managed State"
        b.tvResolvedBugs.text = ""

        b.root.setOnClickListener {
            onItemClick(session)
        }
    }

    override fun getItemCount(): Int = sessions.size
    
    fun updateData(newSessions: List<TestingSession>) {
        sessions = newSessions
        notifyDataSetChanged()
    }
}
