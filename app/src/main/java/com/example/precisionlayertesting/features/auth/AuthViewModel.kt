package com.example.precisionlayertesting.features.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.data.models.auth.*
import com.example.precisionlayertesting.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _loginState = MutableLiveData<Result<LoginResponse>>()
    val loginState: LiveData<Result<LoginResponse>> = _loginState

    private val _workspaceState = MutableLiveData<Result<List<WorkspaceMember>>>()
    val workspaceState: LiveData<Result<List<WorkspaceMember>>> = _workspaceState

    private val _signUpState = MutableLiveData<Result<LoginResponse>>()
    val signUpState: LiveData<Result<LoginResponse>> = _signUpState

    private val _invitationsState = MutableLiveData<Result<List<Invitation>>>()
    val invitationsState: LiveData<Result<List<Invitation>>> = _invitationsState

    private val _invitationActionState = MutableLiveData<Result<Unit>>()
    val invitationActionState: LiveData<Result<Unit>> = _invitationActionState

    private val _createWorkspaceState = MutableLiveData<Result<Unit>>()
    val createWorkspaceState: LiveData<Result<Unit>> = _createWorkspaceState

    // Workspace Switcher State
    private val _detailedWorkspaces = MutableLiveData<Result<List<WorkspaceMemberDetailed>>>()
    val detailedWorkspaces: LiveData<Result<List<WorkspaceMemberDetailed>>> = _detailedWorkspaces

    private val _currentWorkspaceName = MutableLiveData<String?>()
    val currentWorkspaceName: LiveData<String?> = _currentWorkspaceName

    fun loginWithEmailPassword(request: LoginRequest) {
        _loginState.value = Result.Loading
        viewModelScope.launch {
            _loginState.value = repository.login(request)
        }
    }

    fun checkUserWorkspace(userId: String) {
        _workspaceState.value = Result.Loading
        viewModelScope.launch {
            _workspaceState.value = repository.getUserWorkspaces(userId)
        }
    }

    fun fetchUserWorkspacesDetailed(userId: String) {
        _detailedWorkspaces.value = Result.Loading
        viewModelScope.launch {
            try {
                val list = repository.getDetailedWorkspaces(userId)
                _detailedWorkspaces.value = Result.Success(list)
                handleWorkspaceResolution(list)
            } catch (e: Exception) {
                _detailedWorkspaces.value = Result.Error(e)
            }
        }
    }

    private fun handleWorkspaceResolution(workspaces: List<WorkspaceMemberDetailed>) {
        val savedId = repository.getPrefs().getWorkspaceId()
        
        if (workspaces.isEmpty()) {
            _currentWorkspaceName.value = null
            return
        }

        val currentWS = workspaces.find { it.workspaceId == savedId }
        if (currentWS != null) {
            _currentWorkspaceName.value = currentWS.workspace.name
            repository.getPrefs().setWorkspaceName(currentWS.workspace.name)
        } else {
            // Auto-select first if none saved or saved one was deleted/removed
            val first = workspaces.first()
            repository.getPrefs().saveWorkspaceId(first.workspaceId)
            repository.getPrefs().setWorkspaceName(first.workspace.name)
            _currentWorkspaceName.value = first.workspace.name
        }
    }

    fun switchWorkspace(workspace: WorkspaceMemberDetailed) {
        repository.getPrefs().saveWorkspaceId(workspace.workspaceId)
        repository.getPrefs().setWorkspaceName(workspace.workspace.name)
        _currentWorkspaceName.value = workspace.workspace.name
    }

    fun signUp(request: LoginRequest) {
        _signUpState.value = Result.Loading
        viewModelScope.launch {
            _signUpState.value = repository.signUp(request)
        }
    }

    fun fetchPendingInvitations(email: String) {
        _invitationsState.value = Result.Loading
        viewModelScope.launch {
            _invitationsState.value = repository.getPendingInvitations(email)
        }
    }

    fun acceptInvitation(userId: String, invitation: Invitation) {
        _invitationActionState.value = Result.Loading
        viewModelScope.launch {
            _invitationActionState.value = repository.acceptInvitation(userId, invitation)
        }
    }

    fun rejectInvitation(invitationId: String) {
        _invitationActionState.value = Result.Loading
        viewModelScope.launch {
            _invitationActionState.value = repository.rejectInvitation(invitationId)
        }
    }

    fun createWorkspace(workspaceName: String, userName: String, userId: String) {
        _createWorkspaceState.value = Result.Loading
        viewModelScope.launch {
            _createWorkspaceState.value = repository.createWorkspace(workspaceName, userName, userId)
        }
    }
    
    fun logout() {
        repository.getPrefs().clearAll()
    }
}
