package com.example.precisionlayertesting.features.management

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.precisionlayertesting.R
import com.example.precisionlayertesting.databinding.FragmentRolesPermissionsBinding

class RolesPermissionsFragment : Fragment() {

    private var _binding: FragmentRolesPermissionsBinding? = null
    private val binding get() = _binding!!

    /** Currently selected role card view ID */
    private var selectedRoleId: Int = R.id.chipRoleAdmin

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRolesPermissionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set Admin selected by default
        highlightRole(R.id.chipRoleAdmin)
        setupReadonlyPermissions(isAdmin = true)

        // Role card click listeners
        binding.chipRoleAdmin.setOnClickListener {
            highlightRole(R.id.chipRoleAdmin)
            setupReadonlyPermissions(isAdmin = true)
        }
        binding.chipRoleManager.setOnClickListener {
            highlightRole(R.id.chipRoleManager)
            setupReadonlyPermissions(isAdmin = false)
        }
        binding.chipRoleDeveloper.setOnClickListener {
            highlightRole(R.id.chipRoleDeveloper)
            setupReadonlyPermissions(isAdmin = false)
        }
        binding.chipRoleTester.setOnClickListener {
            highlightRole(R.id.chipRoleTester)
            setupReadonlyPermissions(isAdmin = false)
        }
    }

    /** Applies primary-filled background to selected role, clears others. */
    private fun highlightRole(roleViewId: Int) {
        selectedRoleId = roleViewId
        val selected = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_role_selected)
        val unselected = null // default transparent

        listOf(
            binding.chipRoleAdmin,
            binding.chipRoleManager,
            binding.chipRoleDeveloper,
            binding.chipRoleTester
        ).forEach { card ->
            card.background = if (card.id == roleViewId) selected else unselected
        }
    }

    private fun setupReadonlyPermissions(isAdmin: Boolean) {
        // All switches are disabled in Phase 1 (read-only display)
        binding.swInviteUsers.isChecked = isAdmin
        binding.swInviteUsers.isEnabled = false

        binding.swManageMembers.isChecked = isAdmin
        binding.swManageMembers.isEnabled = false

        binding.swAssignTesters.isChecked = isAdmin
        binding.swAssignTesters.isEnabled = false

        binding.swUploadBuilds.isChecked = true
        binding.swUploadBuilds.isEnabled = false

        binding.swDeleteBuilds.isChecked = isAdmin
        binding.swDeleteBuilds.isEnabled = false

        binding.swManageModules.isChecked = isAdmin
        binding.swManageModules.isEnabled = false

        binding.swViewAnalytics.isChecked = true
        binding.swViewAnalytics.isEnabled = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
