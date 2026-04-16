package com.example.precisionlayertesting.features.bug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.precisionlayertesting.core.di.ManualDI
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.databinding.FragmentBugReportsBinding

class BugReportsFragment : Fragment() {

    private var _binding: FragmentBugReportsBinding? = null
    private val binding get() = _binding!!

    private val args: BugReportsFragmentArgs by navArgs()

    private val viewModel: BugViewModel by lazy {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val workspaceId = ManualDI.prefsManager.getWorkspaceId() ?: ""
                @Suppress("UNCHECKED_CAST")
                return BugViewModel(ManualDI.bugRepository, args.sessionId, workspaceId) as T
            }
        })[BugViewModel::class.java]
    }

    private lateinit var adapter: BugReportAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBugReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        adapter = BugReportAdapter(emptyList()) { bug ->
            val breadcrumb = "${args.moduleName} > ${args.versionName} > ${args.sessionTitle}"
            val action = BugReportsFragmentDirections.actionBugReportsToBugDetails(
                bugDetail = bug,
                breadcrumb = breadcrumb
            )
            findNavController().navigate(action)
        }
        
        binding.rvBugReports.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBugReports.adapter = adapter
        
        binding.fabAddBug.setOnClickListener {
             Toast.makeText(requireContext(), "Coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.bugs.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> { }
                is Result.Success -> {
                    adapter.updateData(result.data)
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(), result.exception.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
