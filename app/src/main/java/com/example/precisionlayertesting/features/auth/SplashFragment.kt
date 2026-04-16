package com.example.precisionlayertesting.features.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.precisionlayertesting.MainActivity
import com.example.precisionlayertesting.R
import com.example.precisionlayertesting.core.di.ManualDI
import com.example.precisionlayertesting.databinding.FragmentSplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkSession()
    }

    private fun checkSession() {
        val prefs = ManualDI.prefsManager
        val token = prefs.getAccessToken()
        val userId = prefs.getUserId()
        val workspaceId = prefs.getWorkspaceId()

        lifecycleScope.launch {
            delay(1500) // Brief delay for branding

            if (token.isNullOrEmpty() || userId.isNullOrEmpty()) {
                // No session -> Start onboarding
                findNavController().navigate(R.id.action_splashFragment_to_getStartedFragment)
            } else {
                // Session exists
                if (workspaceId.isNullOrEmpty()) {
                    // Logged in but no workspace selected -> Choose Workspace
                    val action = SplashFragmentDirections.actionSplashFragmentToChooseWorkspaceTypeFragment(userId)
                    findNavController().navigate(action)
                } else {
                    // Fully onboarded -> Dashboard
                    startActivity(Intent(requireActivity(), MainActivity::class.java))
                    requireActivity().finish()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
