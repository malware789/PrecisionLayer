package com.example.precisionlayertesting.data.models.bug

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Module(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("package_name") val packageName: String,
    @SerializedName("workspace_id") val workspaceId: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
) : Parcelable

data class ModuleCreateRequest(
    @SerializedName("name") val name: String,
    @SerializedName("package_name") val packageName: String,
    @SerializedName("description") val description: String?,
    @SerializedName("workspace_id") val workspaceId: String
)

data class AppVersionCreateRequest(
    @SerializedName("module_id") val moduleId: String,
    @SerializedName("workspace_id") val workspaceId: String,
    @SerializedName("version_name") val versionName: String,
    @SerializedName("version_code") val versionCode: Int,
    @SerializedName("build_number") val buildNumber: Int,
    @SerializedName("version_title") val versionTitle: String,
    @SerializedName("release_notes") val releaseNotes: String? = null,
    @SerializedName("file_url") val fileUrl: String? = null,
    @SerializedName("created_by") val createdBy: String
)

data class ApkValidationRequest(
    @SerializedName("module_id") val moduleId: String,
    @SerializedName("version_name") val versionName: String,
    @SerializedName("version_code") val versionCode: Int,
    @SerializedName("package_name") val packageName: String
)

data class ApkValidationResponse(
    @SerializedName("valid") val valid: Boolean,
    @SerializedName("upload_url") val uploadUrl: String? = null,
    @SerializedName("file_path") val filePath: String? = null
)

data class ConfirmUploadRequest(
    @SerializedName("module_id") val moduleId: String,
    @SerializedName("version_name") val versionName: String,
    @SerializedName("version_code") val versionCode: Int,
    @SerializedName("package_name") val packageName: String,
    @SerializedName("file_path") val filePath: String,
    @SerializedName("file_size") val fileSize: Long,
    @SerializedName("file_type") val fileType: String = "apk",
    @SerializedName("version_title") val versionTitle: String,
    @SerializedName("release_notes") val releaseNotes: String? = null
)

@Parcelize
data class AppVersion(
    @SerializedName("id") val id: String,
    @SerializedName("module_id") val moduleId: String,
    @SerializedName("workspace_id") val workspaceId: String,
    @SerializedName("version_name") val versionName: String,
    @SerializedName("version_code") val versionCode: Int,
    @SerializedName("build_number") val buildNumber: Int,
    @SerializedName("version_title") val versionTitle: String,
    @SerializedName("release_notes") val releaseNotes: String? = null,
    @SerializedName("file_url") val fileUrl: String? = null,
    @SerializedName("created_by") val createdBy: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
) : Parcelable

@Parcelize
data class TestingSession(
    @SerializedName("id") val id: String,
    @SerializedName("version_id") val versionId: String,
    @SerializedName("workspace_id") val workspaceId: String,
    @SerializedName("title") val title: String,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("profiles") val userProfile: UserProfile? = null,
    @SerializedName("bug_reports") val bugReports: List<BugReportCount>? = null
) : Parcelable {
    val bugCount: Int get() = bugReports?.firstOrNull()?.count ?: 0
}

@Parcelize
data class UserProfile(
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("email") val email: String? = null
) : Parcelable

@Parcelize
data class BugReportCount(
    @SerializedName("count") val count: Int
) : Parcelable

@Parcelize
data class BugReport(
    @SerializedName("id") val id: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("workspace_id") val workspaceId: String,
    @SerializedName("title") val title: String,
    @SerializedName("severity") val severity: String, // Low, Medium, High, Critical
    @SerializedName("status") val status: String,     // Open, In Progress, Closed
    @SerializedName("description") val description: String? = null,
    @SerializedName("steps_to_repro") val stepsToRepro: String? = null,
    @SerializedName("component") val component: String? = null,
    @SerializedName("image_path") val imagePath: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
) : Parcelable {
    companion object {
        const val SEVERITY_LOW = "Low"
        const val SEVERITY_MEDIUM = "Medium"
        const val SEVERITY_HIGH = "High"
        const val SEVERITY_CRITICAL = "Critical"

        const val STATUS_OPEN = "Open"
        const val STATUS_IN_PROGRESS = "In Progress"
        const val STATUS_CLOSED = "Closed"
    }
}

data class BugReportCreateRequest(
    @SerializedName("workspace_id") val workspaceId: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("reporter_id") val reporterId: String,
    @SerializedName("title") val title: String,
    @SerializedName("severity") val severity: String,
    @SerializedName("description") val description: String,
    @SerializedName("steps_to_repro") val stepsToRepro: String? = null,
    @SerializedName("component") val component: String? = null,
    @SerializedName("image_path") val imagePath: String? = null
)

data class TestingSessionCreateRequest(
    @SerializedName("workspace_id") val workspaceId: String,
    @SerializedName("version_id") val versionId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("title") val title: String,
    @SerializedName("status") val status: String = "Active"
)

// --- Bug Screenshot Upload Models ---

data class ScreenshotUploadRequest(
    @SerializedName("workspace_id") val workspaceId: String,
    @SerializedName("extension") val extension: String,
    @SerializedName("mime_type") val mimeType: String? = null
)

data class ScreenshotUploadResponse(
    @SerializedName("upload_url") val uploadUrl: String,
    @SerializedName("file_path") val filePath: String
)

data class DeleteFileRequest(
    @SerializedName("workspace_id") val workspaceId: String,
    @SerializedName("file_path") val filePath: String
)

@Parcelize
data class BugDraft(
    val title: String,
    val component: String,
    val severity: String,
    val description: String,
    val steps: String?,
    val cachedUri: android.net.Uri? = null,
    val imagePath: String? = null,
    val mimeType: String? = null,
    val isUploading: Boolean = false,
    val uploadRequestId: String? = null,
    val uploadError: String? = null
) : Parcelable
