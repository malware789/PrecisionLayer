package com.example.precisionlayertesting.core.models.dashboard

data class AppVersion(
    val versionNumber: String,
    val buildTitle: String,
    val statusBadge: String,
    val date: String,
    val team: String,
    val verifiedStatus: String,
    val description: String,
    val tags: List<String>,
    val bugCount: Int,
    val isLatest: Boolean = false
)
