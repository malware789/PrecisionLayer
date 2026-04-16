package com.example.precisionlayertesting.features.bug

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.precisionlayertesting.core.base.BaseViewModel
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.data.models.bug.TestingSession
import com.example.precisionlayertesting.data.repository.BugRepository
import kotlinx.coroutines.launch

class TestingSessionViewModel(
    private val bugRepository: BugRepository,
    private val versionId: String,
    private val workspaceId: String
) : BaseViewModel() {

    private val _sessions = MutableLiveData<Result<List<TestingSession>>>()
    val sessions: LiveData<Result<List<TestingSession>>> = _sessions

    init {
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            _sessions.value = Result.Loading
            try {
                val data = bugRepository.getBugGroups(versionId, workspaceId)
                _sessions.value = Result.Success(data)
            } catch (e: Exception) {
                _sessions.value = Result.Error(e)
            }
        }
    }
}
