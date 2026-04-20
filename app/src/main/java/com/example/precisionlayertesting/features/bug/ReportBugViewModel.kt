package com.example.precisionlayertesting.features.bug

import android.net.Uri
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.data.models.bug.*
import com.example.precisionlayertesting.data.repository.BugRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.parcelize.Parcelize
import java.util.UUID
import java.io.File

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

    private val _isCurrentAttachmentUploading = MutableLiveData<Boolean>(false)
    val isCurrentAttachmentUploading: LiveData<Boolean> = _isCurrentAttachmentUploading

    // Current form attachment state
    private val _currentScreenshotPath = MutableLiveData<String?>(null)
    private val _currentCachedUri = MutableLiveData<Uri?>(null)
    private val _currentMimeType = MutableLiveData<String?>(null)
    private val _uploadError = MutableLiveData<String?>(null)

    val currentScreenshotPath: LiveData<String?> = _currentScreenshotPath
    val currentCachedUri: LiveData<Uri?> = _currentCachedUri
    val uploadError: LiveData<String?> = _uploadError

    private var currentUploadRequestId: String? = null
    private var isSubmitting = false

    // Derived: prevents global submit if any draft is still (locally) processing OR final submission is active
    val isAnyUploadInProgress: LiveData<Boolean> = androidx.lifecycle.MediatorLiveData<Boolean>().apply {
        value = false
        addSource(draftBugs) { drafts ->
            value = drafts.any { it.isUploading } || _isCurrentAttachmentUploading.value == true || _submissionState.value == SubmissionState.Submitting
        }
        addSource(_isCurrentAttachmentUploading) { curUploading ->
            value = draftBugs.value.orEmpty().any { it.isUploading } || curUploading == true || _submissionState.value == SubmissionState.Submitting
        }
        addSource(_submissionState) { state ->
            value = draftBugs.value.orEmpty().any { it.isUploading } || _isCurrentAttachmentUploading.value == true || state == SubmissionState.Submitting
        }
    }

    init {
        fetchModules()
    }

    private fun fetchModules() {
        viewModelScope.launch {
            val result = repository.getModules(workspaceId)
            _modules.value = result
        }
    }

    /**
     * Handles local caching and compression.
     * No network calls are made here.
     */
    fun onScreenshotSelected(context: android.content.Context, uri: Uri) {
        val requestId = UUID.randomUUID().toString()
        currentUploadRequestId = requestId
        
        // Use this flag to show local processing progress
        _isCurrentAttachmentUploading.value = true
        _uploadError.value = null
        _currentScreenshotPath.value = null

        viewModelScope.launch {
            try {
                // 1. Resilience & Compression (Local Only)
                val cacheResult = repository.cacheAndCompressImage(context, uri)
                if (cacheResult is Result.Success) {
                    if (currentUploadRequestId == requestId) {
                        val (cachedUri, mimeType) = cacheResult.data
                        _currentCachedUri.value = cachedUri
                        _currentMimeType.value = mimeType
                    }
                } else {
                    throw (cacheResult as Result.Error).exception
                }
            } catch (e: Exception) {
                if (currentUploadRequestId == requestId) {
                    _uploadError.value = "Failed to process image: ${e.message}"
                    _currentCachedUri.value = null
                    _currentMimeType.value = null
                }
            } finally {
                if (currentUploadRequestId == requestId) {
                    _isCurrentAttachmentUploading.value = false
                }
            }
        }
    }

    fun removeCurrentScreenshot() {
        // Delete local cache
        _currentCachedUri.value?.path?.let { path ->
            File(path).delete()
        }

        currentUploadRequestId = null
        _currentScreenshotPath.value = null
        _currentCachedUri.value = null
        _currentMimeType.value = null
        _isCurrentAttachmentUploading.value = false
        _uploadError.value = null
    }

    fun clearAttachment() {
        currentUploadRequestId = null
        _currentScreenshotPath.value = null
        _currentCachedUri.value = null
        _currentMimeType.value = null
        _isCurrentAttachmentUploading.value = false
    }

    fun addBugToDraft(
        title: String,
        component: String,
        severity: String,
        description: String,
        steps: String?
    ) {
        val currentList = draftBugs.value.orEmpty().toMutableList()
        currentList.add(
            BugDraft(
                title = title,
                component = component,
                severity = severity,
                description = description,
                steps = steps,
                cachedUri = _currentCachedUri.value,
                imagePath = null, // Will be populated during final submit
                mimeType = _currentMimeType.value
            )
        )
        savedStateHandle[KEY_DRAFTS] = currentList
        clearAttachment()
    }

    fun updateDraftAt(
        index: Int,
        title: String,
        component: String,
        severity: String,
        description: String,
        steps: String?
    ) {
        val currentList = draftBugs.value.orEmpty().toMutableList()
        if (index in currentList.indices) {
            val oldDraft = currentList[index]
            currentList[index] = oldDraft.copy(
                title = title,
                component = component,
                severity = severity,
                description = description,
                steps = steps,
                cachedUri = _currentCachedUri.value ?: oldDraft.cachedUri,
                mimeType = _currentMimeType.value ?: oldDraft.mimeType
            )
            savedStateHandle[KEY_DRAFTS] = currentList
            clearAttachment()
        }
    }

    fun removeDraftAt(index: Int) {
        val currentList = draftBugs.value.orEmpty().toMutableList()
        if (index in currentList.indices) {
            // Delete local cache
            currentList[index].cachedUri?.path?.let { path -> File(path).delete() }
            currentList.removeAt(index)
            savedStateHandle[KEY_DRAFTS] = currentList
        }
    }

    fun submitAllBugs(context: android.content.Context, userId: String, sessionId: String? = null) {
        if (isSubmitting) return
        
        val bugs = draftBugs.value.orEmpty().toMutableList()
        if (bugs.isEmpty()) return

        isSubmitting = true
        viewModelScope.launch {
            try {
                _submissionState.value = SubmissionState.Submitting

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
                        throw Exception("Failed to create testing session")
                    }
                }

                // 2. Parallel Optimized Uploads
                val draftsToUpload = bugs.filter { it.cachedUri != null && it.imagePath == null }
                if (draftsToUpload.isNotEmpty()) {
                    // 2a. Batch Prepare Signed URLs
                    val batchRequest = ScreenshotBatchPrepareRequest(
                        workspaceId = workspaceId,
                        files = draftsToUpload.map { d ->
                            FileRequest(
                                draftId = d.id,
                                extension = d.mimeType?.substringAfter("/") ?: "jpeg",
                                mimeType = d.mimeType ?: "image/jpeg"
                            )
                        }
                    )
                    
                    val prepResult = repository.prepareScreenshotUploadBatch(batchRequest)
                    if (prepResult !is Result.Success) throw Exception("Failed to prepare storage URLs")
                    
                    val uploadInfos = prepResult.data.uploads
                    val infoMap = uploadInfos.associateBy { it.draftId }

                    // 2b. Parallel Streaming Uploads (Concurrency limit: 3)
                    val semaphore = Semaphore(3)
                    val uploadTasks = draftsToUpload.map { draft ->
                        async {
                            semaphore.withPermit {
                                val info = infoMap[draft.id] ?: throw Exception("Missing upload info for bug")
                                
                                val uploadResult = repository.uploadToR2Stream(
                                    url = info.uploadUrl,
                                    context = context,
                                    uri = draft.cachedUri!!,
                                    mimeType = draft.mimeType ?: "image/jpeg",
                                    onProgress = { _, _ -> }
                                )
                                
                                if (uploadResult is Result.Success) {
                                    info.filePath
                                } else {
                                    throw (uploadResult as Result.Error).exception
                                }
                            }
                        }
                    }

                    val remotePaths = uploadTasks.awaitAll()
                    
                    // 2c. Update local list with remote paths
                    draftsToUpload.forEachIndexed { index, draft ->
                        val listIndex = bugs.indexOfFirst { it.id == draft.id }
                        if (listIndex != -1) {
                            bugs[listIndex] = draft.copy(imagePath = remotePaths[index])
                        }
                    }
                    // Sync with SavedState
                    savedStateHandle[KEY_DRAFTS] = bugs.toList()
                }

                // 3. Final Batch Submission
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
                        imagePath = draft.imagePath
                    )
                }

                val result = repository.submitBugReports(bugRequests)
                if (result is Result.Success) {
                    // Final success cleanup
                    bugs.forEach { draft -> 
                        draft.cachedUri?.path?.let { path -> File(path).delete() }
                    }

                    _submissionState.value = SubmissionState.Success
                    savedStateHandle[KEY_DRAFTS] = emptyList<BugDraft>()
                }
                else {
                    throw Exception("Batch report insertion failed")
                }

            } catch (e: Exception) {
                Log.e("ReportBugViewModel", "Submission error", e)
                _submissionState.value = SubmissionState.Error(e.message ?: "Submission failed")
            }
            finally {
                isSubmitting = false
            }
        }
    }

    sealed class SubmissionState {
        object Idle : SubmissionState()
        object Submitting : SubmissionState()
        object Success : SubmissionState()
        data class Error(val message: String) : SubmissionState()
    }
}
