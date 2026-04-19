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
import com.example.precisionlayertesting.databinding.FragmentBugTrackingBinding
import com.example.precisionlayertesting.features.auth.ChooseWorkspaceTypeFragmentDirections

class BugTrackingFragment : Fragment() {

    private var _binding: FragmentBugTrackingBinding? = null
    private val binding get() = _binding!!
    
    private val args: BugTrackingFragmentArgs by navArgs()
    
    private val viewModel: TestingSessionViewModel by lazy {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                val workspaceId = ManualDI.prefsManager.getWorkspaceId() ?: ""
                @Suppress("UNCHECKED_CAST")
                return TestingSessionViewModel(ManualDI.bugRepository, args.versionId, workspaceId) as T
            }
        })[TestingSessionViewModel::class.java]
    }
    
    private lateinit var adapter: TestingSessionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBugTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        adapter = TestingSessionAdapter(emptyList()) { session ->
            val action = BugTrackingFragmentDirections.actionBugTrackingToBugReports(
                moduleId = args.moduleId,
                versionId = args.versionId,
                sessionId = session.id,
                moduleName = args.moduleName,
                versionName = args.versionName,
                sessionTitle = session.title
            )
            findNavController().navigate(action)
        }
        binding.rvTestingSessions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTestingSessions.adapter = adapter

        binding.fabAddBug.setOnClickListener {
            val action = BugTrackingFragmentDirections.actionBugTrackingFragmentToReportBugFormFragment(
                moduleId = args.moduleId,
                versionId = args.versionId,
                moduleName = args.moduleName,
                versionName = args.versionName
            )
            findNavController().navigate(action)
        }
    }

    private fun observeViewModel() {
        viewModel.sessions.observe(viewLifecycleOwner) { result ->
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
