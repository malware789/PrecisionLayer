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

import com.example.precisionlayertesting.core.utils.ApkMetadataExtractor
import com.example.precisionlayertesting.core.utils.ApkMetadata
import com.example.precisionlayertesting.data.models.bug.ApkValidationRequest
import com.example.precisionlayertesting.data.models.bug.ConfirmUploadRequest
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class UploadState {
    object Idle : UploadState()
    object Validating : UploadState()
    object Uploading : UploadState()
    object Confirming : UploadState()
    data class Success(val version: AppVersion) : UploadState()
    data class Error(val message: String) : UploadState()
}

class VersionViewModel(
    private val repository: BugRepository,
    private val prefsManager: PrefsManager,
    private val apkExtractor: ApkMetadataExtractor
) : ViewModel() {

    companion object {
        private const val TAG = "VersionViewModel"
    }

    // List State
    private val _versions = MutableLiveData<Result<List<AppVersion>>>()
    val versions: LiveData<Result<List<AppVersion>>> = _versions

    // Upload & Creation State
    private val _uploadState = MutableLiveData<UploadState>(UploadState.Idle)
    val uploadState: LiveData<UploadState> = _uploadState

    private val _extractedMetadata = MutableLiveData<ApkMetadata?>(null)
    val extractedMetadata: LiveData<ApkMetadata?> = _extractedMetadata

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

    // Progress State
    private val _uploadProgress = MutableLiveData<Int>(0)
    val uploadProgress: LiveData<Int> = _uploadProgress

    fun extractMetadata(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val metadata = apkExtractor.extract(uri)
            _extractedMetadata.postValue(metadata)
        }
    }

    fun uploadApkAndCreateVersion(
        uri: Uri,
        moduleId: String,
        versionTitle: String,
        releaseNotes: String?
    ) {
        if (_uploadState.value != UploadState.Idle) return

        val workspaceId = prefsManager.getWorkspaceId() ?: run {
            _uploadState.value = UploadState.Error("No workspace selected.")
            return
        }
        val userId = prefsManager.getUserId() ?: ""
        val context = prefsManager.getContext()

        viewModelScope.launch {
            _uploadState.value = UploadState.Validating
            
            // 1. Get File Size and basic checks
            val fileSize = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }

            if (fileSize == 0L) {
                _uploadState.value = UploadState.Error("Could not read APK file or it is empty.")
                return@launch
            }

            if (fileSize > 150 * 1024 * 1024) {
                _uploadState.value = UploadState.Error("APK exceeds 150MB limit.")
                return@launch
            }

            // 2. Ensure Metadata is extracted
            val metadata = _extractedMetadata.value ?: withContext(Dispatchers.IO) { apkExtractor.extract(uri) }
            
            if (metadata == null) {
                _uploadState.value = UploadState.Error("Failed to extract APK metadata.")
                return@launch
            }

            // 3. Server Validation & Signed URL
            val validationRequest = ApkValidationRequest(
                moduleId = moduleId,
                versionName = metadata.versionName,
                versionCode = metadata.versionCode,
                packageName = metadata.packageName
            )
            
            val validationResult = repository.validateApk(validationRequest)
            if (validationResult is Result.Error) {
                _uploadState.value = UploadState.Error(validationResult.exception.message ?: "Validation failed")
                return@launch
            }
            
            val validationResponse = (validationResult as Result.Success).data
            val uploadUrl = validationResponse.uploadUrl ?: run {
                _uploadState.value = UploadState.Error("Server did not provide an upload URL.")
                return@launch
            }
            val filePath = validationResponse.filePath ?: ""

            // 4. Binary Upload to R2 (with progress and retry)
            _uploadState.value = UploadState.Uploading
            _uploadProgress.value = 0
            
            val apkData: ByteArray? = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } catch (e: Exception) {
                    null
                }
            }
            
            if (apkData == null) {
                _uploadState.value = UploadState.Error("Failed to read APK data.")
                return@launch
            }

            val uploadResult = repository.uploadToR2(uploadUrl, apkData) { uploaded, total ->
                val progress = ((uploaded.toDouble() / total.toDouble()) * 100).toInt()
                _uploadProgress.postValue(progress)
            }

            if (uploadResult is Result.Error) {
                _uploadState.value = UploadState.Error(uploadResult.exception.message ?: "Upload failed after retries")
                return@launch
            }

            // 5. Confirm & Create Version Entry
            _uploadState.value = UploadState.Confirming
            val confirmRequest = ConfirmUploadRequest(
                moduleId = moduleId,
                versionName = metadata.versionName,
                versionCode = metadata.versionCode,
                packageName = metadata.packageName,
                filePath = filePath,
                fileSize = fileSize,
                fileType = "apk",
                versionTitle = versionTitle,
                releaseNotes = releaseNotes
            )

            val confirmResult = repository.confirmUpload(confirmRequest)
            if (confirmResult is Result.Success) {
                _uploadState.value = UploadState.Success(confirmResult.data)
            }
            else {
                val error = (confirmResult as Result.Error).exception.message ?: "Confirmation failed"
                _uploadState.value = UploadState.Error(error)
            }
        }
    }

    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }
}
