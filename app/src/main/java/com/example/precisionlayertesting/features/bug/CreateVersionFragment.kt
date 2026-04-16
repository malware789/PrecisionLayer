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
import com.example.precisionlayertesting.databinding.FragmentCreateVersionBinding
import com.google.android.material.snackbar.Snackbar

class CreateVersionFragment : Fragment() {

    private var _binding: FragmentCreateVersionBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: VersionViewModel
    private val args: CreateVersionFragmentArgs by navArgs()

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
                return VersionViewModel(ManualDI.bugRepository, ManualDI.prefsManager) as T
            }
        })[VersionViewModel::class.java]

        // Prefetch to check for duplicates
        viewModel.loadVersions(args.moduleId)

        setupInputWatcher()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupInputWatcher() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasName = !binding.etVersionName.text.isNullOrBlank()
                val hasCode = !binding.etVersionCode.text.isNullOrBlank()
                
                binding.btnCreateVersion.isEnabled = hasName && hasCode
                binding.btnCreateVersion.alpha = if (hasName && hasCode) 1.0f else 0.5f
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        
        binding.etVersionName.addTextChangedListener(watcher)
        binding.etVersionCode.addTextChangedListener(watcher)
    }

    private fun setupClickListeners() {
        binding.btnCreateVersion.setOnClickListener {
            val versionName = binding.etVersionName.text.toString()
            val versionCodeStr = binding.etVersionCode.text.toString()
            val releaseNotes = binding.etReleaseNotes.text.toString()
            
            val versionCode = versionCodeStr.toIntOrNull() ?: 1
            
            viewModel.createVersion(
                moduleId = args.moduleId,
                versionName = versionName,
                versionCode = versionCode,
                releaseNotes = releaseNotes
            )
        }
        
        binding.layoutUploadApk.setOnClickListener {
            Snackbar.make(binding.root, "APK selection will be implemented in a future update.", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.createState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnCreateVersion.isEnabled = false
                    binding.btnCreateVersion.text = "Creating..."
                    setInputsEnabled(false)
                }
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    setFragmentResult("create_version_request", bundleOf("refresh" to true))
                    
                    val display = "${result.data.versionName} (b${result.data.buildNumber})"
                    Snackbar.make(binding.root, "✓ Version '$display' created!", Snackbar.LENGTH_LONG)
                        .setBackgroundTint(resources.getColor(android.R.color.holo_green_dark, null))
                        .show()
                    
                    findNavController().popBackStack()
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCreateVersion.isEnabled = true
                    binding.btnCreateVersion.alpha = 1.0f
                    binding.btnCreateVersion.text = "Create Version"
                    setInputsEnabled(true)
                    
                    val message = result.exception.message ?: "Unknown error"
                    Snackbar.make(binding.root, "Error: $message", Snackbar.LENGTH_LONG)
                        .setBackgroundTint(resources.getColor(android.R.color.holo_red_dark, null))
                        .show()
                }
            }
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.etVersionName.isEnabled = enabled
        binding.etVersionCode.isEnabled = enabled
        binding.etReleaseNotes.isEnabled = enabled
        binding.layoutUploadApk.isEnabled = enabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
