package com.example.precisionlayertesting.data.remote

import com.example.precisionlayertesting.data.models.auth.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {
    @POST("auth/v1/token?grant_type=password")
    suspend fun loginWithEmailPassword(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("auth/v1/signup")
    suspend fun signUp(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @POST("rest/v1/profiles")
    suspend fun createProfile(
        @Body profile: ProfileRequest,
        @Header("Prefer") prefer: String = "resolution=ignore-duplicates"
    ): Response<Unit>

    @PATCH("rest/v1/profiles")
    suspend fun updateProfile(
        @Query("id") idFilter: String,
        @Body profileUpdates: Map<String, String>
    ): Response<Unit>

    @GET("rest/v1/workspace_members")
    suspend fun getUserWorkspaces(
        @Query("user_id") filter: String,
        @Query("select") select: String = "*"
    ): List<WorkspaceMember>

    @GET("rest/v1/invitations")
    suspend fun getPendingInvitations(
        @Query("email") emailFilter: String,
        @Query("status") statusFilter: String = "eq.pending",
        @Query("select") select: String = "*,workspaces(name)"
    ): List<Invitation>

    @PATCH("rest/v1/invitations")
    suspend fun updateInvitationStatus(
        @Query("id") idFilter: String,
        @Body statusUpdate: Map<String, String>
    ): Response<Unit>

    @POST("rest/v1/workspace_members")
    suspend fun addWorkspaceMember(
        @Body member: Map<String, String>
    ): Response<Unit>

    @POST("rest/v1/workspaces")
    @Headers("Prefer: return=representation")
    suspend fun createWorkspace(
        @Body workspace: Workspace
    ): Response<List<Workspace>>
    @GET("rest/v1/workspace_members")
    suspend fun getUserWorkspacesDetailed(
        @Query("user_id") filter: String,
        @Query("select") select: String = "*,workspaces(*)"
    ): List<WorkspaceMemberDetailed>

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshToken(
        @Body body: Map<String, String>
    ): retrofit2.Response<LoginResponse>
}
