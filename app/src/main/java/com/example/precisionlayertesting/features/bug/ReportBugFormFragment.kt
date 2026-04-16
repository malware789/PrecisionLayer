package com.example.precisionlayertesting.features.bug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.precisionlayertesting.data.models.bug.BugReport
import com.example.precisionlayertesting.databinding.FragmentReportBugFormBinding

class ReportBugFormFragment : Fragment() {

    private var _binding: FragmentReportBugFormBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBugFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupAddedBugsList()
    }

    private fun setupAddedBugsList() {
        // dummy workspace and session IDs for preview
        val wId = "work-123"
        val sId = "sess-456"
        
        val dummyBugs = listOf(
            BugReport("1", sId, wId, "UI layout shift on login", "High", "Open"),
            BugReport("2", sId, wId, "Slow response on refresh", "Medium", "Open"),
            BugReport("3", sId, wId, "Token leak in logs", "Critical", "In Progress"),
            BugReport("4", sId, wId, "Need to change title", "Low", "Closed")
        )
        binding.rvAddedBugs.adapter = AddedBugAdapter(dummyBugs)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
