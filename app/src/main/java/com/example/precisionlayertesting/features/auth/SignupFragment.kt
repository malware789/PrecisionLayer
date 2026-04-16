package com.example.precisionlayertesting.features.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.precisionlayertesting.R
import com.example.precisionlayertesting.core.di.ManualDI
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.data.models.auth.LoginRequest
import com.example.precisionlayertesting.databinding.FragmentSignupBinding

class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

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
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpClicks()
        observer()
    }

    private fun setUpClicks() {
        binding.btnSignUp.setOnClickListener {
            validateAndSignUp()
        }

        binding.tvLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun validateAndSignUp() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (email.isEmpty()) {
            binding.etEmail.error = "Email required"
            return
        }

        if (password.isEmpty()) {
            binding.etPassword.error = "Password required"
            return
        }

        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            return
        }

        viewModel.signUp(LoginRequest(email, password))
    }

    private fun observer() {
        viewModel.signUpState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> setLoading(true)
                is Result.Success -> {
                    setLoading(false)
                    val accessToken = result.data.accessToken
                    val userId = result.data.user.id
                    
                    if (!accessToken.isNullOrEmpty()) {
                        // Email verification OFF -> User is logged in
                        Toast.makeText(requireContext(), "Account created successfully", Toast.LENGTH_SHORT).show()
                        val action = SignupFragmentDirections.actionSignupFragmentToChooseWorkspaceTypeFragment(userId)
                        findNavController().navigate(action)
                    } else {
                        // Email verification ON -> Must verify and login
                        Toast.makeText(requireContext(), "Account created. Please verify your email before login.", Toast.LENGTH_LONG).show()
                        findNavController().navigate(R.id.action_signupFragment_to_loginFragment)
                    }
                }
                is Result.Error -> {
                    setLoading(false)
                    val errorMsg = result.exception.message ?: "Signup failed"
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnSignUp.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSignUp.text = if (isLoading) "" else "Sign Up"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
