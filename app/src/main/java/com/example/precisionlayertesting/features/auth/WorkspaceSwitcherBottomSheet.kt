package com.example.precisionlayertesting.features.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.precisionlayertesting.core.di.ManualDI
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.databinding.BottomSheetWorkspaceSwitcherBinding
import com.example.precisionlayertesting.adapter.WorkspaceSwitcherAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class WorkspaceSwitcherBottomSheet(
    private val onCreateNew: () -> Unit,
    private val onSwitched: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetWorkspaceSwitcherBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(ManualDI.authRepository) as T
            }
        }
    }

    private lateinit var adapter: WorkspaceSwitcherAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetWorkspaceSwitcherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpList()
        observeWorkspaces()
        
        binding.btnCreateWorkspace.setOnClickListener {
            dismiss()
            onCreateNew()
        }
    }

    private fun setUpList() {
        adapter = WorkspaceSwitcherAdapter(
            emptyList(),
            ManualDI.prefsManager.getWorkspaceId()
        ) { selected ->
            viewModel.switchWorkspace(selected)
            onSwitched()
            dismiss()
        }
        binding.rvWorkspaces.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWorkspaces.adapter = adapter
    }

    private fun observeWorkspaces() {
        viewModel.detailedWorkspaces.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.rvWorkspaces.alpha = 0.5f
                }
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rvWorkspaces.alpha = 1.0f
                    adapter.updateList(result.data, ManualDI.prefsManager.getWorkspaceId())
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to load workspaces", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "WorkspaceSwitcherBottomSheet"
    }
}
