package com.example.precisionlayertesting

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.example.precisionlayertesting.core.di.ManualDI
import com.example.precisionlayertesting.databinding.ActivityMainBinding
import com.example.precisionlayertesting.features.auth.AuthActivity
import com.example.precisionlayertesting.features.auth.AuthViewModel
import com.example.precisionlayertesting.features.auth.WorkspaceSwitcherBottomSheet

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: AuthViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(ManualDI.authRepository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupDrawer()
        observeViewModel()

        // Initial Data Fetch
        val userId = ManualDI.prefsManager.getUserId()
        if (userId != null) {
            viewModel.fetchUserWorkspacesDetailed(userId)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.ivMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.toolbar.workspaceSwitcher.setOnClickListener {
            val bottomSheet = WorkspaceSwitcherBottomSheet(
                onCreateNew = {
                    // Navigate to Create Workspace screen (usually in Auth Graph, but we can host it or redirect)
                    // For now, let's just show a toast or handle navigation if possible
                    Toast.makeText(this, "Redirecting to Create Workspace...", Toast.LENGTH_SHORT).show()
                },
                onSwitched = {
                    // Reload Dashboard (re-attach current fragment or trigger refresh)
                    reloadDashboard()
                }
            )
            bottomSheet.show(supportFragmentManager, WorkspaceSwitcherBottomSheet.TAG)
        }
    }

    private fun setupDrawer() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    // Navigate to Dashboard
                }
                R.id.nav_profile -> {
                    // Navigate to Profile
                }
                R.id.nav_logout -> {
                    performLogout()
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun observeViewModel() {
        viewModel.currentWorkspaceName.observe(this) { name ->
            binding.toolbar.tvWorkspaceName.text = name ?: "Select Workspace"
        }
    }

    private fun reloadDashboard() {
        // Simple way to refresh: find the nav host and navigate to dashboard again or just refresh the active fragment
        // Or if Dashboard is a Fragment, tell it to refresh its data
        Toast.makeText(this, "Workspace Switched", Toast.LENGTH_SHORT).show()
    }

    private fun performLogout() {
        viewModel.logout()
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}