package com.example.precisionlayertesting.core.remote

import com.example.precisionlayertesting.core.models.bugModel.ApkValidationRequest
import com.example.precisionlayertesting.core.models.bugModel.ApkValidationResponse
import com.example.precisionlayertesting.core.models.bugModel.AppVersion
import com.example.precisionlayertesting.core.models.bugModel.AppVersionCreateRequest
import com.example.precisionlayertesting.core.models.bugModel.BugReport
import com.example.precisionlayertesting.core.models.bugModel.BugReportCreateRequest
import com.example.precisionlayertesting.core.models.bugModel.ConfirmUploadRequest
import com.example.precisionlayertesting.core.models.bugModel.DeleteFileRequest
import com.example.precisionlayertesting.core.models.bugModel.Module
import com.example.precisionlayertesting.core.models.bugModel.ModuleCreateRequest
import com.example.precisionlayertesting.core.models.bugModel.ScreenshotBatchPrepareRequest
import com.example.precisionlayertesting.core.models.bugModel.ScreenshotBatchPrepareResponse
import com.example.precisionlayertesting.core.models.bugModel.ScreenshotUploadRequest
import com.example.precisionlayertesting.core.models.bugModel.ScreenshotUploadResponse
import com.example.precisionlayertesting.core.models.bugModel.TestingSession
import com.example.precisionlayertesting.core.models.bugModel.TestingSessionCreateRequest
import com.example.precisionlayertesting.core.models.bugModel.ViewScreenshotRequest
import com.example.precisionlayertesting.core.models.bugModel.ViewScreenshotResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface BugApiService {

    @GET("rest/v1/modules")
    suspend fun getModules(
        @Query("workspace_id") workspaceId: String,
        @Query("select") select: String = "*"
    ): List<Module>

    @GET("rest/v1/app_versions")
    suspend fun getVersions(
        @Query("module_id") moduleId: String,
        @Query("workspace_id") workspaceId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): List<AppVersion>

    @GET("rest/v1/testing_sessions")
    suspend fun getBugGroups(
        @Query("version_id") versionId: String,
        @Query("workspace_id") workspaceId: String,
        @Query("select") select: String = "*,profiles(full_name,email),bug_reports(count)",
        @Query("order") order: String = "created_at.desc"
    ): List<TestingSession>

    @GET("rest/v1/bug_reports")
    suspend fun getBugReports(
        @Query("session_id") sessionId: String,
        @Query("workspace_id") workspaceId: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): List<BugReport>

    @GET("rest/v1/bug_reports")
    suspend fun getBugDetail(
        @Query("id") bugId: String,
        @Query("select") select: String = "*"
    ): List<BugReport>

    @POST("rest/v1/modules")
    suspend fun createModule(
        @Header("Prefer") prefer: String = "return=representation",
        @Body request: ModuleCreateRequest
    ): Response<List<Module>>

    @POST("rest/v1/app_versions")
    suspend fun createVersion(
        @Header("Prefer") prefer: String = "return=representation",
        @Body request: AppVersionCreateRequest
    ): Response<List<AppVersion>>

    @POST("rest/v1/testing_sessions")
    suspend fun createTestingSession(
        @Header("Prefer") prefer: String = "return=representation",
        @Body request: TestingSessionCreateRequest
    ): Response<List<TestingSession>>

    @POST("rest/v1/bug_reports")
    suspend fun createBugReports(
        @Header("Prefer") prefer: String = "return=representation",
        @Body request: List<BugReportCreateRequest>
    ): Response<List<BugReport>>

    @POST("functions/v1/validate-apk")
    suspend fun validateApk(
        @Body request: ApkValidationRequest
    ): Response<ApkValidationResponse>

    @POST("functions/v1/confirm-upload")
    suspend fun confirmUpload(
        @Body request: ConfirmUploadRequest
    ): Response<AppVersion>

    @POST("functions/v1/prepare-bug-screenshot-upload")
    suspend fun prepareScreenshotUpload(
        @Body request: ScreenshotUploadRequest
    ): Response<ScreenshotUploadResponse>

    @POST("functions/v1/prepare-bug-screenshot-upload-batch")
    suspend fun prepareScreenshotUploadBatch(
        @Body request: ScreenshotBatchPrepareRequest
    ): Response<ScreenshotBatchPrepareResponse>

    @POST("functions/v1/delete-r2-file")
    suspend fun deleteR2File(
        @Body request: DeleteFileRequest
    ): Response<Unit>

    @POST("functions/v1/view-bug-screenshot")
    suspend fun viewScreenshot(
        @Body request: ViewScreenshotRequest
    ): Response<ViewScreenshotResponse>
}
