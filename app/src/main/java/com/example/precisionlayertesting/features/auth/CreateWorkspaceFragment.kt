package com.example.precisionlayertesting.features.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.precisionlayertesting.MainActivity
import com.example.precisionlayertesting.core.di.ManualDI
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.databinding.FragmentCreateWorkspaceBinding

class CreateWorkspaceFragment : Fragment() {
    private var _binding: FragmentCreateWorkspaceBinding? = null
    private val binding get() = _binding!!

    private val args: CreateWorkspaceFragmentArgs by navArgs()

    private val viewModel: AuthViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(ManualDI.authRepository) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateWorkspaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpClicks()
        observeViewModel()
    }

    private fun setUpClicks() {
        binding.btnCreateWorkspace.setOnClickListener {
            val workspaceName = binding.etCompanyName.text.toString().trim()
            val userName = binding.etAdminName.text.toString().trim()

            if (workspaceName.isEmpty()) {
                binding.etCompanyName.error = "Workspace name required"
                return@setOnClickListener
            }

            if (userName.isEmpty()) {
                binding.etAdminName.error = "Your name required"
                return@setOnClickListener
            }

            viewModel.createWorkspace(workspaceName, userName, args.userId)
        }
    }

    private fun observeViewModel() {
        viewModel.createWorkspaceState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    setLoading(true)
                }
                is Result.Success -> {
                    setLoading(false)
                    Toast.makeText(requireContext(), "Workspace created successfully", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                }
                is Result.Error -> {
                    setLoading(false)
                    Toast.makeText(requireContext(), result.exception.message ?: "Failed to create workspace", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnCreateWorkspace.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnCreateWorkspace.text = if (isLoading) "" else "Create Workspace"
    }

    private fun navigateToMain() {
        startActivity(Intent(requireActivity(), MainActivity::class.java))
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
