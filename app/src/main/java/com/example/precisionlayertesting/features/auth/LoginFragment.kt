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
import com.example.precisionlayertesting.R
import com.example.precisionlayertesting.MainActivity
import com.example.precisionlayertesting.core.di.ManualDI
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.data.models.auth.LoginRequest
import com.example.precisionlayertesting.data.models.auth.User
import com.example.precisionlayertesting.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(ManualDI.authRepository) as T
            }
        }
    }

    private var currentUser: User? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpClicks()
        observer()
    }

    private fun setUpClicks() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.etEmail.error = "Email required"
                return@setOnClickListener
            }

            if (pass.isEmpty()) {
                binding.etPassword.error = "Password required"
                return@setOnClickListener
            }

            viewModel.loginWithEmailPassword(LoginRequest(email, pass))
        }

        binding.tvSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }
    }

    private fun observer(){
        viewModel.loginState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    setLoading(true)
                }

                is Result.Success -> {
                    currentUser = result.data.user
                    handlePostLogin(result.data.user)
                }

                is Result.Error -> {
                    setLoading(false)
                    Toast.makeText(
                        requireContext(),
                        result.exception.message ?: "Login failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        viewModel.workspaceState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    setLoading(true)
                }
                is Result.Success -> {
                    setLoading(false)
                    val workspaces = result.data

                    if (workspaces.isEmpty()) {
                        // No workspace → navigate to Choose Type screen with user ID
                        val userId = currentUser?.id ?: ""
                        val action = LoginFragmentDirections.actionLoginFragmentToChooseWorkspaceTypeFragment(userId)
                        findNavController().navigate(action)
                    } else {
                        // Has workspace → open MainActivity
                        startActivity(Intent(requireActivity(), MainActivity::class.java))
                        requireActivity().finish()
                    }
                }

                is Result.Error -> {
                    setLoading(false)
                    Toast.makeText(
                        requireContext(),
                        result.exception.message ?: "Error checking workspace",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {}
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnLogin.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.text = if (isLoading) "" else "Sign In"
    }

    private fun handlePostLogin(user: User) {
        viewModel.checkUserWorkspace(user.id)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
