package com.example.precisionlayertesting.features.bug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.precisionlayertesting.core.di.ManualDI
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.databinding.FragmentBugListBinding
import com.example.precisionlayertesting.features.bug.adapter.BugReportAdapter

class BugListFragment : Fragment() {

    private var _binding: FragmentBugListBinding? = null
    private val binding get() = _binding!!

    private val args: BugListFragmentArgs by navArgs()

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
        _binding = FragmentBugListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handleOnBackPressed()
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.tvHeaderTitle.text = "Bugs for ${args.testerName}"
        binding.tvHeaderAvatar.text = args.testerName.getInitials()
        
        adapter = BugReportAdapter(emptyList()) { bug ->
            val breadcrumb = "${args.moduleName} > ${args.versionName} > ${args.sessionTitle}"
            val action = BugListFragmentDirections.actionBugReportsToBugDetails(
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
                    val bugs = result.data
                    adapter.updateData(bugs)
                    
                    // Update header counts
                    val total = bugs.size
                    val openCount = bugs.count { it.status.equals("Open", ignoreCase = true) }
                    val resolvedCount = bugs.count { it.status.equals("Closed", ignoreCase = true) }
                    
                    binding.tvHeaderSubtitle.text = "$total total reports • $openCount open • $resolvedCount resolved"
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(), result.exception.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun String.getInitials(): String {
        return this.split(" ").filter { it.isNotEmpty() }.take(2)
            .map { it[0].uppercaseChar() }.joinToString("")
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
