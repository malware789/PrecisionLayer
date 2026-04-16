package com.example.precisionlayertesting.data.models.auth

import com.google.gson.annotations.SerializedName

data class Workspace(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String = "organization",
    @SerializedName("created_by") val createdBy: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)
