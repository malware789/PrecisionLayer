package com.example.precisionlayertesting.core.models.auth

import com.google.gson.annotations.SerializedName

data class Invitation(
    @SerializedName("id") val id: String,
    @SerializedName("workspace_id") val workspaceId: String,
    @SerializedName("email") val email: String,
    @SerializedName("role") val role: String,
    @SerializedName("status") val status: String,
    @SerializedName("invited_by") val invitedBy: String,
    @SerializedName("accepted_by") val acceptedBy: String? = null,
    @SerializedName("accepted_at") val acceptedAt: String? = null,
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("workspaces") val workspace: WorkspaceInfo? = null
)

data class WorkspaceInfo(
    @SerializedName("name") val name: String
)
