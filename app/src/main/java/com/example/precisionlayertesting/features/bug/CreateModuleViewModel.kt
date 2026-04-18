package com.example.precisionlayertesting.features.bug

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.precisionlayertesting.core.utils.PrefsManager
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.data.models.bug.Module
import com.example.precisionlayertesting.data.models.bug.ModuleCreateRequest
import com.example.precisionlayertesting.data.repository.BugRepository
import android.util.Log
import kotlinx.coroutines.launch

class CreateModuleViewModel(
    private val repository: BugRepository,
    private val prefsManager: PrefsManager
) : ViewModel() {

    companion object {
        private const val TAG = "CreateModuleViewModel"
    }

    private val _createState = MutableLiveData<Result<Module>>()
    val createState: LiveData<Result<Module>> = _createState

    fun createModule(name: String, packageName: String, description: String) {
        val workspaceId = prefsManager.getWorkspaceId()
        if (workspaceId.isNullOrBlank()) {
            _createState.value = Result.Error(Exception("No workspace selected. Please select a workspace first."))
            return
        }

        viewModelScope.launch {
            _createState.value = Result.Loading
            val request = ModuleCreateRequest(
                name = name.trim(),
                packageName = packageName.trim(),
                description = description.trim().takeIf { it.isNotEmpty() },
                workspaceId = workspaceId
            )
            Log.d(TAG, "Creating module with request: $request")
            val result = repository.createModule(request)
            if (result is Result.Error) {
                Log.e(TAG, "Create module failed: ${result.exception.message}")
            }
            _createState.value = result
        }
    }
}
