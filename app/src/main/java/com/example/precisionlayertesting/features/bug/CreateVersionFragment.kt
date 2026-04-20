package com.example.precisionlayertesting.features.bug

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.precisionlayertesting.core.di.ManualDI
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.core.utils.ApkMetadata
import androidx.activity.result.contract.ActivityResultContracts
import com.example.precisionlayertesting.databinding.FragmentCreateVersionBinding
import com.google.android.material.snackbar.Snackbar
import android.net.Uri
import android.widget.Toast

class CreateVersionFragment : Fragment() {

    private var _binding: FragmentCreateVersionBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: VersionViewModel
    private val args: CreateVersionFragmentArgs by navArgs()
    
    private var selectedApkUri: Uri? = null

    private val selectApkLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedApkUri = it
            binding.tvApkStatus.text = it.lastPathSegment ?: "APK File Selected"
            binding.ivApkIcon.setImageResource(com.example.precisionlayertesting.R.drawable.ic_schedule)
            binding.ivApkIcon.imageTintList = android.content.res.ColorStateList.valueOf(resources.getColor(android.R.color.holo_green_dark, null))
            
            // Trigger automatic extraction
            viewModel.extractMetadata(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateVersionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return VersionViewModel(
                    ManualDI.bugRepository, 
                    ManualDI.prefsManager,
                    ManualDI.apkMetadataExtractor
                ) as T
            }
        })[VersionViewModel::class.java]

        setupClickListeners()
        observeViewModel()
    }

    private fun updateButtonState(isProcessing: Boolean = false) {
        // Submit enabled only if APK is selected, metadata extraction succeeded, and not currently processing
        val hasApk = selectedApkUri != null
        val hasMetadata = viewModel.extractedMetadata.value != null
        
        binding.btnCreateVersion.isEnabled = hasApk && hasMetadata && !isProcessing
        binding.btnCreateVersion.alpha = if (binding.btnCreateVersion.isEnabled) 1.0f else 0.5f
    }

    private fun setupClickListeners() {
        binding.btnCreateVersion.setOnClickListener {
            val uri = selectedApkUri ?: return@setOnClickListener
            val versionTitle = binding.etVersionTitle.text.toString()
            val releaseNotes = binding.etReleaseNotes.text?.toString()

            if (!versionTitle.isNullOrEmpty()){
                viewModel.uploadApkAndCreateVersion(
                    uri = uri,
                    moduleId = args.moduleId,
                    versionTitle = versionTitle,
                    releaseNotes = releaseNotes
                )
            }
            else{
                Toast.makeText(requireContext(),"Version title cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        binding.layoutUploadApk.setOnClickListener {
            selectApkLauncher.launch("application/vnd.android.package-archive")
        }
    }

    private fun observeViewModel() {
        // Observe metadata for preview
        viewModel.extractedMetadata.observe(viewLifecycleOwner) { metadata: ApkMetadata? ->
            if (metadata != null) {
                binding.layoutPreview.visibility = View.VISIBLE
                binding.tvPreviewPackage.text = "Package: ${metadata.packageName}"
                binding.tvPreviewVersionName.text = metadata.versionName
                binding.tvPreviewVersionCode.text = metadata.versionCode.toString()
                binding.tvApkDetails.text = "Metadata extraction successful"
            }
            else {
                binding.layoutPreview.visibility = View.GONE
            }
            updateButtonState()
        }

        // Observe upload progress
        viewModel.uploadProgress.observe(viewLifecycleOwner) { progress ->
            binding.pbUpload.progress = progress
            binding.tvProgressPercent.text = "$progress%"
        }

        viewModel.uploadState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UploadState.Idle -> {
                    binding.progressBar.visibility = View.GONE
                    binding.layoutUploadProgress.visibility = View.GONE
                    binding.btnCreateVersion.text = "Upload & Create Version"
                    setInputsEnabled(true)
                    updateButtonState(false)
                }
                is UploadState.Validating -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.layoutUploadProgress.visibility = View.GONE
                    binding.btnCreateVersion.text = "Validating APK..."
                    setInputsEnabled(false)
                    updateButtonState(true)
                }
                is UploadState.Uploading -> {
                    binding.progressBar.visibility = View.GONE
                    binding.layoutUploadProgress.visibility = View.VISIBLE
                    binding.btnCreateVersion.text = "Uploading..."
                    setInputsEnabled(false)
                    updateButtonState(true)
                }
                is UploadState.Confirming -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.layoutUploadProgress.visibility = View.GONE
                    binding.btnCreateVersion.text = "Finalizing..."
                    setInputsEnabled(false)
                    updateButtonState(true)
                }
                is UploadState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.layoutUploadProgress.visibility = View.GONE
                    setFragmentResult("create_version_request", bundleOf("refresh" to true))
                    
                    val display = "${state.version.versionName} (b${state.version.buildNumber})"
                    Snackbar.make(binding.root, "✓ Version '$display' created successfully!", Snackbar.LENGTH_LONG)
                        .setBackgroundTint(resources.getColor(android.R.color.holo_green_dark, null))
                        .show()
                    findNavController().popBackStack()
                }
                is UploadState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.layoutUploadProgress.visibility = View.GONE
                    binding.btnCreateVersion.text = "Upload & Create Version"
                    setInputsEnabled(true)
                    updateButtonState(false)

                    Snackbar.make(binding.root, "Error: ${state.message}", Snackbar.LENGTH_LONG)
                        .setBackgroundTint(resources.getColor(android.R.color.holo_red_dark, null))
                        .show()
                }
            }
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.etVersionTitle.isEnabled = enabled
        binding.etReleaseNotes.isEnabled = enabled
        binding.layoutUploadApk.isEnabled = enabled
        binding.layoutUploadApk.alpha = if (enabled) 1.0f else 0.5f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
