package com.example.precisionlayertesting.core.models.auth

import com.google.gson.annotations.SerializedName

data class WorkspaceMember(
    @SerializedName("workspace_id")
    val workspaceId: String,
    @SerializedName("role")
    val role: String,
    @SerializedName("user_id")
    val userId: String? = null
)
