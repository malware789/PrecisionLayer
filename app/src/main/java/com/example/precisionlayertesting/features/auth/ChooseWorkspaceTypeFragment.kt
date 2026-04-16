package com.example.precisionlayertesting.features.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.precisionlayertesting.R
import com.example.precisionlayertesting.databinding.FragmentChooseWorkspaceTypeBinding

class ChooseWorkspaceTypeFragment : Fragment() {

    private var _binding: FragmentChooseWorkspaceTypeBinding? = null
    private val binding get() = _binding!!

    private val args: ChooseWorkspaceTypeFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChooseWorkspaceTypeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardJoinWorkspace.setOnClickListener {
            val action = ChooseWorkspaceTypeFragmentDirections.actionChooseWorkspaceTypeFragmentToJoinWorkspaceFragment(args.userId)
            findNavController().navigate(action)
        }

        binding.cardCreateWorkspace.setOnClickListener {
            val action = ChooseWorkspaceTypeFragmentDirections.actionChooseWorkspaceTypeFragmentToCreateWorkspaceFragment(args.userId)
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
