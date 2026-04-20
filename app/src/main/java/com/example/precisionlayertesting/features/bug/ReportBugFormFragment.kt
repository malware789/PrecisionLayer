package com.example.precisionlayertesting.features.bug

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.precisionlayertesting.R
import com.example.precisionlayertesting.core.di.ManualDI
import com.example.precisionlayertesting.data.models.bug.BugReport
import com.example.precisionlayertesting.databinding.FragmentReportBugFormBinding
import com.google.android.material.snackbar.Snackbar

class ReportBugFormFragment : Fragment() {

    private var _binding: FragmentReportBugFormBinding? = null
    private val binding get() = _binding!!

    private val args: ReportBugFormFragmentArgs by navArgs()

    private val viewModel: ReportBugViewModel by lazy {
        val factory = object : androidx.lifecycle.AbstractSavedStateViewModelFactory(this, null) {
            override fun <T : androidx.lifecycle.ViewModel> create(
                key: String,
                modelClass: Class<T>,
                handle: androidx.lifecycle.SavedStateHandle
            ): T {
                val workspaceId = ManualDI.prefsManager.getWorkspaceId() ?: ""
                @Suppress("UNCHECKED_CAST")
                return ReportBugViewModel(
                    ManualDI.bugRepository,
                    handle,
                    workspaceId,
                    args.moduleId,
                    args.versionId
                ) as T
            }
        }
        ViewModelProvider(this, factory)[ReportBugViewModel::class.java]
    }

    private var selectedScreenshotUri: Uri? = null
    private var editingDraftIndex: Int? = null
    private val stepViews = mutableListOf<EditText>()

