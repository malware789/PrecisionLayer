package com.example.precisionlayertesting.features.management

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.precisionlayertesting.core.repository.AuthRepository
import com.example.precisionlayertesting.core.utils.Result
import kotlinx.coroutines.launch

class InviteTeamMembersViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    private val _inviteState = MutableLiveData<Result<Unit>>()
    val inviteState: LiveData<Result<Unit>> = _inviteState

    fun sendInvitation(email: String, role: String = "developer") {
        _inviteState.value = Result.Loading
        viewModelScope.launch {
            val workspaceId = repository.getPrefs().getWorkspaceId()
            val userId = repository.getPrefs().getUserId()
            
            if (workspaceId == null || userId == null) {
                _inviteState.value = Result.Error(Exception("Missing workspace or user context"))
                return@launch
            }
            
            _inviteState.value = repository.createInvitation(
                workspaceId = workspaceId,
                email = email,
                role = role,
                invitedBy = userId
            )
        }
    }
}
