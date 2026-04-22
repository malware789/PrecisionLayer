package com.example.precisionlayertesting.features.bug

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.precisionlayertesting.R
import com.example.precisionlayertesting.core.di.ManualDI
import com.example.precisionlayertesting.core.models.bugModel.BugDraft
import com.example.precisionlayertesting.core.models.bugModel.BugReport
import com.example.precisionlayertesting.databinding.FragmentReportBugFormBinding
import com.example.precisionlayertesting.adapter.AddedBugsAdapter
import com.google.android.material.snackbar.Snackbar
import java.util.UUID

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

    // Tracks each step row's EditText AND its delete ImageView
    private data class StepRow(val editText: EditText, val deleteIcon: ImageView)
    private val stepRows = mutableListOf<StepRow>()

    // Convenience accessor kept for legacy helpers that used stepViews
    private val stepViews: List<EditText> get() = stepRows.map { it.editText }

    private val addedBugsAdapter = AddedBugsAdapter()

    private var lastBackPressedTime: Long = 0L

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
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
        setupBackPressHandling()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Back press
    // ─────────────────────────────────────────────────────────────────────────
    private fun setupBackPressHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (System.currentTimeMillis() - lastBackPressedTime < 2000) {
                findNavController().popBackStack()
            } else {
                lastBackPressedTime = System.currentTimeMillis()
                Toast.makeText(requireContext(), "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RecyclerView
    // ─────────────────────────────────────────────────────────────────────────
    private fun setupRecyclerView() {
        binding.rvAddedBugs.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = addedBugsAdapter
        }

        // Delete chip
        addedBugsAdapter.onDeleteItem = { index ->
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Remove Bug Draft?")
                .setMessage("Are you sure you want to remove this bug draft?")
                .setPositiveButton("Remove") { _, _ ->
                    viewModel.removeDraftAt(index)
                    if (editingDraftIndex == index) cancelEdit()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Edit chip — wired here (was already implemented in ViewModel/Fragment, adapter just needed the callback)
        addedBugsAdapter.onEditItem = { index ->
            loadForEdit(index)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load a draft into the form for editing
    // ─────────────────────────────────────────────────────────────────────────
    private fun loadForEdit(index: Int) {
        val draft = viewModel.draftBugs.value?.getOrNull(index) ?: return
        editingDraftIndex = index

        binding.etBugTitle.setText(draft.title)
        binding.etDescription.setText(draft.description)

        if (binding.atvComponent.isEnabled) {
            binding.atvComponent.setText(draft.component, false)
        }

        // Priority
        val priorityId = when (draft.severity) {
            BugReport.SEVERITY_LOW      -> R.id.btnLow
            BugReport.SEVERITY_MEDIUM   -> R.id.btnMed
            BugReport.SEVERITY_CRITICAL -> R.id.btnCrit
            else                        -> R.id.btnHigh
        }
        binding.togglePriority.check(priorityId)

        // Steps
        binding.llStepsContainer.removeAllViews()
        stepRows.clear()
        val steps = draft.steps?.split("\n") ?: emptyList()
        if (steps.isEmpty()) {
            addStepRow(binding.llStepsContainer, 1)
        } else {
            steps.forEachIndexed { i, stepText ->
                val pureText = stepText.substringAfter(". ")
                addRowWithText(binding.llStepsContainer, i + 1, pureText)
            }
        }
        refreshStepDeleteIcons()

        // Restore attachment
        viewModel.restoreAttachmentForEdit(draft.cachedUri, draft.mimeType)

        binding.toolbar.btnAddAnotherBug.text = "Update Bug"
        updateProgressBar()

        // Scroll to top so user sees the form
        binding.nestedScrollView.smoothScrollTo(0, 0)
    }

    private fun addRowWithText(container: LinearLayout, stepNumber: Int, text: String) {
        addStepRow(container, stepNumber)
        stepRows.lastOrNull()?.editText?.setText(text)
    }

    private fun cancelEdit() {
        editingDraftIndex = null
        resetForm()
        viewModel.clearAttachment()
        binding.toolbar.btnAddAnotherBug.text = "+ Add Bug"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI setup
    // ─────────────────────────────────────────────────────────────────────────
    private fun setupUI() {

        // Component autocomplete
        viewModel.modules.observe(viewLifecycleOwner) { modules ->
            val names = modules.map { it.name }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names)
            binding.atvComponent.setAdapter(adapter)
            if (names.contains(args.moduleName)) {
                binding.atvComponent.setText(args.moduleName, false)
            }
        }

        // Lock component if pre-filled from nav args
        if (args.moduleName.isNotBlank()) {
            binding.atvComponent.setText(args.moduleName, false)
            binding.atvComponent.isEnabled = false
            binding.tilComponent.isEnabled = false
            binding.tilComponent.helperText = "Fixed for this session"
        }

        // Steps — clear XML placeholder, add first row dynamically
        binding.llStepsContainer.removeAllViews()
        addStepRow(binding.llStepsContainer, 1)

        binding.btnAddStep.setOnClickListener {
            addStepRow(binding.llStepsContainer, stepRows.size + 1)
            refreshStepDeleteIcons()
        }

        // Attachment
        binding.attachmentPlaceholder.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Add / Update bug
        binding.toolbar.btnAddAnotherBug.setOnClickListener {
            if (validateForm()) {
                val priority = getSelectedSeverity()
                val stepsText = getStepsString()

                if (editingDraftIndex != null) {
                    viewModel.updateDraftAt(
                        editingDraftIndex!!,
                        title       = binding.etBugTitle.text.toString().trim(),
                        component   = binding.atvComponent.text.toString().trim(),
                        severity    = priority,
                        description = binding.etDescription.text.toString().trim(),
                        steps       = stepsText.ifBlank { null }
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

        // Submit all
        binding.btnSubmitAll.setOnClickListener {
            val hasDrafts       = viewModel.draftBugs.value?.isNotEmpty() == true
            val currentFormValid = validateForm(showErrors = false)

            if (!hasDrafts && !currentFormValid) {
                Snackbar.make(binding.root, "Please fill in at least one bug report", Snackbar.LENGTH_LONG).show()
                validateForm(showErrors = true)
                return@setOnClickListener
            }

            val draftsCount = viewModel.draftBugs.value?.size ?: 0
            val isEditing   = editingDraftIndex != null
            val count       = if (isEditing || !currentFormValid) draftsCount else draftsCount + 1

            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Submit Bug Reports")
                .setMessage("You are about to submit $count bug report(s). Continue?")
                .setPositiveButton("Submit") { _, _ ->
                    val finalBug = if (currentFormValid) {
                        val currentDrafts = viewModel.draftBugs.value.orEmpty()
                        val existingId    = if (isEditing) currentDrafts.getOrNull(editingDraftIndex!!)?.id else null
                        BugDraft(
                            id          = existingId ?: UUID.randomUUID().toString(),
                            title       = binding.etBugTitle.text.toString().trim(),
                            component   = binding.atvComponent.text.toString().trim(),
                            severity    = getSelectedSeverity(),
                            description = binding.etDescription.text.toString().trim(),
                            steps       = getStepsString().ifBlank { null },
                            cachedUri   = viewModel.currentCachedUri.value,
                            mimeType    = viewModel.currentMimeType.value
                        )
                    } else null

                    val userId = ManualDI.prefsManager.getUserId() ?: ""
                    viewModel.submitAllBugs(requireContext(), userId, args.sessionId, finalBug)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Cancel
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

        // Text watchers for live progress bar update
        listOf(binding.etBugTitle, binding.etDescription).forEach { et ->
            et.addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) = updateProgressBar()
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            })
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step rows
    // ─────────────────────────────────────────────────────────────────────────
    private fun addStepRow(container: LinearLayout, stepNumber: Int) {
        val row = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_step_row, container, false) as LinearLayout

        val numberLabel = row.findViewById<TextView>(R.id.tvStepNumber)
        val editText    = row.findViewById<EditText>(R.id.etStep)
        val deleteIcon  = row.findViewById<ImageView>(R.id.ivDeleteStep)

        numberLabel.text = "$stepNumber."
        editText.hint    = "Step $stepNumber..."
        editText.text    = null

        deleteIcon.setOnClickListener {
            val idx = stepRows.indexOfFirst { it.editText == editText }
            if (idx != -1) {
                stepRows.removeAt(idx)
                container.removeView(row)
                // Renumber remaining rows
                stepRows.forEachIndexed { i, sr ->
                    (sr.editText.parent as? LinearLayout)
                        ?.findViewById<TextView>(R.id.tvStepNumber)
                        ?.text = "${i + 1}."
                    sr.editText.hint = "Step ${i + 1}..."
                }
                refreshStepDeleteIcons()
            }
        }

        container.addView(row)
        stepRows.add(StepRow(editText, deleteIcon))
    }

    /** Show delete icon only when there are 2+ steps */
    private fun refreshStepDeleteIcons() {
        val show = stepRows.size > 1
        stepRows.forEach { it.deleteIcon.visibility = if (show) View.VISIBLE else View.GONE }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Progress bar (0-4 based on filled fields)
    // ─────────────────────────────────────────────────────────────────────────
    private fun updateProgressBar() {
        var progress = 0
        if (!binding.etBugTitle.text.isNullOrBlank())   progress++
        if (!binding.atvComponent.text.isNullOrBlank()) progress++
        if (!binding.etDescription.text.isNullOrBlank()) progress++
        if (stepRows.any { !it.editText.text.isNullOrBlank() }) progress++
        binding.progressFormCompletion.progress = progress
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Observers
    // ─────────────────────────────────────────────────────────────────────────
    private fun observeViewModel() {

        viewModel.draftBugs.observe(viewLifecycleOwner) { drafts ->
            addedBugsAdapter.submitList(drafts.toList())

            val hasDrafts = drafts.isNotEmpty()
            binding.rvAddedBugs.visibility  = if (hasDrafts) View.VISIBLE else View.GONE
            binding.toolbar.tvBugCount.text         = "${drafts.size} Bug${if (drafts.size == 1) "" else "s"}"
            binding.toolbar.tvBugCount.visibility   = if (hasDrafts) View.VISIBLE else View.GONE
        }

        viewModel.isCurrentAttachmentUploading.observe(viewLifecycleOwner) { isUploading ->
            binding.toolbar.btnAddAnotherBug.isEnabled      = !isUploading
            binding.attachmentArea.alpha            = if (isUploading) 0.5f else 1.0f
            binding.pbAttachmentUpload.visibility   = if (isUploading) View.VISIBLE else View.GONE

            if (isUploading) {
                binding.toolbar.btnAddAnotherBug.text           = "Processing Image..."
                binding.attachmentPlaceholder.isClickable = false
            } else {
                binding.toolbar.btnAddAnotherBug.text           =
                    if (editingDraftIndex != null) "Update Bug" else "+ Add Bug"
                binding.attachmentPlaceholder.isClickable = true
            }
        }

        viewModel.submissionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ReportBugViewModel.SubmissionState.Submitting -> setLoading(true)
                is ReportBugViewModel.SubmissionState.Success -> {
                    setLoading(false)
                    Toast.makeText(requireContext(), "✓ All bugs submitted successfully!", Toast.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }
                is ReportBugViewModel.SubmissionState.Error -> {
                    setLoading(false)
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Submission Failed")
                        .setMessage(state.message)
                        .setPositiveButton("Retry", null)
                        .show()
                }
                else -> setLoading(false)
            }
        }

        viewModel.isAnyUploadInProgress.observe(viewLifecycleOwner) { isAnyLoading ->
            binding.btnSubmitAll.isEnabled = !isAnyLoading
            binding.btnSubmitAll.alpha     = if (isAnyLoading) 0.5f else 1.0f
            if (isAnyLoading &&
                viewModel.submissionState.value is ReportBugViewModel.SubmissionState.Submitting
            ) {
                binding.btnSubmitAll.text = "Uploading Evidence..."
            } else if (!isAnyLoading) {
                binding.btnSubmitAll.text = "Submit All Bugs"
            }
        }

        binding.ivDeleteScreenshot.setOnClickListener {
            viewModel.removeCurrentScreenshot()
        }

        viewModel.currentCachedUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                binding.ivScreenshotPreview.setImageURI(uri)
                binding.attachmentPlaceholder.visibility = View.GONE
                binding.ivScreenshotPreview.visibility   = View.VISIBLE
                binding.ivDeleteScreenshot.visibility    = View.VISIBLE
                binding.ivScreenshotPreview.setOnClickListener { showFullscreenImage(uri) }
                updateProgressBar()
            } else {
                binding.ivScreenshotPreview.setImageDrawable(null)
                binding.attachmentPlaceholder.visibility = View.VISIBLE
                binding.ivScreenshotPreview.visibility   = View.GONE
                binding.ivDeleteScreenshot.visibility    = View.GONE
                updateProgressBar()
            }
        }

        viewModel.uploadError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Snackbar.make(binding.root, "Attachment Error: $error", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────
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

    private fun getSelectedSeverity(): String = when (binding.togglePriority.checkedButtonId) {
        R.id.btnLow  -> BugReport.SEVERITY_LOW
        R.id.btnMed  -> BugReport.SEVERITY_MEDIUM
        R.id.btnHigh -> BugReport.SEVERITY_HIGH
        R.id.btnCrit -> BugReport.SEVERITY_CRITICAL
        else         -> BugReport.SEVERITY_LOW
    }

    private fun getStepsString(): String = stepRows
        .mapIndexed { i, row -> "${i + 1}. ${row.editText.text.toString().trim()}" }
        .filter { it.substringAfter(". ").isNotBlank() }
        .joinToString("\n")

    private fun addCurrentBugToDraft() {
        viewModel.addBugToDraft(
            title       = binding.etBugTitle.text.toString().trim(),
            component   = binding.atvComponent.text.toString().trim(),
            severity    = getSelectedSeverity(),
            description = binding.etDescription.text.toString().trim(),
            steps       = getStepsString().ifBlank { null }
        )
    }

    private fun showFullscreenImage(uri: Uri) {
        val imageView = ImageView(requireContext()).apply {
            setPadding(32, 32, 32, 32)
            setImageURI(uri)
            adjustViewBounds = true
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(imageView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun resetForm() {
        binding.etBugTitle.text    = null
        binding.etDescription.text = null
        if (binding.atvComponent.isEnabled) binding.atvComponent.text = null

        stepRows.clear()
        binding.llStepsContainer.removeAllViews()
        addStepRow(binding.llStepsContainer, 1)
        refreshStepDeleteIcons()

        binding.togglePriority.check(R.id.btnLow)

        binding.ivScreenshotPreview.setImageDrawable(null)
        binding.attachmentPlaceholder.visibility = View.VISIBLE
        binding.ivScreenshotPreview.visibility   = View.GONE

        binding.tilBugTitle.error    = null
        binding.tilComponent.error   = null
        binding.tilDescription.error = null

        updateProgressBar()
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSubmitAll.isEnabled       = !loading
        binding.toolbar.btnAddAnotherBug.isEnabled   = !loading
        binding.btnCancel.isEnabled          = !loading
        binding.btnSubmitAll.text            = if (loading) "Submitting..." else "Submit All Bugs"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}