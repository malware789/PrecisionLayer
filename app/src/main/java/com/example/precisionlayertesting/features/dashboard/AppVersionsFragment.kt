package com.example.precisionlayertesting.features.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.precisionlayertesting.core.di.ManualDI
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.databinding.FragmentAppVersionBinding
import com.example.precisionlayertesting.features.bug.VersionViewModel

class AppVersionsFragment : Fragment() {

    private var _binding: FragmentAppVersionBinding? = null
    private val binding get() = _binding!!

    private val args: AppVersionsFragmentArgs by navArgs()
    
    private val viewModel: VersionViewModel by lazy {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return VersionViewModel(ManualDI.bugRepository, ManualDI.prefsManager) as T
            }
        })[VersionViewModel::class.java]
    }

    private lateinit var adapter: AppVersionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppVersionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupFragmentResultListener()
        observeViewModel()
        
        // Initial load
        viewModel.loadVersions(args.moduleId)
    }

    private fun setupUI() {
        adapter = AppVersionAdapter(emptyList()) { version ->
            val display = "${version.versionName} (b${version.buildNumber})"
            val action = AppVersionsFragmentDirections.actionAppVersionToBugTracking(
                moduleId = args.moduleId,
                versionId = version.id,
                moduleName = args.moduleName,
                versionName = display
            )
            findNavController().navigate(action)
        }
        
        binding.rvVersions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVersions.adapter = adapter

        binding.fabUpload.setOnClickListener {
            val action = AppVersionsFragmentDirections.actionModuleVersionsToCreateVersion(args.moduleId)
            findNavController().navigate(action)
        }
    }

    private fun setupFragmentResultListener() {
        // Production-ready refresh handling using Fragment Result API
        setFragmentResultListener("create_version_request") { _, bundle ->
            val refresh = bundle.getBoolean("refresh", false)
            if (refresh) {
                viewModel.loadVersions(args.moduleId)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.versions.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    // Could show a progress bar here
                }
                is Result.Success -> {
                    adapter.updateData(result.data)
                    if (result.data.isEmpty()) {
                        Toast.makeText(requireContext(), "No versions found for this module", Toast.LENGTH_SHORT).show()
                    }
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(), "Error: ${result.exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