    private val addedBugsAdapter = AddedBugsAdapter()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.onScreenshotSelected(requireContext(), it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBugFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupUI()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.rvAddedBugs.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = addedBugsAdapter
        }
        addedBugsAdapter.onDeleteItem = { index -> 
            viewModel.removeDraftAt(index)
            if (editingDraftIndex == index) cancelEdit()
        }
        addedBugsAdapter.onEditItem = { index -> 
            loadForEdit(index)
        }
    }

    private fun loadForEdit(index: Int) {
        val draft = viewModel.draftBugs.value?.getOrNull(index) ?: return
        editingDraftIndex = index
        
        binding.etBugTitle.setText(draft.title)
        binding.etDescription.setText(draft.description)
        
        if (binding.atvComponent.isEnabled) {
            binding.atvComponent.setText(draft.component, false)
        }

        // Set priority
        val priorityId = when (draft.severity) {
            BugReport.SEVERITY_LOW -> R.id.btnLow
            BugReport.SEVERITY_MEDIUM -> R.id.btnMed
            BugReport.SEVERITY_CRITICAL -> R.id.btnCrit
            else -> R.id.btnHigh
        }
        binding.togglePriority.check(priorityId)

        // Load steps
        binding.llStepsContainer.removeAllViews()
        stepViews.clear()
        val steps = draft.steps?.split("\n") ?: emptyList()
        if (steps.isEmpty()) {
            addStepRow(binding.llStepsContainer, 1)
        } else {
            steps.forEachIndexed { i, stepText ->
                val pureText = stepText.substringAfter(". ")
                addRowWithText(binding.llStepsContainer, i + 1, pureText)
            }
        }

        // Pre-fill attachment state in ViewModel for editing (so user can see/replace it)
        // Note: In a real app, you might want a specialized 'loadEditState' method in VM
        
        binding.btnAddAnotherBug.text = "Update Bug"
        binding.tvFormTitle.text = "Edit Bug Draft"
        
        // Scroll to top to see edit form
        binding.nestedScrollView.smoothScrollTo(0, 0)
    }

    private fun addRowWithText(container: LinearLayout, stepNumber: Int, text: String) {
        addStepRow(container, stepNumber)
        stepViews.lastOrNull()?.setText(text)
    }

    private fun cancelEdit() {
        editingDraftIndex = null
        resetForm()
        viewModel.clearAttachment()
        binding.btnAddAnotherBug.text = "Add Another Bug"
        binding.tvFormTitle.text = "Bug Information"
    }

    private fun setupUI() {
        // --- Component Autocomplete ---
        viewModel.modules.observe(viewLifecycleOwner) { modules ->
            val names = modules.map { it.name }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names)
            binding.atvComponent.setAdapter(adapter)
            if (names.contains(args.moduleName)) {
                binding.atvComponent.setText(args.moduleName, false)
            }
        }

        // --- Pre-populate component from nav args and lock if necessary ---
        if (args.moduleName.isNotBlank()) {
            binding.atvComponent.setText(args.moduleName, false)
            binding.atvComponent.isEnabled = false
            binding.tilComponent.isEnabled = false
            // Optional: visual cue that it's read-only
            binding.tilComponent.helperText = "Fixed for this session"
        }

        // --- Steps: wire up initial step view ---
        val stepsContainer = binding.llStepsContainer
        // Clear template step inflated from XML and manage dynamically
        stepsContainer.removeAllViews()
        addStepRow(stepsContainer, 1)

        binding.btnAddStep.setOnClickListener {
            addStepRow(stepsContainer, stepViews.size + 1)
        }

        // --- Attachment area ---
        binding.attachmentPlaceholder.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // --- Add Another/Update Bug ---
        binding.btnAddAnotherBug.setOnClickListener {
            if (validateForm()) {
                val priority = getSelectedSeverity()
                val stepsText = getStepsString()

                if (editingDraftIndex != null) {
                    viewModel.updateDraftAt(
                        editingDraftIndex!!,
                        title = binding.etBugTitle.text.toString().trim(),
                        component = binding.atvComponent.text.toString().trim(),
                        severity = priority,
                        description = binding.etDescription.text.toString().trim(),
                        steps = stepsText.ifBlank { null }
                    )
                    cancelEdit()
                    Snackbar.make(binding.root, "✓ Draft updated", Snackbar.LENGTH_SHORT).show()
                } else {
                    addCurrentBugToDraft()
                    resetForm()
                    Snackbar.make(binding.root, "✓ Bug added to batch", Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        // --- Submit All Bugs ---
        binding.btnSubmitAll.setOnClickListener {
            val hasDrafts = viewModel.draftBugs.value?.isNotEmpty() == true
            val currentFormValid = validateForm(showErrors = false)

            if (!hasDrafts && !currentFormValid) {
                Snackbar.make(binding.root, "Please fill in at least one bug report", Snackbar.LENGTH_LONG).show()
                validateForm(showErrors = true)
                return@setOnClickListener
            }

            // Show confirmation dialog
            val count = (viewModel.draftBugs.value?.size ?: 0) + (if (currentFormValid) 1 else 0)
            
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Submit Bug Reports")
                .setMessage("You are about to submit $count bug report(s). Continue?")
                .setPositiveButton("Submit") { _, _ ->
                    if (currentFormValid) {
                        addCurrentBugToDraft()
                    }
                    val userId = ManualDI.prefsManager.getUserId() ?: ""
                    viewModel.submitAllBugs(requireContext(), userId, args.sessionId)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // --- Cancel ---
        binding.btnCancel.setOnClickListener {
            if (editingDraftIndex != null) {
                cancelEdit()
            } else if (viewModel.draftBugs.value?.isNotEmpty() == true) {
                  com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Discard Session?")
                    .setMessage("You have unsaved drafts. Are you sure you want to leave?")
                    .setPositiveButton("Discard & Exit") { _, _ -> findNavController().popBackStack() }
                    .setNegativeButton("Keep Editing", null)
                    .show()
            } else {
                findNavController().popBackStack()
            }
        }
    }

    private fun addStepRow(container: LinearLayout, stepNumber: Int) {
        val row = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_step_row, container, false) as LinearLayout

        val numberLabel = row.getChildAt(0) as TextView
        val editText = row.getChildAt(1) as EditText

        numberLabel.text = "$stepNumber."
        editText.hint = "Step $stepNumber..."
        editText.text = null

        container.addView(row)
        stepViews.add(editText)
    }

    private fun observeViewModel() {
        viewModel.draftBugs.observe(viewLifecycleOwner) { drafts ->
            addedBugsAdapter.submitList(drafts.toList())
            binding.rvAddedBugs.visibility = if (drafts.isEmpty()) View.GONE else View.VISIBLE
            
            // Update counter
            binding.tvBugCount.text = "${drafts.size} Bug${if (drafts.size == 1) "" else "s"}"
            binding.tvBugCount.visibility = if (drafts.isEmpty()) View.GONE else View.VISIBLE
            viewModel.isCurrentAttachmentUploading.observe(viewLifecycleOwner) { isUploading ->
            binding.btnAddAnotherBug.isEnabled = !isUploading
            binding.attachmentArea.alpha = if (isUploading) 0.5f else 1.0f
            binding.pbAttachmentUpload.visibility = if (isUploading) View.VISIBLE else View.GONE
            
            if (isUploading) {
                binding.btnAddAnotherBug.text = "Processing Image..."
                binding.attachmentPlaceholder.isClickable = false
            }
            else {
                binding.btnAddAnotherBug.text = if (editingDraftIndex != null) "Update Bug" else "Add Another Bug"
                binding.attachmentPlaceholder.isClickable = true
            }
        }

        viewModel.submissionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ReportBugViewModel.SubmissionState.Submitting -> {
                    binding.btnSubmitAll.isEnabled = false
                    binding.btnSubmitAll.text = "Submitting..."
                    // Lock other actions
                    binding.btnAddAnotherBug.isEnabled = false
                }
                is ReportBugViewModel.SubmissionState.Success -> {
                    Snackbar.make(binding.root, "✓ All bugs submitted successfully", Snackbar.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }
                is ReportBugViewModel.SubmissionState.Error -> {
                    binding.btnSubmitAll.isEnabled = true
                    binding.btnSubmitAll.text = "Submit All Bugs"
                    binding.btnAddAnotherBug.isEnabled = true
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Submission Failed")
                        .setMessage(state.message)
                        .setPositiveButton("Retry", null)
                        .show()
                }
                else -> {
                    // Idle state - reset if needed
                }
            }
        }

        viewModel.isAnyUploadInProgress.observe(viewLifecycleOwner) { isAnyLoading ->
            // Prevent global submission if any bug (current or draft) is still processing
            binding.btnSubmitAll.isEnabled = !isAnyLoading
            if (isAnyLoading) {
                binding.btnSubmitAll.alpha = 0.5f
                if (viewModel.submissionState.value is ReportBugViewModel.SubmissionState.Submitting) {
                    binding.btnSubmitAll.text = "Uploading Evidence..."
                }
            } else {
                binding.btnSubmitAll.alpha = 1.0f
                binding.btnSubmitAll.text = "Submit All Bugs"
            }
        }
  }

        binding.ivDeleteScreenshot.setOnClickListener {
            viewModel.removeCurrentScreenshot()
        }

        viewModel.currentCachedUri.observe(viewLifecycleOwner) { uri ->
             if (uri != null) {
                binding.ivScreenshotPreview.setImageURI(uri)
                binding.attachmentPlaceholder.visibility = View.GONE
                binding.ivScreenshotPreview.visibility = View.VISIBLE
                binding.ivDeleteScreenshot.visibility = View.VISIBLE
            }
             else {
                binding.ivScreenshotPreview.setImageDrawable(null)
                binding.attachmentPlaceholder.visibility = View.VISIBLE
                binding.ivScreenshotPreview.visibility = View.GONE
                binding.ivDeleteScreenshot.visibility = View.GONE
            }
        }

        viewModel.uploadError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Snackbar.make(binding.root, "Attachment Error: $error", Snackbar.LENGTH_LONG).show()
            }
        }

        viewModel.submissionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ReportBugViewModel.SubmissionState.Submitting -> setLoading(true)
                is ReportBugViewModel.SubmissionState.Success -> {
                    setLoading(false)
                    Toast.makeText(requireContext(), "✓ All bugs submitted!", Toast.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }
                is ReportBugViewModel.SubmissionState.Error -> {
                    setLoading(false)
                    Snackbar.make(binding.root, "Error: ${state.message}", Snackbar.LENGTH_LONG)
                        .setBackgroundTint(resources.getColor(android.R.color.holo_red_dark, null))
                        .show()
                }
                else -> setLoading(false)
            }
        }
    }

    private fun validateForm(showErrors: Boolean = true): Boolean {
        var valid = true

        if (binding.etBugTitle.text.isNullOrBlank()) {
            if (showErrors) binding.tilBugTitle.error = "Title is required"
            valid = false
        } else {
            binding.tilBugTitle.error = null
        }

        if (binding.atvComponent.text.isNullOrBlank()) {
            if (showErrors) binding.tilComponent.error = "Component is required"
            valid = false
        } else {
            binding.tilComponent.error = null
        }

        if (binding.etDescription.text.isNullOrBlank()) {
            if (showErrors) binding.tilDescription.error = "Description is required"
            valid = false
        } else {
            binding.tilDescription.error = null
        }

        return valid
    }

    private fun getSelectedSeverity(): String {
        return when (binding.togglePriority.checkedButtonId) {
            R.id.btnLow -> BugReport.SEVERITY_LOW
            R.id.btnMed -> BugReport.SEVERITY_MEDIUM
            R.id.btnHigh -> BugReport.SEVERITY_HIGH
            R.id.btnCrit -> BugReport.SEVERITY_CRITICAL
            else -> BugReport.SEVERITY_HIGH
        }
    }

    private fun getStepsString(): String {
        return stepViews
            .mapIndexed { i, et -> "${i + 1}. ${et.text.toString().trim()}" }
            .filter { it.substringAfter(". ").isNotBlank() }
            .joinToString("\n")
    }

    private fun addCurrentBugToDraft() {
        viewModel.addBugToDraft(
            title = binding.etBugTitle.text.toString().trim(),
            component = binding.atvComponent.text.toString().trim(),
            severity = getSelectedSeverity(),
            description = binding.etDescription.text.toString().trim(),
            steps = getStepsString().ifBlank { null }
        )
    }

    private fun resetForm() {
        binding.etBugTitle.text = null
        binding.etDescription.text = null
        if (binding.atvComponent.isEnabled) {
            binding.atvComponent.text = null
        }

        // Reset steps
        stepViews.clear()
        binding.llStepsContainer.removeAllViews()
        addStepRow(binding.llStepsContainer, 1)

        // Reset priority to HIGH
        binding.togglePriority.check(R.id.btnHigh)

        binding.ivScreenshotPreview.setImageDrawable(null)
        binding.attachmentPlaceholder.visibility = View.VISIBLE
        binding.ivScreenshotPreview.visibility = View.GONE

        binding.tilBugTitle.error = null
        binding.tilComponent.error = null
        binding.tilDescription.error = null
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSubmitAll.isEnabled = !loading
        binding.btnAddAnotherBug.isEnabled = !loading
        binding.btnCancel.isEnabled = !loading
        binding.btnSubmitAll.text = if (loading) "Submitting..." else "Submit All Bugs"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
