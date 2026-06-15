package com.example.precisionlayertesting.core.repository

import com.example.precisionlayertesting.core.models.auth.Invitation
import com.example.precisionlayertesting.core.models.auth.LoginRequest
import com.example.precisionlayertesting.core.models.auth.LoginResponse
import com.example.precisionlayertesting.core.models.auth.ProfileRequest
import com.example.precisionlayertesting.core.models.auth.Workspace
import com.example.precisionlayertesting.core.models.auth.WorkspaceMember
import com.example.precisionlayertesting.core.models.auth.WorkspaceMemberDetailed
import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.core.utils.PrefsManager
import com.example.precisionlayertesting.core.remote.AuthApiService

class AuthRepository(
    private val authApiService: AuthApiService,
    private val prefsManager: PrefsManager
) {
    
    suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return try {
            val response = authApiService.loginWithEmailPassword(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    prefsManager.saveUserId(body.user.id)
                    prefsManager.saveUserEmail(body.user.email)
                    prefsManager.saveAccessToken(body.accessToken)
                    prefsManager.saveRefreshToken(body.refreshToken)
                    Result.Success(body)
                } else {
                    Result.Error(Exception("Empty login response payload"))
                }
            } else {
                Result.Error(Exception("Login failed: Error ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun signUp(request: LoginRequest): Result<LoginResponse> {
        return try {
            val response = authApiService.signUp(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    prefsManager.saveUserId(body.user.id)
                    prefsManager.saveUserEmail(body.user.email)
                    prefsManager.saveAccessToken(body.accessToken)
                    prefsManager.saveRefreshToken(body.refreshToken)
                    createProfile(ProfileRequest(id = body.user.id, email = body.user.email))
                    Result.Success(body)
                } else {
                    Result.Error(Exception("Empty signup response payload"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                val msg = if (errorBody.contains("User already registered", ignoreCase = true)) {
                    "Account already exists. Please login"
                } else {
                    "Signup failed: ${response.code()} - ${response.message()}"
                }
                Result.Error(Exception(msg))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun createProfile(profile: ProfileRequest): Result<Unit> {
        return try {
            val response = authApiService.createProfile(profile)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Profile creation failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getUserWorkspaces(userId: String): Result<List<WorkspaceMember>> {
        return try {
            val list = authApiService.getUserWorkspaces("eq.$userId")
            Result.Success(list)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getPendingInvitations(email: String): Result<List<Invitation>> {
        return try {
            val list = authApiService.getPendingInvitations("eq.$email")
            Result.Success(list)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun acceptInvitation(userId: String, invitation: Invitation): Result<Unit> {
        return try {
            val response = authApiService.acceptInvitationEdgeFunction(
                mapOf("invitation_id" to invitation.id)
            )
            if (response.isSuccessful) {
                // Read workspace_id from response and save it
                val body = response.body()
                val workspaceId = body?.get("workspace_id") as? String
                if (workspaceId != null) {
                    prefsManager.saveWorkspaceId(workspaceId)
                }
                Result.Success(Unit)
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                val msg = if (errorBody.contains("already_member")) {
                    "You are already a member of this workspace"
                } else if (errorBody.contains("expired")) {
                    "This invitation has expired"
                } else {
                    "Failed to accept invitation: ${response.code()}"
                }
                Result.Error(Exception(msg))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun rejectInvitation(invitationId: String): Result<Unit> {
        return try {
            val response = authApiService.updateInvitationStatus(
                "eq.$invitationId",
                mapOf("status" to "revoked", "updated_at" to java.time.Instant.now().toString())
            )
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Failed to reject invitation"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun createInvitation(workspaceId: String, email: String, role: String, invitedBy: String): Result<Unit> {
        return try {
            val response = authApiService.createInvitation(
                mapOf(
                    "workspace_id" to workspaceId,
                    "email" to email,
                    "role" to role,
                    "invited_by" to invitedBy
                )
            )
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(Exception("Failed to send invitation"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun createWorkspace(
        workspaceName: String,
        userName: String,
        userId: String
    ): Result<Unit> {
        return try {
            // STEP 1: Create workspace
            val wsResponse = authApiService.createWorkspace(
                Workspace(
                    name = workspaceName,
                    createdBy = userId
                )
            )
            if (wsResponse.isSuccessful) {
                val workspaceId = wsResponse.body()?.firstOrNull()?.id
                if (workspaceId != null) {
                    // STEP 2: Add user as admin
                    val memberResponse = authApiService.addWorkspaceMember(mapOf(
                        "user_id" to userId,
                        "workspace_id" to workspaceId,
                        "role" to "admin"
                    ))
                    
                    if (memberResponse.isSuccessful) {
                        // STEP 3: Update profile full_name
                        val profileResponse = authApiService.updateProfile(
                            "eq.$userId",
                            mapOf("full_name" to userName)
                        )
                        if (profileResponse.isSuccessful) {
                            prefsManager.saveWorkspaceId(workspaceId)
                            Result.Success(Unit)
                        } else {
                            Result.Error(Exception("Failed to update profile name"))
                        }
                    } else {
                        Result.Error(Exception("Failed to assign admin role"))
                    }
                } else {
                    Result.Error(Exception("Workspace created but ID not returned"))
                }
            } else {
                // Parse error body for more detail
                val errorMsg = wsResponse.errorBody()?.string() ?: "Failed to create workspace: ${wsResponse.code()}"
                Result.Error(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    suspend fun getDetailedWorkspaces(userId: String): List<WorkspaceMemberDetailed> {
        return authApiService.getUserWorkspacesDetailed("eq.$userId")
    }

    /**
     * Returns the currently authenticated user's id and email from the local session.
     * Returns null for either field if the session has not been established yet.
     */
    fun getCurrentSession(): Pair<String?, String?> {
        return Pair(prefsManager.getUserId(), prefsManager.getUserEmail())
    }

    fun getPrefs() = prefsManager
}
