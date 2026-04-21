package com.example.precisionlayertesting.features.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.precisionlayertesting.R
import com.example.precisionlayertesting.core.di.ManualDI
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DashboardViewModel
    private lateinit var adapter: ModuleAdapter
    private var lastBackPressedTime: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DashboardViewModel(ManualDI.bugRepository, ManualDI.prefsManager) as T
            }
        })[DashboardViewModel::class.java]

        handleOnBackPressed()
        setupRecyclerView()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Reload modules every time we return to this screen (e.g. after module creation)
        viewModel.fetchModules()
    }

    private fun setupRecyclerView() {
        adapter = ModuleAdapter(emptyList())
        adapter.onModuleClick = { module ->
            val action = DashboardFragmentDirections.actionDashboardToModuleVersions(
                moduleId = module.id,
                moduleName = module.name
            )
            findNavController().navigate(action)
        }
        binding.recyclerViewModules.adapter = adapter

        // FAB now navigates to CreateModuleFragment
        binding.fabUpload.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_createModule)
        }
    }

    private fun observeViewModel() {
        viewModel.modules.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    // Optionally show shimmer or loading state
                }
                is Result.Success -> {
                    adapter.updateData(result.data)
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(), "Error: ${result.exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.fabUpload.visibility = View.VISIBLE
    }
    private fun handleOnBackPressed() {
        // Correct syntax for the modern Back Press API
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (System.currentTimeMillis() - lastBackPressedTime < 2000) {
                // Disable this callback so the next call triggers the activity's default behavior
                isEnabled = false
                requireActivity().onBackPressed()
            }
            else {
                lastBackPressedTime = System.currentTimeMillis()
                Toast.makeText(requireContext(), "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
