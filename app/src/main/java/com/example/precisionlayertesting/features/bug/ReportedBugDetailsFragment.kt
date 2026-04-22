package com.example.precisionlayertesting.features.bug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.precisionlayertesting.R
import com.example.precisionlayertesting.core.utils.BugStyleUtils
import com.example.precisionlayertesting.databinding.FragmentReportedBugDetailsBinding
import com.example.precisionlayertesting.core.network.SupabaseConfig
import androidx.lifecycle.ViewModelProvider
import com.example.precisionlayertesting.core.di.ManualDI
class ReportedBugDetailsFragment : Fragment() {

    private var _binding: FragmentReportedBugDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: ReportedBugDetailsFragmentArgs by navArgs()

    private val viewModel: ReportedBugDetailsViewModel by lazy {
        val factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ReportedBugDetailsViewModel(ManualDI.bugRepository) as T
            }
        }
        ViewModelProvider(this, factory)[ReportedBugDetailsViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportedBugDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handleOnBackPressed()
        setupUI()
    }

    private fun setupUI() {
        val bug = args.bugDetail
        
        binding.tvBreadcrumb.text = args.breadcrumb
        binding.tvBugTitle.text = bug.title
        binding.tvBugId.text = "#BUG-${bug.id.take(8).uppercase()}"
        binding.tvDate.text = "Reported: ${bug.createdAt?.split("T")?.firstOrNull() ?: "Unknown"}"
        
        // Description
        if (bug.description.isNullOrBlank()) {
            binding.tvDescription.visibility = View.GONE
        } else {
            binding.tvDescription.text = bug.description
            binding.tvDescription.visibility = View.VISIBLE
        }

        // Steps to reproduce
        val steps = bug.stepsToRepro
        if (steps.isNullOrBlank()) {
            binding.llStepsContainer.visibility = View.GONE
        } else {
            binding.llStepsContainer.visibility = View.VISIBLE
            binding.llStepsList.removeAllViews()
            val stepLines = steps.split("\n").filter { it.isNotBlank() }
            stepLines.forEachIndexed { index, step ->
                val stepTextView = android.widget.TextView(requireContext()).apply {
                    text = "${index + 1}. $step"
                    setTextColor(resources.getColor(R.color.on_surface, null))
                    textSize = 13f
                    setPadding(0, 0, 0, 8.dpToPx())
                }
                binding.llStepsList.addView(stepTextView)
            }
        }

        // Evidence (Image)
        if (bug.imagePath.isNullOrBlank()) {
            binding.llEvidenceContainer.visibility = View.GONE
        }
        else {
            binding.llEvidenceContainer.visibility = View.VISIBLE
            
            // Set up observers
            viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
                if (isLoading) {
                    binding.ivEvidenceImage.setImageResource(R.drawable.shadow_profile) // or some loading graphic
                }
            }

            viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
                if (errorMessage != null) {
                    binding.ivEvidenceImage.setImageResource(R.drawable.ic_cloud_upload)
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            viewModel.viewUrl.observe(viewLifecycleOwner) { url ->
                if (url != null) {
                    Glide.with(this)
                        .load(url)
                        .placeholder(R.drawable.shadow_profile)
                        .error(R.drawable.ic_cloud_upload) // Fallback for error
                        .into(binding.ivEvidenceImage)
                        
                    binding.cvEvidenceImage.setOnClickListener {
                        showFullscreenImage(url)
                    }
                } else {
                    binding.llEvidenceContainer.visibility = View.GONE
                }
            }
            
            // Trigger fetch
            viewModel.fetchScreenshotUrl(bug.id)
        }

        // Activity Timeline
        setupTimeline(bug)
        
        BugStyleUtils.applySeverityStyle(binding.tvSeverity, bug.severity)
        BugStyleUtils.applyStatusStyle(binding.tvStatus, bug.status)

        binding.btnMarkResolved.setOnClickListener {
            Toast.makeText(requireContext(), "Marking as resolved...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTimeline(bug: com.example.precisionlayertesting.core.models.bugModel.BugReport) {
        binding.llTimelineList.removeAllViews()
        
        // Mock timeline item for "Bug Created"
        val inflater = LayoutInflater.from(requireContext())
        val itemView = inflater.inflate(R.layout.layout_timeline_item, binding.llTimelineList, false)
        
        val tvTitle = itemView.findViewById<android.widget.TextView>(R.id.tvTimelineTitle)
        val tvTime = itemView.findViewById<android.widget.TextView>(R.id.tvTimelineTime)
        
        tvTitle.text = "Bug Reported"
        tvTime.text = bug.createdAt?.split("T")?.firstOrNull() ?: "Just now"
        
        binding.llTimelineList.addView(itemView)
    }

    private fun showFullscreenImage(imageUrl: String) {
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_fullscreen_image)
        
        val imageView = dialog.findViewById<android.widget.ImageView>(R.id.ivFullscreen)
        val closeBtn = dialog.findViewById<android.view.View>(R.id.btnDetailClose)
        
        Glide.with(requireContext())
            .load(imageUrl)
            .into(imageView)
            
        closeBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun handleOnBackPressed() {
        // Correct syntax for the modern Back Press API
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
