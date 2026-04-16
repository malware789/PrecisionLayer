package com.example.precisionlayertesting.data.models.bug

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Module(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("workspace_id") val workspaceId: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
) : Parcelable

data class ModuleCreateRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("workspace_id") val workspaceId: String
)

data class AppVersionCreateRequest(
    @SerializedName("module_id") val moduleId: String,
    @SerializedName("workspace_id") val workspaceId: String,
    @SerializedName("version_name") val versionName: String,
    @SerializedName("version_code") val versionCode: Int,
    @SerializedName("build_number") val buildNumber: Int,
    @SerializedName("release_notes") val releaseNotes: String? = null,
    @SerializedName("file_url") val fileUrl: String? = null,
    @SerializedName("created_by") val createdBy: String
)

@Parcelize
data class AppVersion(
    @SerializedName("id") val id: String,
    @SerializedName("module_id") val moduleId: String,
    @SerializedName("workspace_id") val workspaceId: String,
    @SerializedName("version_name") val versionName: String,
    @SerializedName("version_code") val versionCode: Int,
    @SerializedName("build_number") val buildNumber: Int,
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
    @SerializedName("bug_reports") val bugReports: List<BugReportCount>? = null
) : Parcelable {
    val bugCount: Int
        get() = bugReports?.firstOrNull()?.count ?: 0
}

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
