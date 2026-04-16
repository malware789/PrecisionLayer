package com.example.precisionlayertesting.features.bug

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.precisionlayertesting.core.utils.PrefsManager
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.data.models.bug.AppVersion
import com.example.precisionlayertesting.data.models.bug.AppVersionCreateRequest
import com.example.precisionlayertesting.data.repository.BugRepository
import kotlinx.coroutines.launch

class VersionViewModel(
    private val repository: BugRepository,
    private val prefsManager: PrefsManager
) : ViewModel() {

    companion object {
        private const val TAG = "VersionViewModel"
    }

    // List State
    private val _versions = MutableLiveData<Result<List<AppVersion>>>()
    val versions: LiveData<Result<List<AppVersion>>> = _versions

    // Creation State
    private val _createState = MutableLiveData<Result<AppVersion>>()
    val createState: LiveData<Result<AppVersion>> = _createState

    fun loadVersions(moduleId: String) {
        val workspaceId = prefsManager.getWorkspaceId() ?: return
        viewModelScope.launch {
            _versions.value = Result.Loading
            try {
                val data = repository.getVersions(moduleId, workspaceId)
                _versions.value = Result.Success(data)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load versions: ${e.message}")
                _versions.value = Result.Error(e)
            }
        }
    }

    fun createVersion(
        moduleId: String, 
        versionName: String,
        versionCode: Int,
        fileUrl: String? = null,
        releaseNotes: String? = null
    ) {
        val workspaceId = prefsManager.getWorkspaceId()
        val userId = prefsManager.getUserId() ?: "" // Assuming UserID is stored in Prefs
        if (workspaceId.isNullOrBlank()) {
            _createState.value = Result.Error(Exception("No workspace selected."))
            return
        }

        val trimmedName = versionName.trim()
        if (trimmedName.isEmpty()) {
            _createState.value = Result.Error(Exception("Version name cannot be empty."))
            return
        }

        viewModelScope.launch {
            _createState.value = Result.Loading
            val request = AppVersionCreateRequest(
                moduleId = moduleId,
                workspaceId = workspaceId,
                versionName = trimmedName,
                versionCode = versionCode,
                buildNumber = 0, // Will be calculated in repository
                releaseNotes = releaseNotes,
                fileUrl = fileUrl,
                createdBy = userId
            )
            Log.d(TAG, "Creating version with request: $request")
            val result = repository.createVersion(request)
            if (result is Result.Error) {
                Log.e(TAG, "Create version failed: ${result.exception.message}")
            }
            _createState.value = result
        }
    }
}
