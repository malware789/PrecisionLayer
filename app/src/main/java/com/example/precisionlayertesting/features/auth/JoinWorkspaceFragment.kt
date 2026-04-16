package com.example.precisionlayertesting.features.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.precisionlayertesting.R
import com.example.precisionlayertesting.core.di.ManualDI
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.data.models.auth.Invitation
import com.example.precisionlayertesting.databinding.FragmentJoinWorkspaceBinding

class JoinWorkspaceFragment : Fragment() {

    private var _binding: FragmentJoinWorkspaceBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(ManualDI.authRepository) as T
            }
        }
    }

    private lateinit var adapter: InvitationAdapter
    private var currentUserEmail: String? = null
    private var currentUserId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJoinWorkspaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // In a real app, we'd get this from a session/preference
        // For now, we'll try to get it from the last login result or a mock if needed
        // Assuming session is handled globally in DI or shared preferences
        // Mocking for now to demonstrate the flow
        currentUserEmail = "user@example.com" 
        currentUserId = "63d76b1f-7f5b-4b11-9a7e-4b68ff5f9e9a"

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        currentUserEmail?.let { viewModel.fetchPendingInvitations(it) }
    }

    private fun setupRecyclerView() {
        adapter = InvitationAdapter(
            onAccept = { invitation ->
                currentUserId?.let { userId ->
                    viewModel.acceptInvitation(userId, invitation)
                }
            },
            onReject = { invitation ->
                viewModel.rejectInvitation(invitation.id)
            }
        )
        binding.rvInvitations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@JoinWorkspaceFragment.adapter
        }
    }

    private fun setupListeners() {
        binding.btnCreateWorkspace.setOnClickListener {
            findNavController().navigate(R.id.action_joinWorkspaceFragment_to_createWorkspaceFragment)
        }
    }

    private fun observeViewModel() {
        viewModel.invitationsState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.emptyStateLayout.visibility = View.GONE
                }
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val list = result.data
                    adapter.submitList(list)
                    if (list.isEmpty()) {
                        binding.emptyStateLayout.visibility = View.VISIBLE
                        binding.rvInvitations.visibility = View.GONE
                    } else {
                        binding.emptyStateLayout.visibility = View.GONE
                        binding.rvInvitations.visibility = View.VISIBLE
                    }
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.invitationActionState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Success!", Toast.LENGTH_SHORT).show()
                    // Refresh list
                    currentUserEmail?.let { viewModel.fetchPendingInvitations(it) }
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Action failed: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
