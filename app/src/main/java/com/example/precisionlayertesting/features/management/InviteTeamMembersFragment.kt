package com.example.precisionlayertesting.features.management

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.precisionlayertesting.core.di.ManualDI
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.databinding.FragmentInviteTeamMembersBinding

class InviteTeamMembersFragment : Fragment() {

    private var _binding: FragmentInviteTeamMembersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: InviteTeamMembersViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return InviteTeamMembersViewModel(ManualDI.authRepository) as T
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInviteTeamMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSendInvitation.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim()
            if (email.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please enter an email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Set role to developer by default or let user choose later.
            viewModel.sendInvitation(email, "developer")
        }

        viewModel.inviteState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.btnSendInvitation.isEnabled = false
                    binding.btnSendInvitation.text = "Sending..."
                }
                is Result.Success -> {
                    binding.btnSendInvitation.isEnabled = true
                    binding.btnSendInvitation.text = "Send Invitation"
                    binding.etEmail.text?.clear()
                    Toast.makeText(requireContext(), "Invitation sent successfully!", Toast.LENGTH_SHORT).show()
                }
                is Result.Error -> {
                    binding.btnSendInvitation.isEnabled = true
                    binding.btnSendInvitation.text = "Send Invitation"
                    Toast.makeText(requireContext(), result.exception.message ?: "Failed to send invitation", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
