package com.example.precisionlayertesting.features.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.precisionlayertesting.core.base.BaseViewModel
import com.example.precisionlayertesting.core.utils.PrefsManager
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.core.models.bugModel.Module
import com.example.precisionlayertesting.core.repository.BugRepository
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val bugRepository: BugRepository,
    private val prefsManager: PrefsManager
) : BaseViewModel() {

    private val _modules = MutableLiveData<Result<List<Module>>>()
    val modules: LiveData<Result<List<Module>>> = _modules

    val isDeveloperRole = true // Can be fetched from membership later

    init {
        loadModules()
    }

    fun loadModules() {
        val workspaceId = prefsManager.getWorkspaceId() ?: return
        viewModelScope.launch {
            _modules.value = Result.Loading
            try {
                val data = bugRepository.getModules(workspaceId)
                _modules.value = Result.Success(data)
            } catch (e: Exception) {
                _modules.value = Result.Error(e)
            }
        }
    }

    /** Alias used by onResume to force a data refresh. */
    fun fetchModules() = loadModules()
}
