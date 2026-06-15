package com.example.precisionlayertesting.features.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import android.content.Intent
import com.example.precisionlayertesting.MainActivity
import com.example.precisionlayertesting.R
import com.example.precisionlayertesting.core.di.ManualDI
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.databinding.FragmentJoinWorkspaceBinding

class JoinWorkspaceFragment : Fragment() {

    companion object {
        private const val TAG = "JoinWorkspaceFragment"
    }

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

    // Populated from SharedPreferences via ViewModel — never hardcoded
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

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        // Load the real authenticated session from SharedPreferences
        viewModel.loadCurrentSession()
    }

    private fun setupRecyclerView() {
        adapter = InvitationAdapter(
            onAccept = { invitation ->
                val userId = currentUserId
                if (userId != null) {
                    viewModel.acceptInvitation(userId, invitation)
                } else {
                    Log.e(TAG, "Accept attempted but userId is null — session not loaded yet")
                    Toast.makeText(requireContext(), "Session error. Please re-login.", Toast.LENGTH_SHORT).show()
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
        // Observe the session — triggers invitation fetch once real email is available
        viewModel.currentSession.observe(viewLifecycleOwner) { (userId, email) ->
            currentUserId = userId
            currentUserEmail = email

            if (email != null) {
                Log.d(TAG, "Authenticated session loaded — email: $email, userId: $userId")
                viewModel.fetchPendingInvitations(email)
            } else {
                Log.w(TAG, "Session email is null — user may not be logged in or session was cleared")
                Toast.makeText(
                    requireContext(),
                    "Session not found. Please re-login.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        viewModel.invitationsState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.emptyStateLayout.visibility = View.GONE
                }
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val list = result.data
                    Log.d(TAG, "Invitations fetched — email: $currentUserEmail, count: ${list.size}")
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
                    Log.e(TAG, "Failed to fetch invitations: ${result.exception.message}")
                    Toast.makeText(
                        requireContext(),
                        "Error: ${result.exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        viewModel.invitationActionState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val action = result.data
                    Toast.makeText(requireContext(), "Success!", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Invitation action success: $action")
                    
                    if (action == "ACCEPT") {
                        currentUserId?.let { viewModel.fetchUserWorkspacesDetailed(it) }
                    }
                    // Always refresh pending invitations list
                    currentUserEmail?.let { viewModel.fetchPendingInvitations(it) }
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Action failed: ${result.exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        viewModel.detailedWorkspaces.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> { }
                is Result.Success -> {
                    val list = result.data
                    Log.d(TAG, "Workspace list count: ${list.size}")
                    if (list.isNotEmpty()) {
                        binding.btnCreateWorkspace.visibility = View.GONE
                        binding.tvRedirecting.visibility = View.VISIBLE
                        Log.d(TAG, "User belongs to workspace, redirecting to MainActivity")
                        
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        requireActivity().finish()
                    } else {
                        binding.btnCreateWorkspace.visibility = View.VISIBLE
                        binding.tvRedirecting.visibility = View.GONE
                    }
                }
                is Result.Error -> {
                    Log.e(TAG, "Failed to fetch detailed workspaces: ${result.exception.message}")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
