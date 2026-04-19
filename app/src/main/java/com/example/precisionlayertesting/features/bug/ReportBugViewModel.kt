package com.example.precisionlayertesting.features.bug

import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.data.models.bug.*
import com.example.precisionlayertesting.data.repository.BugRepository
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class ReportBugViewModel(
    private val repository: BugRepository,
    private val savedStateHandle: SavedStateHandle,
    private val workspaceId: String,
    private val moduleId: String,
    private val versionId: String
) : ViewModel() {

    companion object {
        private const val KEY_DRAFTS = "draft_bugs"
    }

    // Persist drafts via SavedStateHandle
    val draftBugs: LiveData<List<BugDraft>> = savedStateHandle.getLiveData(KEY_DRAFTS, emptyList())

    private val _submissionState = MutableLiveData<SubmissionState>(SubmissionState.Idle)
    val submissionState: LiveData<SubmissionState> = _submissionState

    private val _modules = MutableLiveData<List<Module>>()
    val modules: LiveData<List<Module>> = _modules

    private var isSubmitting = false

    init {
        fetchModules()
    }

    private fun fetchModules() {
        viewModelScope.launch {
            val result = repository.getModules(workspaceId)
            _modules.value = result
        }
    }

    fun addBugToDraft(
        title: String,
        component: String,
        severity: String,
        description: String,
        steps: String?,
        screenshotUri: Uri?
    ) {
        val currentList = draftBugs.value.orEmpty().toMutableList()
        currentList.add(
            BugDraft(
                title = title,
                component = component,
                severity = severity,
                description = description,
                steps = steps,
                screenshotUri = screenshotUri
            )
        )
        savedStateHandle[KEY_DRAFTS] = currentList
    }

    fun updateDraftAt(
        index: Int,
        title: String,
        component: String,
        severity: String,
        description: String,
        steps: String?,
        screenshotUri: Uri?
    ) {
        val currentList = draftBugs.value.orEmpty().toMutableList()
        if (index in currentList.indices) {
            currentList[index] = BugDraft(
                title = title,
                component = component,
                severity = severity,
                description = description,
                steps = steps,
                screenshotUri = screenshotUri
            )
            savedStateHandle[KEY_DRAFTS] = currentList
        }
    }

    fun removeDraftAt(index: Int) {
        val currentList = draftBugs.value.orEmpty().toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            savedStateHandle[KEY_DRAFTS] = currentList
        }
    }

    fun submitAllBugs(userId: String, sessionId: String? = null) {
        if (isSubmitting) return
        
        val bugs = draftBugs.value.orEmpty()
        if (bugs.isEmpty()) return

        isSubmitting = true
        viewModelScope.launch {
            _submissionState.value = SubmissionState.Submitting

            try {
                // 1. Determine Session
                val finalSessionId = if (sessionId != null) {
                    sessionId
                } else {
                    val sessionRequest = TestingSessionCreateRequest(
                        workspaceId = workspaceId,
                        versionId = versionId,
                        userId = userId,
                        title = "Bug Report Session - ${System.currentTimeMillis() / 1000}"
                    )
                    val sessionResult = repository.createTestingSession(sessionRequest)
                    if (sessionResult is Result.Success) {
                        sessionResult.data.id
                    } else {
                        throw Exception((sessionResult as Result.Error).exception.message ?: "Failed to create session")
                    }
                }

                // 2. Prepare Bug Reports
                // TODO: Upload screenshots to R2 here and get paths
                val bugRequests = bugs.map { draft ->
                    BugReportCreateRequest(
                        workspaceId = workspaceId,
                        sessionId = finalSessionId,
                        reporterId = userId,
                        title = draft.title,
                        severity = draft.severity,
                        description = draft.description,
                        stepsToRepro = draft.steps,
                        component = draft.component,
                        imagePath = null // Placeholder for R2 path
                    )
                }

                // 3. Submit Batch
                val result = repository.submitBugReports(bugRequests)
                if (result is Result.Success) {
                    _submissionState.value = SubmissionState.Success
                    savedStateHandle[KEY_DRAFTS] = emptyList<BugDraft>()
                } else {
                    throw Exception((result as Result.Error).exception.message ?: "Batch submission failed")
                }
            } catch (e: Exception) {
                _submissionState.value = SubmissionState.Error(e.message ?: "Submission failed")
            } finally {
                isSubmitting = false
            }
        }
    }

    @Parcelize
    data class BugDraft(
        val title: String,
        val component: String,
        val severity: String,
        val description: String,
        val steps: String?,
        val screenshotUri: Uri?
    ) : Parcelable

    sealed class SubmissionState {
        object Idle : SubmissionState()
        object Submitting : SubmissionState()
        object Success : SubmissionState()
        data class Error(val message: String) : SubmissionState()
    }
}
