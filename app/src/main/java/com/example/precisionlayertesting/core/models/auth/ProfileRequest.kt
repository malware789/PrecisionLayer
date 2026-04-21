package com.example.precisionlayertesting.core.models.auth

import com.google.gson.annotations.SerializedName

data class ProfileRequest(
    val id: String,
    val email: String
)
