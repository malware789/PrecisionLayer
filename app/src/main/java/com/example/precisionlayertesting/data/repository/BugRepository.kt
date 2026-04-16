package com.example.precisionlayertesting.data.repository

import com.example.precisionlayertesting.core.utils.Result
import com.example.precisionlayertesting.data.models.bug.*
import com.example.precisionlayertesting.data.remote.BugApiService
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BugRepository(private val apiService: BugApiService) {

    companion object {
        private const val TAG = "BugRepository"
    }

    suspend fun getModules(workspaceId: String): List<Module> = withContext(Dispatchers.IO) {
        apiService.getModules("eq.$workspaceId")
    }

    suspend fun getVersions(moduleId: String, workspaceId: String): List<AppVersion> = withContext(Dispatchers.IO) {
        apiService.getVersions("eq.$moduleId", "eq.$workspaceId")
    }

    suspend fun getBugGroups(versionId: String, workspaceId: String): List<TestingSession> = withContext(Dispatchers.IO) {
        apiService.getBugGroups("eq.$versionId", "eq.$workspaceId")
    }

    suspend fun getBugReports(sessionId: String, workspaceId: String, page: Int = 0): List<BugReport> = withContext(Dispatchers.IO) {
        val limit = 20
        val offset = page * limit
        apiService.getBugReports(
            sessionId = "eq.$sessionId",
            workspaceId = "eq.$workspaceId",
            limit = limit,
            offset = offset
        )
    }

    suspend fun getBugDetail(bugId: String): BugReport? = withContext(Dispatchers.IO) {
        apiService.getBugDetail("eq.$bugId").firstOrNull()
    }

    suspend fun createModule(request: ModuleCreateRequest): Result<Module> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.createModule(request = request)
            if (response.isSuccessful) {
                val module = response.body()?.firstOrNull()
                if (module != null) {
                    Result.Success(module)
                } else {
                    Result.Error(Exception("No data returned from server"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "createModule failed: ${response.code()} ${response.message()} - $errorBody")
                Result.Error(Exception("Server error: ${response.code()} $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "createModule exception: ${e.message}", e)
            Result.Error(e)
        }
    }

    suspend fun createVersion(request: AppVersionCreateRequest): Result<AppVersion> = withContext(Dispatchers.IO) {
        try {
            // 🔥 IMPORTANT LOGIC: Fetch existing versions to calculate build_number and version_code
            val existingVersions = apiService.getVersions("eq.${request.moduleId}", "eq.${request.workspaceId}", select = "version_name,version_code,build_number")
            
            val sameVersionEntries = existingVersions.filter { it.versionName == request.versionName }
            val isNewVersionName = sameVersionEntries.isEmpty()
            
            val maxBuildNumber = sameVersionEntries.maxOfOrNull { it.buildNumber } ?: 0
            val lastVersionCode = existingVersions.maxOfOrNull { it.versionCode } ?: 0
            
            val finalBuildNumber = if (isNewVersionName) 1 else maxBuildNumber + 1
            val finalVersionCode = if (isNewVersionName) lastVersionCode + 1 else {
                // Same version name, keep the version code from previous entries of this version
                sameVersionEntries.first().versionCode
            }

            val finalRequest = request.copy(
                buildNumber = finalBuildNumber,
                versionCode = finalVersionCode
            )

            Log.d(TAG, "Creating version: ${finalRequest.versionName} (b$finalBuildNumber), Code: $finalVersionCode")

            val response = apiService.createVersion(request = finalRequest)
            if (response.isSuccessful) {
                val version = response.body()?.firstOrNull()
                if (version != null) {
                    Result.Success(version)
                } else {
                    Result.Error(Exception("No data returned from server"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "createVersion failed: ${response.code()} ${response.message()} - $errorBody")
                Result.Error(Exception("Server error: ${response.code()} $errorBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "createVersion exception: ${e.message}", e)
            Result.Error(e)
        }
    }
}
