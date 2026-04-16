package com.example.precisionlayertesting.data.models.auth

import com.google.gson.annotations.SerializedName

data class Invitation(
    @SerializedName("id") val id: String,
    @SerializedName("workspace_id") val workspaceId: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("status") val status: String,
    @SerializedName("invited_by") val invitedBy: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    // Join with workspaces table if possible, but for now we might need a separate call or a query
    @SerializedName("workspaces") val workspace: WorkspaceInfo? = null
)

data class WorkspaceInfo(
    @SerializedName("name") val name: String
)
