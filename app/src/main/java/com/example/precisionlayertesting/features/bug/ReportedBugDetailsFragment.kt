package com.example.precisionlayertesting.features.bug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.precisionlayertesting.core.utils.BugStyleUtils
import com.example.precisionlayertesting.databinding.FragmentReportedBugDetailsBinding

class ReportedBugDetailsFragment : Fragment() {

    private var _binding: FragmentReportedBugDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: ReportedBugDetailsFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportedBugDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
    }

    private fun setupUI() {
        val bug = args.bugDetail
        
        binding.tvBreadcrumb.text = args.breadcrumb
        binding.tvBugTitle.text = bug.title
        binding.tvBugId.text = "#BUG-${bug.id.take(8).uppercase()}"
        binding.tvDescription.text = bug.description
        binding.tvDate.text = "Reported: ${bug.createdAt?.split("T")?.firstOrNull() ?: "Unknown"}"
        
        BugStyleUtils.applySeverityStyle(binding.tvSeverity, bug.severity)
        BugStyleUtils.applyStatusStyle(binding.tvStatus, bug.status)

        binding.btnMarkResolved.setOnClickListener {
            // Future update logic
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
