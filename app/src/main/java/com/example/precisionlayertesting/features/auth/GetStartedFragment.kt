package com.example.precisionlayertesting.features.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.precisionlayertesting.R
import com.example.precisionlayertesting.databinding.FragmentGetStartedBinding

class GetStartedFragment : Fragment() {

    private var _binding: FragmentGetStartedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGetStartedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnOrganization.setOnClickListener {
            val bundle = Bundle().apply { putBoolean("isFromIndividual", false) }
            findNavController().navigate(R.id.action_getStarted_to_login, bundle)
        }

        binding.btnIndividual.setOnClickListener {
            val bundle = Bundle().apply { putBoolean("isFromIndividual", true) }
            findNavController().navigate(R.id.action_getStarted_to_login, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
