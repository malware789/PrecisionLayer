package com.example.precisionlayertesting.features.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.precisionlayertesting.MainActivity
import com.example.precisionlayertesting.core.base.BaseActivity
import com.example.precisionlayertesting.core.di.ManualDI
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.core.models.auth.LoginRequest
import com.example.precisionlayertesting.databinding.FragmentLoginBinding

class LoginActivity : BaseActivity() {

    private lateinit var viewModel: AuthViewModel
    private lateinit var binding: FragmentLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        viewModel = AuthViewModel(ManualDI.authRepository)
        super.onCreate(savedInstanceState)
    }

    override fun setupViews() {
        binding = FragmentLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val pass = binding.etPassword.text.toString()
            if (email.isNotBlank() && pass.isNotBlank()) {
                viewModel.loginWithEmailPassword(LoginRequest(email, pass))
            }
        }
    }

    override fun observeViewModel() {
        viewModel.loginState.observe(this) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.btnLogin.text = ""
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.text = "Sign In"
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.text = "Sign In"
                    Toast.makeText(this, "Error: ${result.exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
