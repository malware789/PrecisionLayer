package com.example.precisionlayertesting

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.precisionlayertesting.core.di.ManualDI
import com.example.precisionlayertesting.databinding.ActivityMainBinding
import com.example.precisionlayertesting.features.auth.AuthActivity
import com.example.precisionlayertesting.features.auth.AuthViewModel
import com.example.precisionlayertesting.features.auth.WorkspaceSwitcherBottomSheet

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val viewModel: AuthViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(ManualDI.authRepository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.mainNavHost) as NavHostFragment
        navController = navHostFragment.navController

        setupToolbar()
        setupDrawer()
        setupBottomNav()
        setupDestinationListener()
        observeViewModel()

        // Initial Data Fetch
        val userId = ManualDI.prefsManager.getUserId()
        if (userId != null) {
            viewModel.fetchUserWorkspacesDetailed(userId)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.ivMenu.setOnClickListener {
            if (isTopLevelDestination(navController.currentDestination?.id)) {
                binding.drawerLayout.openDrawer(GravityCompat.START)
            }
            else {
                onBackPressedDispatcher.onBackPressed()
            }
        }

        binding.toolbar.workspaceSwitcher.setOnClickListener {
            val bottomSheet = WorkspaceSwitcherBottomSheet(
                onCreateNew = {
                    Toast.makeText(this, "Redirecting to Create Workspace...", Toast.LENGTH_SHORT).show()
                },
                onSwitched = {
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
                    navController.navigate(R.id.dashboardFragment)
                }
                R.id.nav_modules -> {
                    navController.navigate(R.id.moduleVersionsFragment)
                }
                R.id.nav_bugs -> {
                    navController.navigate(R.id.bugTrackingFragment)
                }
                R.id.nav_logout -> {
                    performLogout()
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dashboardFragment -> {
                    navController.navigate(R.id.dashboardFragment)
                    true
                }
                R.id.moduleVersionsFragment -> {
                    navController.navigate(R.id.moduleVersionsFragment)
                    true
                }
                R.id.bugTrackingFragment -> {
                    navController.navigate(R.id.bugTrackingFragment)
                    true
                }

                else -> false
            }
        }
    }


    private fun setupDestinationListener() {
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            val config = getToolbarConfig(destination.id, arguments)
            applyToolbarConfig(config)
            syncBottomNavSelection(destination.id)
        }
    }

    private fun getToolbarConfig(destinationId: Int, args: Bundle?): ToolbarConfig {
        return when (destinationId) {
            R.id.dashboardFragment -> ToolbarConfig(
                title = "Dashboard",
                subtitle = "Overview",
                showBottomNav = true,
                showWorkspaceSwitcher = true,
                useDrawerIcon = true
            )

            R.id.moduleVersionsFragment -> ToolbarConfig(
                title = "Modules",
                subtitle = "Manage your apps",
                showBottomNav = true,
                showWorkspaceSwitcher = true,
                useDrawerIcon = true
            )

            R.id.bugTrackingFragment -> ToolbarConfig(
                title = "Bugs",
                subtitle = "Track reported issues",
                showBottomNav = true,
                showWorkspaceSwitcher = true,
                useDrawerIcon = true
            )



            R.id.createModuleFragment -> ToolbarConfig(
                title = "Create Module",
                subtitle = "Add a new module",
                showBottomNav = false,
                showWorkspaceSwitcher = false,
                useDrawerIcon = false
            )

            R.id.createVersionFragment -> ToolbarConfig(
                title = "Create Version",
                subtitle = "Upload a new build",
                showBottomNav = false,
                showWorkspaceSwitcher = false,
                useDrawerIcon = false
            )

            R.id.reportBugFormFragment -> ToolbarConfig(
                title = "Report Bug",
                subtitle = "Add issue details",
                showToolBar =false,
                showBottomNav = false,
                showWorkspaceSwitcher = false,
                useDrawerIcon = false
            )

            else -> ToolbarConfig(
                title = "PrecisionLayer",
                subtitle = null,
                showBottomNav = false,
                showWorkspaceSwitcher = false,
                useDrawerIcon = false
            )
        }
    }

    private fun applyToolbarConfig(config: ToolbarConfig) {
        binding.toolbar.toolbar.visibility = if (config.showToolBar) View.VISIBLE else View.GONE
        binding.toolbar.tvToolbarTitle.text = config.title
        binding.toolbar.tvToolbarSubtitle.text = config.subtitle ?: ""
        binding.toolbar.tvToolbarSubtitle.visibility = if (config.subtitle.isNullOrBlank()) View.GONE else View.VISIBLE

        binding.toolbar.workspaceSwitcher.visibility = if (config.showWorkspaceSwitcher) View.VISIBLE else View.GONE

        binding.bottomNav.visibility = if (config.showBottomNav) View.VISIBLE else View.GONE

        if (config.useDrawerIcon) {
            binding.toolbar.ivMenu.setImageResource(R.drawable.button_menu)
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        } else {
            binding.toolbar.ivMenu.setImageResource(R.drawable.ic_arrow_back)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        }
    }

    private fun syncBottomNavSelection(destinationId: Int) {
        when (destinationId) {
            R.id.dashboardFragment -> binding.bottomNav.selectedItemId = R.id.dashboardFragment
            R.id.moduleVersionsFragment -> binding.bottomNav.selectedItemId = R.id.moduleVersionsFragment
            R.id.bugTrackingFragment -> binding.bottomNav.selectedItemId = R.id.bugTrackingFragment
        }
    }

    private fun isTopLevelDestination(destinationId: Int?): Boolean {
        return destinationId in setOf(
            R.id.dashboardFragment,
            R.id.moduleVersionsFragment,
            R.id.bugTrackingFragment
        )
    }

    private fun observeViewModel() {
        viewModel.currentWorkspaceName.observe(this) { name ->
            binding.toolbar.tvWorkspaceName.text = name ?: "Select Workspace"
        }
    }

    private fun reloadDashboard() {
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
        }
        else {
            super.onBackPressed()
        }
    }
}

data class ToolbarConfig(
    val title: String,
    val subtitle: String?,
    val showToolBar: Boolean=true,
    val showBottomNav: Boolean,
    val showWorkspaceSwitcher: Boolean,
    val useDrawerIcon: Boolean
)