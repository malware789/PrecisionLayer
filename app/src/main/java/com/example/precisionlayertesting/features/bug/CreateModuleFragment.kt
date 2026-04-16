package com.example.precisionlayertesting.features.bug

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.precisionlayertesting.core.di.ManualDI
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.databinding.FragmentCreateModuleBinding
import com.google.android.material.snackbar.Snackbar

class CreateModuleFragment : Fragment() {

    private var _binding: FragmentCreateModuleBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CreateModuleViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateModuleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CreateModuleViewModel(ManualDI.bugRepository, ManualDI.prefsManager) as T
            }
        })[CreateModuleViewModel::class.java]

        setupInputWatcher()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupInputWatcher() {
        binding.etModuleName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasName = !s.isNullOrBlank()
                binding.btnCreateModule.isEnabled = hasName
                binding.btnCreateModule.alpha = if (hasName) 1.0f else 0.5f
                // Clear error on typing
                if (hasName) binding.tilModuleName.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupClickListeners() {
        binding.btnCreateModule.setOnClickListener {
            val name = binding.etModuleName.text.toString()
            val description = binding.etDescription.text.toString()

            if (name.isBlank()) {
                binding.tilModuleName.error = "Module name is required"
                binding.etModuleName.requestFocus()
                return@setOnClickListener
            }

            viewModel.createModule(name, description)
        }
    }

    private fun observeViewModel() {
        viewModel.createState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnCreateModule.isEnabled = false
                    binding.btnCreateModule.text = "Creating..."
                    binding.etModuleName.isEnabled = false
                    binding.etDescription.isEnabled = false
                }
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, "✓ Module '${result.data.name}' created!", Snackbar.LENGTH_LONG)
                        .setBackgroundTint(resources.getColor(android.R.color.holo_green_dark, null))
                        .show()
                    // Navigate back — Dashboard will refresh on resume
                    findNavController().popBackStack()
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnCreateModule.isEnabled = true
                    binding.btnCreateModule.alpha = 1.0f
                    binding.btnCreateModule.text = "Create Module"
                    binding.etModuleName.isEnabled = true
                    binding.etDescription.isEnabled = true
                    Snackbar.make(
                        binding.root,
                        "Error: ${result.exception.message}",
                        Snackbar.LENGTH_LONG
                    ).setBackgroundTint(resources.getColor(android.R.color.holo_red_dark, null))
                        .show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
